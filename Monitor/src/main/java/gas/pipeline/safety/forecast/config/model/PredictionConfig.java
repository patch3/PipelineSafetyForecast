package gas.pipeline.safety.forecast.config.model;

import gas.pipeline.safety.forecast.util.mod.Mode;
import gas.pipeline.safety.forecast.util.time.series.LinearRegression;
import lombok.Getter;
import math.series.time.ForecastResult;
import math.series.time.TimeSeries;
import math.series.time.arima.analytics.Arima;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Getter
@Configuration
@PropertySource("classpath:config/prediction.properties")
public class PredictionConfig {
    @Value("${days.interval:7}")
    private int daysInterval;

    @Value("${forecast.model:arima}")
    private ForecastModelMode forecastModel;

    @Value("${prediction.days:7}")
    private int predictionDays;

    @Value("${average.frequency:10}")
    private int averageFrequency;


    @Bean
    public TimeSeries<? extends ForecastResult> pressureForecastModel() {
        return forecastModel.getModel();
    }


    public enum ForecastModelMode implements Mode {
        LINEAR_REGRESSION,
        ARIMA;

        public TimeSeries<? extends ForecastResult> getModel() {
            return switch (this) {
                case LINEAR_REGRESSION -> new LinearRegression();
                case ARIMA -> new Arima();
            };
        }
    }
}
