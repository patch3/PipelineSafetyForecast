package gas.pipeline.safety.forecast.service.models;


import gas.pipeline.safety.forecast.config.ModelsConfig;
import gas.pipeline.safety.forecast.model.sensor.Sensor;
import gas.pipeline.safety.forecast.model.sensor.SensorReading;
import gas.pipeline.safety.forecast.repository.SensorReadingRepository;
import gas.pipeline.safety.forecast.util.PressureAnalyzer;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class LeakDetectionService extends BaseLeakService {
    private final PressureAnalyzer pressureAnalyzer;
    @Autowired
    public LeakDetectionService(SensorReadingRepository sensorReadingRepository,
                                ModelsConfig modelsConfig,
                                PressureAnalyzer pressureAnalyzer) {
        super(sensorReadingRepository, modelsConfig);
        this.pressureAnalyzer = pressureAnalyzer;
    }

    @Override
    protected void processSensorReadings(String sensorId, List<SensorReading> date) {
        date.stream()
                .filter(reading -> !reading.isLeak())
                .forEach(reading ->
                        pressureAnalyzer.analyzePressure(
                                reading.getSensor().getName(),
                                reading.getPressure()
                        )
                );
    }


    public SensorReading processSensorReading(String sensorName, double pressure, LocalDateTime timestamp) {
        val isLeak = pressureAnalyzer.analyzePressure(sensorName, pressure);
        val reading = new SensorReading(
                new Sensor(sensorName),
                pressure,
                isLeak,
                timestamp
        );
        return sensorReadingRepo.save(reading);
    }
}