package gas.pipeline.safety.forecast.config;

import gas.pipeline.safety.forecast.util.PressureAnalyzer;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Getter
@Configuration
@PropertySource(value = "classpath:config/model.properties")
public class PressureModelsConfig {
    @Value("${pressure.cusum.threshold:30}")
    private Double cusumThreshold;
    @Value("${pressure.leak.threshold:7}")
    private Double leakThreshold;
    @Value("${pressure.calibration.records:30}")
    private Integer calibrationRecords;
    @Value("${pressure.moving.average.window:10}")
    private Integer filterWindow;
    @Value("${pressure.filter.type:median}")
    private PressureAnalyzer.FilterType filterType;


    @Bean
    public PressureAnalyzer pressureAnalyzer() {
        return new PressureAnalyzer(
                cusumThreshold,
                leakThreshold,
                calibrationRecords,
                filterWindow,
                filterType
        );
    }
}
