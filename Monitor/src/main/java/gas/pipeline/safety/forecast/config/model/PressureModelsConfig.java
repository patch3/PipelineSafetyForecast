package gas.pipeline.safety.forecast.config.model;

import gas.pipeline.safety.forecast.util.AnomalyAnalyzer;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Getter
@Configuration
@PropertySource(value = "classpath:config/pressure.properties")
public class PressureModelsConfig {
    @Value("${cusum.threshold:30}")
    private Double cusumThreshold;
    @Value("${leak.threshold:7}")
    private Double leakThreshold;
    @Value("${decay.factor:0.3}")
    private Double decayFactor;
    @Value("${calibration.records:30}")
    private Integer calibrationRecords;
    @Value("${moving.average.window:10}")
    private Integer filterWindow;
    @Value("${filter.type:median}")
    private AnomalyAnalyzer.FilterMode filterMode;


    @Bean
    public AnomalyAnalyzer pressureAnalyzer() {
        return new AnomalyAnalyzer(
                cusumThreshold,
                leakThreshold,
                decayFactor,
                calibrationRecords,
                filterWindow,
                filterMode
        );
    }
}
