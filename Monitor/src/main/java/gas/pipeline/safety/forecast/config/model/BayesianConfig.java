package gas.pipeline.safety.forecast.config.model;

import gas.pipeline.safety.forecast.util.BayesianTheorem;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Getter
@Configuration
@PropertySource("classpath:config/bayesian.properties")
public class BayesianConfig {

    @Value("${leak.decay.factor:0.7}")
    public double leakDecayFactor;
    @Value("${normal.decay.factor:0.2}")
    public double normalDecayFactor;
    @Value("${update.mode}")
    private BayesianTheorem.UpdateMode updateMode;

    @Bean
    public BayesianTheorem bayesianLeakModel() {
        return new BayesianTheorem(leakDecayFactor, normalDecayFactor, updateMode);
    }
}
