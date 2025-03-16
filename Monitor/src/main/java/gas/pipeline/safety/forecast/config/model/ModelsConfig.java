package gas.pipeline.safety.forecast.config.model;

import gas.pipeline.safety.forecast.util.analyzer.Analyzer;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Getter
@Configuration
@PropertySource("classpath:config/model.properties")
public class ModelsConfig {
    @Value("${training.days:30}")
    private int trainingDays;

    @Value("${update.mode}")
    private Analyzer.UpdateMode updateMode;
}
