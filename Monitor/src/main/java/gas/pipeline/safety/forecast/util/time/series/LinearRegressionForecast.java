package gas.pipeline.safety.forecast.util.time.series;

import lombok.Getter;
import math.series.time.ForecastResult;

@Getter
public class LinearRegressionForecast extends ForecastResult {
    private final double slope;
    private final double intercept;
    private final double rSquare;

    public LinearRegressionForecast(double[] forecast,
                                    double slope,
                                    double intercept,
                                    double rSquare) {
        super(forecast);
        this.slope = slope;
        this.intercept = intercept;
        this.rSquare = rSquare;
    }
}