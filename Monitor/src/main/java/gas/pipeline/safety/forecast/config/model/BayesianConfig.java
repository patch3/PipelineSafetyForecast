package gas.pipeline.safety.forecast.config.model;

import gas.pipeline.safety.forecast.util.analyzer.BayesianTheorem;
import gas.pipeline.safety.forecast.util.analyzer.IProbability;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Getter
@Configuration
@PropertySource("classpath:config/bayesian.properties")
public class BayesianConfig {
    private final ModelsConfig modelsConfig;
    @Value("${leak.decay.factor:0.7}")
    private double leakDecayFactor;
    @Value("${normal.decay.factor:0.2}")
    private double normalDecayFactor;
    @Value("${prediction.days:7}")
    private int predictionDays;
    @Value("${average.frequency:10}")
    private int averageFrequency;

    public BayesianConfig(ModelsConfig modelsConfig) {
        this.modelsConfig = modelsConfig;
    }

    @Bean
    public IProbability bayesianLeakModel() {
        return new BayesianTheorem(leakDecayFactor, normalDecayFactor, modelsConfig.getUpdateMode());
    }
}
