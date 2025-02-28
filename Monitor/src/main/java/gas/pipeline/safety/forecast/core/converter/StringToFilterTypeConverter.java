package gas.pipeline.safety.forecast.core.converter;

import gas.pipeline.safety.forecast.util.PressureAnalyzer;
import jakarta.annotation.Nullable;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToFilterTypeConverter implements Converter<String, PressureAnalyzer.FilterType> {
    @Override
    public PressureAnalyzer.FilterType convert(@Nullable String source) {
        return PressureAnalyzer.FilterType.fromString(source)
                .orElseThrow(() -> new IllegalArgumentException("Invalid filter type: " + source));
    }
}
