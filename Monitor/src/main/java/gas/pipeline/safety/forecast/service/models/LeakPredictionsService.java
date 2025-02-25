package gas.pipeline.safety.forecast.service.models;

import gas.pipeline.safety.forecast.config.ModelsConfig;
import gas.pipeline.safety.forecast.model.sensor.SensorReading;
import gas.pipeline.safety.forecast.repository.SensorReadingRepository;
import gas.pipeline.safety.forecast.util.BayesianLeakModel;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
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

    private final int defaultTrainingDays;
    private final int defaultPredictionsDays;
    private final int defaultAverageFrequency;


    @Autowired
    public LeakPredictionsService(SensorReadingRepository sensorReadingRepo,
                                  BayesianLeakModel leakModel,
                                  ModelsConfig modelsConfig) {
        super(sensorReadingRepo, modelsConfig);

        this.leakModel = leakModel;

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
        val forecastModel = new BayesianLeakModel(leakModel);

        var frequency = calculateFrequency(sensorName);
        if (frequency <= 0) {
            frequency = defaultAverageFrequency; // Используем значение по умолчанию
        }

        val intervalMinutes = (long) (1440 / frequency);
        val totalPredictions = (int) (defaultPredictionsDays * frequency);

        val timeFirstPoint = LocalDateTime.now().minusMinutes(intervalMinutes);

        val predictions = new ArrayList<LeakPrediction>(totalPredictions);


        for (long i = 0; i < totalPredictions; i++) {
            // TODO пока принимаются в качестве прогнозируемого давления среднее в нормали, но может можно это определять?
            val predictedPressure = forecastModel.getNormalMean(sensorName);

            // Обновляем модель-копию прогнозируемым давлением (без реальной утечки)
            forecastModel.update(sensorName, false, predictedPressure);

            // Получаем вероятность из обновленной копии
            val probability = forecastModel.getLeakProbability(sensorName);

            val prediction = new LeakPrediction();
            prediction.setSensorName(sensorName);
            prediction.setTimestamp(timeFirstPoint.minusMinutes(i * intervalMinutes));
            prediction.setLeakProbability(probability); // TODO здесь должна быть определеноя вероятность для следующей итерации данных датчика
            predictions.add(prediction);
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

        val count = sensorReadingRepo.countBySensorIdAndTimestampAfter(
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
    public static class LeakPrediction {
        private Long id;
        private String sensorName;
        private LocalDateTime timestamp;
        private double leakProbability;
    }
}
