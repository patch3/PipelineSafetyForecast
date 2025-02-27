package gas.pipeline.safety.forecast.config;

import gas.pipeline.safety.forecast.util.BayesianLeakModel;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Getter
@Configuration
@PropertySource("classpath:config/model.properties")
public class ModelsConfig {
    @Value("${training.days:30}")
    private int trainingDays;
    @Value("${prediction.days:7}")
    private int predictionDays;
    @Value("${average.frequency:30}")
    private int averageFrequency;

    @Bean
    public BayesianLeakModel bayesianLeakModel() {
        return new BayesianLeakModel();
    }
}
