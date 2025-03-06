package gas.pipeline.safety.forecast.service.models;

import gas.pipeline.safety.forecast.config.model.ModelsConfig;
import gas.pipeline.safety.forecast.model.sensor.SensorReading;
import gas.pipeline.safety.forecast.repository.SensorReadingRepository;
import gas.pipeline.safety.forecast.util.AnomalyAnalyzer;
import gas.pipeline.safety.forecast.util.BayesianTheorem;
import gas.pipeline.safety.forecast.util.model.AnomalyModel;
import gas.pipeline.safety.forecast.util.model.BayesianModel;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class LeakPredictionsService extends BaseLeakService {
    private final BayesianTheorem leakModel;
    private final AnomalyAnalyzer anomalyAnalyzer;

    private final int defaultTrainingDays;
    private final int defaultPredictionsDays;
    private final int defaultAverageFrequency;


    @Autowired
    public LeakPredictionsService(SensorReadingRepository sensorReadingRepo,
                                  BayesianTheorem leakModel,
                                  AnomalyAnalyzer anomalyAnalyzer,
                                  ModelsConfig modelsConfig) {
        super(sensorReadingRepo, modelsConfig);

        this.leakModel = leakModel;
        this.anomalyAnalyzer = anomalyAnalyzer;

        this.defaultTrainingDays = modelsConfig.getTrainingDays();
        this.defaultPredictionsDays = modelsConfig.getPredictionDays();
        this.defaultAverageFrequency = modelsConfig.getAverageFrequency();
    }

    @Override
    protected void processSensorReadings(String sensorName, List<SensorReading> data) {
        data.forEach(reading ->
                leakModel.update(
                        reading.getSensor().getName(),
                        reading.isLeak(),
                        reading.getPressure()
                )
        );
        checkAlerts(leakModel.getModel(sensorName));
    }


    public void processNewReadings(SensorReading reading) {
        leakModel.update(reading.getSensor().getName(), reading.isLeak(), reading.getPressure());
        checkAlerts(leakModel.getModel(reading.getSensor().getName()));
    }

    public List<LeakPrediction> generatePredictionsForPeriod(String sensorName) {
        val forecastBayesianTheorem = new BayesianTheorem(leakModel);
        val sensorBayesianTheorem = forecastBayesianTheorem.getModel(sensorName);

        val forecastAnomalyAnalyzer = new AnomalyAnalyzer(anomalyAnalyzer);
        val sensorAnomalyModel = forecastAnomalyAnalyzer.getModel(sensorName);

/*        val originalStats = anomalyAnalyzer.getModel(sensorName);
        if (originalStats != null) {
            forecastAnomalyAnalyzer.punSensor(sensorName, new AnomalyModel(originalStats)) .getModel() getSensorModels().put(sensorName, new AnomalyModel(originalStats));
        }*/

        var frequency = calculateFrequency(sensorName);
        frequency = frequency > 0 ? frequency : defaultAverageFrequency; // Используем значение по умолчанию

        val intervalMinutes = (long) (1440L / frequency);
        val totalPredictions = (int) (defaultPredictionsDays * frequency);

        val predictions = new ArrayList<LeakPrediction>(totalPredictions);
        val currentTime = LocalDateTime.now();

        for (long i = 0; i < totalPredictions; i++) {
            // прогноз давления с учетом тренда
            val predictedPressure = forecastAnomalyAnalyzer.predictNextPressure(sensorAnomalyModel);

            // обновляем модель давления на будущие
            val isAnomaly = forecastAnomalyAnalyzer.analyzePressure(sensorName, predictedPressure);

            // Обновляем модель-копию прогнозируемым давлением на основе предполагаемых данных
            forecastBayesianTheorem.update(sensorName, isAnomaly, predictedPressure);

            // Получаем вероятность из обновленной копии
            val probability = sensorBayesianTheorem.getLeakProbability();

            predictions.add(LeakPrediction.builder()
                    .timestamp(currentTime.plusMinutes(i * intervalMinutes))
                    .probability(probability)
                    .build());
        }
        return predictions;
    }


    double calculateFrequency(String sensorModel) {
        val opFirstTime = sensorReadingRepo.findFirstTimestamp();
        val opLastTime = sensorReadingRepo.findLastTimestamp();

        if (opFirstTime.isEmpty() || opLastTime.isEmpty()) {
            return 0.0;
        }
        val difference = ChronoUnit.DAYS.between(opFirstTime.get(), opLastTime.get());

        long trainingDays;
        if (difference > defaultAverageFrequency) {
            trainingDays = defaultTrainingDays;
        } else {
            trainingDays = difference;
        }

        val count = sensorReadingRepo.countBySensorNameAndTimestampAfter(
                sensorModel,
                LocalDateTime.now().minusDays(trainingDays)
        );
        return count > 0 ? (double) count / trainingDays : defaultAverageFrequency;
    }


    private void checkAlerts(BayesianModel model) {
        val prob = model.getLeakProbability();
        if (prob > 0.7) {
            log.warn("Leak prediction probability is higher than {}", prob);
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class LeakPrediction {
        private LocalDateTime timestamp;
        private double probability;
    }
}
