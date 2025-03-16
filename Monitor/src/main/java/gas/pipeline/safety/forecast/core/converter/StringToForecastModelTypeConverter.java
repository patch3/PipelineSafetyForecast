package gas.pipeline.safety.forecast.core.converter;

import gas.pipeline.safety.forecast.config.model.PredictionConfig;
import lombok.NonNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;


@Component
public class StringToForecastModelTypeConverter implements Converter<String, PredictionConfig.ForecastModelMode> {
    @Override
    public PredictionConfig.ForecastModelMode convert(@NonNull String source) {
        try {
            return PredictionConfig.ForecastModelMode.valueOf(source);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid time series type: " + source);
        }
    }
}
