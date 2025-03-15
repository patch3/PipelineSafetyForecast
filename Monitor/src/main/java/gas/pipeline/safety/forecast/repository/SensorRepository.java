package gas.pipeline.safety.forecast.repository;

import gas.pipeline.safety.forecast.model.sensor.Sensor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SensorRepository extends JpaRepository<Sensor, Long> {
    Optional<Sensor> findByName(String name);

    @Query("SELECT s.name FROM Sensor s")
    List<String> findAllSensorNames();
}
