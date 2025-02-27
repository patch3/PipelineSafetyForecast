package gas.pipeline.safety.forecast.service.models;

import gas.pipeline.safety.forecast.config.ModelsConfig;
import gas.pipeline.safety.forecast.model.sensor.SensorReading;
import gas.pipeline.safety.forecast.repository.SensorReadingRepository;
import gas.pipeline.safety.forecast.util.BayesianLeakModel;
import gas.pipeline.safety.forecast.util.PressureAnalyzer;
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
    private final BayesianLeakModel leakModel;
    private final PressureAnalyzer pressureAnalyzer;

    private final int defaultTrainingDays;
    private final int defaultPredictionsDays;
    private final int defaultAverageFrequency;


    @Autowired
    public LeakPredictionsService(SensorReadingRepository sensorReadingRepo,
                                  BayesianLeakModel leakModel, PressureAnalyzer pressureAnalyzer,
                                  ModelsConfig modelsConfig) {
        super(sensorReadingRepo, modelsConfig);

        this.leakModel = leakModel;
        this.pressureAnalyzer = pressureAnalyzer;

        this.defaultTrainingDays = modelsConfig.getTrainingDays();
        this.defaultPredictionsDays = modelsConfig.getPredictionDays();
        this.defaultAverageFrequency = modelsConfig.getAverageFrequency();
    }

    @Override
    protected void processSensorReadings(String sensorId, List<SensorReading> data) {
        data.forEach(reading ->
                leakModel.update(
                        reading.getSensor().getName(),
                        reading.isLeak(),
                        reading.getPressure()
                )
        );
        //generatePredictions(sensorId);
        checkAlerts(sensorId);
    }


    public void processNewReadings(SensorReading reading) {
        //sensorReadingRepo.save(reading);
        leakModel.update(reading.getSensor().getName(), reading.isLeak(), reading.getPressure());

        //generatePredictions(reading.getSensorId());
        checkAlerts(reading.getSensor().getName());
    }

    public List<LeakPrediction> generatePredictionsForPeriod(String sensorName) {
        val forecastBayesianModel = new BayesianLeakModel(leakModel);
        val forecastPressureModel = new PressureAnalyzer(pressureAnalyzer);

        val originalStats = pressureAnalyzer.getSensorStats(sensorName);
        if (originalStats != null) {
            forecastPressureModel.getSensorStats().put(sensorName,
                    PressureAnalyzer.SensorStats.builder()
                            .mean(originalStats.getMean())
                            .variance(originalStats.getVariance())
                            .count(originalStats.getCount())
                            .cusum(originalStats.getCusum())
                            .build());
        }

        var frequency = calculateFrequency(sensorName);
        frequency = frequency > 0 ? frequency : defaultAverageFrequency; // Используем значение по умолчанию


        val intervalMinutes = (long) (1440L / frequency);
        val totalPredictions = (int) (defaultPredictionsDays * frequency);

        val predictions = new ArrayList<LeakPrediction>(totalPredictions);
        val currentTime = LocalDateTime.now();

        for (long i = 0; i < totalPredictions; i++) {
            // прогноз давления с учетом тренда
            val predictedPressure = forecastPressureModel.predictNextPressure(sensorName);

            // Обновляем модель-копию прогнозируемым давлением (без реальной утечки)
            forecastBayesianModel.update(sensorName, false, predictedPressure);

            // Получаем вероятность из обновленной копии
            val probability = forecastBayesianModel.getLeakProbability(sensorName);

            // обновляем модель давления на будущие
            forecastPressureModel.analyzePressure(sensorName, predictedPressure);

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


    private void checkAlerts(String sensorModel) {
        val prob = leakModel.getLeakProbability(sensorModel);
        if (prob > 0.7) {
            log.warn("Leak prediction probability is higher than {} for sensor {}", prob, sensorModel);
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
