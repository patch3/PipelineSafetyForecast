package gas.pipeline.safety.forecast.config;

import gas.pipeline.safety.forecast.util.PressureAnalyzer;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Getter
@Configuration
@PropertySource(value = "classpath:config/model.properties")
public class PressureModelsConfig {
    @Value("${pressure.cusum.threshold}")
    private double cusumThreshold;
    @Value("${pressure.leak.threshold}")
    private double leakThreshold;
    @Value("${pressure.calibration.records}")
    private int calibrationRecords;


    @Bean
    public PressureAnalyzer pressureAnalyzer() {
        return new PressureAnalyzer(cusumThreshold, leakThreshold, calibrationRecords);
    }
}
