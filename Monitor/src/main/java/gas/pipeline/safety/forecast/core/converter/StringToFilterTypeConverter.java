package gas.pipeline.safety.forecast.core.converter;

import gas.pipeline.safety.forecast.util.Analyzer;
import gas.pipeline.safety.forecast.util.AnomalyAnalyzer;
import lombok.NonNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToFilterTypeConverter implements Converter<String, Analyzer.Mode> {
    @Override
    public Analyzer.Mode convert(@NonNull String source) {
        try {
            return AnomalyAnalyzer.FilterMode.valueOf(source.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid filter type: " + source);
        }
    }
}
