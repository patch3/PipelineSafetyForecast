package gas.pipeline.safety.forecast.config.model;

import gas.pipeline.safety.forecast.util.analyzer.AnomalyAnalyzer;
import gas.pipeline.safety.forecast.util.analyzer.IAnomaly;
import gas.pipeline.safety.forecast.util.analyzer.filter.FilterStrategy;
import gas.pipeline.safety.forecast.util.analyzer.filter.MedianFilter;
import gas.pipeline.safety.forecast.util.analyzer.filter.MovingAverageFilter;
import gas.pipeline.safety.forecast.util.mod.Mode;
import gas.pipeline.safety.forecast.util.time.series.ISimplePrediction;
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
@PropertySource(value = "classpath:config/pressure.properties")
public class PressureModelsConfig {
    private final ModelsConfig modelsConfig;
    @Value("${cusum.threshold:30}")
    private Double cusumThreshold;
    @Value("${leak.threshold:7}")
    private Double leakThreshold;
    @Value("${decay.factor:0.3}")
    private Double decayFactor;
    @Value("${calibration.records:30}")
    private Integer calibrationRecords;
    @Value("${moving.average.window:10}")
    private Integer filterWindow;
    @Value("${filter.strategy:median}")
    private FilterMode filterStrategy;
    @Value("${forecast.model:arima}")
    private ForecastModelMode forecastModel;


    public PressureModelsConfig(ModelsConfig modelsConfig) {
        this.modelsConfig = modelsConfig;
    }


    @Bean
    public IAnomaly pressureAnalyzer() {
        return new AnomalyAnalyzer(
                cusumThreshold,
                leakThreshold,
                decayFactor,
                calibrationRecords,
                filterWindow,
                filterStrategy.getFilter(),
                modelsConfig.getUpdateMode()
        );
    }

    @Bean
    public TimeSeries<? extends ForecastResult> pressureForecastModel() {
        return forecastModel.getModel();
    }

    @Bean
    public ISimplePrediction linearRegression() {
        return new LinearRegression();
    }

    public enum FilterMode implements Mode {
        MOVING_AVERAGE,
        MEDIAN;

        public FilterStrategy getFilter() {
            return switch (this) {
                case MOVING_AVERAGE -> new MovingAverageFilter();
                case MEDIAN -> new MedianFilter();
            };
        }
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
