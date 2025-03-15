package gas.pipeline.safety.forecast.model.sensor;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class Sensor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    @OneToMany(mappedBy = "sensor", cascade = CascadeType.ALL)
    private List<SensorReading> sensorReadings = new ArrayList<>();


    public Sensor(String name) {
        this.name = name;
    }
}
