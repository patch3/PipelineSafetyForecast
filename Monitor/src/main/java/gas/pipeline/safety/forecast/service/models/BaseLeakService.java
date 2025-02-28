package gas.pipeline.safety.forecast.service.models;

import gas.pipeline.safety.forecast.config.ModelsConfig;
import gas.pipeline.safety.forecast.model.sensor.SensorReading;
import gas.pipeline.safety.forecast.repository.SensorReadingRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
public abstract class BaseLeakService {
    protected final SensorReadingRepository sensorReadingRepo;
    protected final ModelsConfig modelsConfig;

    @Async
    @PostConstruct
    protected void loadSensorReadings() {
        val trainingDay = modelsConfig.getTrainingDays();
        val startDate = LocalDateTime.now().minusDays(trainingDay);
        val sensorIds = sensorReadingRepo.findDistinctSensorNames();

        sensorIds.forEach(sensorId -> {
            val data = sensorReadingRepo.findBySensorNameAndTimestampAfter(
                    sensorId,
                    startDate
            );
            processSensorReadings(sensorId, data);
        });
    }

    protected abstract void processSensorReadings(String sensorId, List<SensorReading> data);
}
