package gas.pipeline.safety.forecast.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LeakProbabilityForecastDTO {
    private LocalDateTime time;
    private double value;
}
