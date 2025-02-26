package gas.pipeline.safety.forecast.config;

import gas.pipeline.safety.forecast.util.BayesianLeakModel;
import gas.pipeline.safety.forecast.util.PressureAnalyzer;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.Scope;

@Getter
@Configuration
@PropertySource("classpath:config/model.properties")
public class ModelsConfig {
    @Value("${training.days}")
    private int trainingDays;
    @Value("${prediction.days}")
    private int predictionDays;
    @Value("${average.frequency}")
    private int averageFrequency;


    @Bean
    public BayesianLeakModel bayesianLeakModel() {
        return new BayesianLeakModel();
    }
}
