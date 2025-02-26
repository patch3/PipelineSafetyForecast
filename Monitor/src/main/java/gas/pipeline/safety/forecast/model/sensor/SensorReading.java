package gas.pipeline.safety.forecast.model.sensor;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SensorReading {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "sensor_id", nullable = false)
    private Sensor sensor;

    private double pressure;
    private boolean isLeak;
    private LocalDateTime timestamp;


    public SensorReading(Sensor sensorName, double pressure, boolean isLeak, LocalDateTime timestamp) {
        this.sensor = sensorName;
        this.pressure = pressure;
        this.isLeak = isLeak;
        this.timestamp = timestamp;
    }
}
