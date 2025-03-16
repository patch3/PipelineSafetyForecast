package gas.pipeline.safety.forecast.repository;

import gas.pipeline.safety.forecast.model.sensor.Sensor;
import gas.pipeline.safety.forecast.model.sensor.SensorReading;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SensorReadingRepository extends JpaRepository<SensorReading, Long> {

    List<SensorReading> findBySensorNameAndTimestampAfter(String sensorName, LocalDateTime timestamp);

    long countBySensorNameAndTimestampAfter(String sensorName, LocalDateTime timestamp);

    @Query("SELECT DISTINCT s.sensor.name FROM SensorReading s")
    List<String> findDistinctSensorNames();


    @Query(
            value = "SELECT timestamp FROM sensor_reading ORDER BY timestamp ASC LIMIT 1",
            nativeQuery = true
    )
    Optional<LocalDateTime> findFirstTimestamp();

    @Query(
            value = "SELECT timestamp FROM sensor_reading ORDER BY timestamp DESC LIMIT 1",
            nativeQuery = true
    )
    Optional<LocalDateTime> findLastTimestamp();


    @Query(
            value = "SELECT timestamp FROM sensor_reading WHERE sensor_id = :sensorId ORDER BY timestamp ASC LIMIT 1",
            nativeQuery = true
    )
    Optional<LocalDateTime> findFirstTimestampBySensorId(@Param("sensorId") Long sensorId);

    @Query(
            value = "SELECT timestamp FROM sensor_reading WHERE sensor_id = :sensorId ORDER BY timestamp DESC LIMIT 1",
            nativeQuery = true
    )
    Optional<LocalDateTime> findLastTimestampBySensorId(@Param("sensorId") Long sensorId);


    List<SensorReading> findBySensorNameAndTimestampBetweenAndIsLeakFalse(
            String sensorName,
            LocalDateTime start,
            LocalDateTime end
    );

    List<SensorReading> findBySensorNameAndTimestampBetween(
            String sensorName,
            LocalDateTime start,
            LocalDateTime end
    );
}

