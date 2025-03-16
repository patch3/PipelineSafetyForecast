package gas.pipeline.safety.forecast.service.models;

import gas.pipeline.safety.forecast.config.model.ModelsConfig;
import gas.pipeline.safety.forecast.config.model.PredictionConfig;
import gas.pipeline.safety.forecast.model.sensor.Sensor;
import gas.pipeline.safety.forecast.model.sensor.SensorReading;
import gas.pipeline.safety.forecast.repository.SensorReadingRepository;
import gas.pipeline.safety.forecast.util.analyzer.AnomalyAnalyzer;
import gas.pipeline.safety.forecast.util.analyzer.BayesianTheorem;
import gas.pipeline.safety.forecast.util.analyzer.IAnomaly;
import gas.pipeline.safety.forecast.util.analyzer.IProbability;
import gas.pipeline.safety.forecast.util.model.ProbabilityModel;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import math.series.time.ForecastResult;
import math.series.time.TimeSeries;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class LeakPredictionsService extends BaseLeakService {
    private static final int MINUTE_RER_DAY = 1440; // 24 * 60

    private static final Duration DAY_DURATION = Duration.ofDays(1);

    private final TimeSeries<? extends ForecastResult> timeSeries;
    private final IProbability probabilityAnalyzer;
    private final IAnomaly anomalyAnalyzer;

    private final int defaultTrainingDays;

    private final int defaultAverageFrequency;

    private int daysInternal;
    private int defaultPredictionsDays;

    private Duration intervalDuration;
    private Duration remainder;

    private long averageIntervalMinutes;


    @Autowired
    public LeakPredictionsService(SensorReadingRepository sensorReadingRepo,
                                  IProbability probabilityAnalyzer,
                                  IAnomaly anomalyAnalyzer,
                                  TimeSeries<?> timeSeries,
                                  ModelsConfig modelsConfig,
                                  PredictionConfig predictionConfig) {
        super(sensorReadingRepo, modelsConfig);

        this.probabilityAnalyzer = probabilityAnalyzer;
        this.anomalyAnalyzer = anomalyAnalyzer;
        this.timeSeries = timeSeries;

        this.defaultTrainingDays = modelsConfig.getTrainingDays();
        this.defaultAverageFrequency = predictionConfig.getAverageFrequency();

        setPredictions(predictionConfig.getDaysInterval(), predictionConfig.getPredictionDays());
    }

    public void setPredictions(int internal, int day) {
        this.daysInternal = internal;
        this.defaultPredictionsDays = day;

        this.intervalDuration = DAY_DURATION.dividedBy(daysInternal);
        this.remainder = DAY_DURATION.minus(intervalDuration.multipliedBy(daysInternal));

        // Средняя длительность интервала (в минутах)
        this.averageIntervalMinutes = remainder.plus(remainder).toMinutes() / daysInternal;
    }

    @Override
    protected void processSensorReadings(String sensorName, List<SensorReading> data) {
        data.forEach(reading ->
                probabilityAnalyzer.update(
                        reading.getSensor().getName(),
                        reading.isLeak(),
                        reading.getPressure()
                )
        );
        checkAlerts(probabilityAnalyzer.getModel(sensorName));
    }


    public void processNewReadings(SensorReading reading) {
        probabilityAnalyzer.update(reading.getSensor().getName(), reading.isLeak(), reading.getPressure());
        checkAlerts(probabilityAnalyzer.getModel(reading.getSensor().getName()));
    }


    public List<LeakPrediction> generatePredictionsForPeriod(String sensorName) {
        val endDate = LocalDateTime.now();
        val startDate = endDate.minusDays(defaultPredictionsDays);
        List<SensorReading> historicalData = sensorReadingRepo.findBySensorNameAndTimestampBetween(
                sensorName,
                startDate,
                endDate
        );

        if (historicalData.isEmpty()) {
            log.warn("No historical data for sensor: {}", sensorName);
            return Collections.emptyList();
        }

        val frequencyPerDay = calculateFrequencyPerDay(historicalData);
        val frequencyPerInterval = frequencyPerDay * ((double) averageIntervalMinutes / DAY_DURATION.toMinutes());

        val pressureData = historicalData.stream()
                .mapToDouble(SensorReading::getPressure)
                .toArray();


        timeSeries.fit(pressureData);
        double[] forecastResult = timeSeries.forecast(
                (int) (defaultPredictionsDays * frequencyPerDay)
        ).getForecast();

        val forecastBayesianTheorem = new BayesianTheorem(probabilityAnalyzer);
        val bayesianTheoremModel = forecastBayesianTheorem.getModel(sensorName);

        val forecastAnomalyAnalyzer = new AnomalyAnalyzer(anomalyAnalyzer);

        val predictions = new ArrayList<LeakPrediction>();
        val startDateTime = historicalData.getLast().getTimestamp();
        for (int i = 0, indexSensor = 0; i < defaultPredictionsDays; ++i) {
            var currentStart = startDateTime.plusDays(i).truncatedTo(ChronoUnit.DAYS);
            for (int j = 0; j < daysInternal; ++j) {
                boolean isAnomaly;
                for (int k = 0; k < frequencyPerInterval; ++k) {
                    val pressure = forecastResult[indexSensor++];
                    isAnomaly = forecastAnomalyAnalyzer.analyzePressure(sensorName, pressure);
                    forecastBayesianTheorem.update(sensorName, isAnomaly, pressure);
                }

                Duration currentInterval = (i == daysInternal - 1)
                        ? intervalDuration.plus(remainder)
                        : intervalDuration;

                currentStart = currentStart.plus(currentInterval);

                predictions.add(LeakPrediction.builder()
                        .timestamp(currentStart)
                        .probability(bayesianTheoremModel.getLeakProbability())
                        .build());
                log.info("Leak prediction: {}", predictions);
            }
        }
        return predictions;
    }


    private double calculateFrequencyPerDay(List<SensorReading> sensorsStat) {
        val difference = ChronoUnit.DAYS.between(
                sensorsStat.getFirst().getTimestamp(),
                sensorsStat.getLast().getTimestamp()
        );

        long trainingDays;
        if (difference > defaultAverageFrequency) {
            trainingDays = defaultTrainingDays;
        } else {
            trainingDays = difference;
        }
        val size = sensorsStat.size();
        return size > 0 ? (double) size / trainingDays : defaultAverageFrequency;
    }


    private void checkAlerts(ProbabilityModel model) {
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
