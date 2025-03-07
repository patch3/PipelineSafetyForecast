package gas.pipeline.safety.forecast.service.models;


import gas.pipeline.safety.forecast.config.model.ModelsConfig;
import gas.pipeline.safety.forecast.model.sensor.Sensor;
import gas.pipeline.safety.forecast.model.sensor.SensorReading;
import gas.pipeline.safety.forecast.repository.SensorReadingRepository;
import gas.pipeline.safety.forecast.repository.SensorRepository;
import gas.pipeline.safety.forecast.util.analyzer.IAnomaly;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class LeakDetectionService extends BaseLeakService {
    private final IAnomaly anomalyAnalyzer;
    private final SensorRepository sensorRepository;

    @Autowired
    public LeakDetectionService(SensorReadingRepository sensorReadingRepository,
                                ModelsConfig modelsConfig,
                                IAnomaly anomalyAnalyzer,
                                SensorRepository sensorRepository) {
        super(sensorReadingRepository, modelsConfig);
        this.anomalyAnalyzer = anomalyAnalyzer;
        this.sensorRepository = sensorRepository;
    }


    @Override
    protected void processSensorReadings(String sensorId, List<SensorReading> date) {
        date.stream()
                .filter(reading -> !reading.isLeak())
                .forEach(reading ->
                        anomalyAnalyzer.analyzePressure(
                                reading.getSensor().getName(),
                                reading.getPressure()
                        )
                );
    }

    public SensorReading processSensorReading(String sensorName, double pressure, LocalDateTime timestamp) {
        val sensor = sensorRepository.findByName(sensorName).orElseGet(() ->
                sensorRepository.save(new Sensor(sensorName))
        );
        val isLeak = anomalyAnalyzer.analyzePressure(sensorName, pressure);
        val reading = new SensorReading(sensor, pressure, isLeak, timestamp);
        return sensorReadingRepo.save(reading);
    }
}