package gas.pipeline.safety.forecast.core.converter;

import gas.pipeline.safety.forecast.config.model.PressureModelsConfig;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;


@Component
public class StringToFilterTypeConverter implements Converter<String, PressureModelsConfig.FilterMode> {
    @Override
    public PressureModelsConfig.FilterMode convert(String source) {
        try {
            return PressureModelsConfig.FilterMode.valueOf(source.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid filter type: " + source);
        }
    }
}