package gas.pipeline.safety.forecast.core.converter;

import gas.pipeline.safety.forecast.config.model.PressureModelsConfig;
import lombok.NonNull;
import math.series.time.ForecastResult;
import math.series.time.TimeSeries;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;


@Component
public class StringToForecastModelTypeConverter implements Converter<String, PressureModelsConfig.ForecastModelMode> {
    @Override
    public PressureModelsConfig.ForecastModelMode convert(@NonNull String source) {
        try {
            return PressureModelsConfig.ForecastModelMode.valueOf(source);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid time series type: " + source);
        }
    }
}
