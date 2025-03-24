package gas.pipeline.safety.forecast.core.converter;

import gas.pipeline.safety.forecast.config.model.AnomalyModelsConfig;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;


@Component
public class StringToFilterTypeConverter implements Converter<String, AnomalyModelsConfig.FilterMode> {
    @Override
    public AnomalyModelsConfig.FilterMode convert(String source) {
        try {
            return AnomalyModelsConfig.FilterMode.valueOf(source.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid filter type: " + source);
        }
    }
}