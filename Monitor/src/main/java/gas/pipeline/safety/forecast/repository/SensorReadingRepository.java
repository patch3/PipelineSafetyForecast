package gas.pipeline.safety.forecast.repository;

import gas.pipeline.safety.forecast.model.sensor.SensorReading;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SensorReadingRepository extends JpaRepository<SensorReading, Long> {
    List<SensorReading> findBySensorIdAndTimestampAfter(String sensorId, LocalDateTime timestamp);

    long countBySensorIdAndTimestampAfter(String sensorId, LocalDateTime timestamp);

    @Query("SELECT DISTINCT s.sensorName FROM SensorReading s")
    List<String> findDistinctSensorIds();


    @Query("SELECT timestamp FROM SensorReading ORDER BY timestamp ASC LIMIT 1")
    Optional<LocalDateTime> findFirstTimestamp();

    @Query("SELECT timestamp FROM SensorReading ORDER BY timestamp DESC LIMIT 1")
    Optional<LocalDateTime> findLastTimestamp();

    List<SensorReading> findBySensorIdAndTimestampBetweenAndIsLeakFalse(
            String sensorId,
            LocalDateTime start,
            LocalDateTime end
    );
}

