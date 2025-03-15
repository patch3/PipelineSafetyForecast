package gas.pipeline.safety.forecast.util.time.series;

import lombok.NoArgsConstructor;
import math.series.time.TimeSeries;
import org.apache.commons.math3.stat.regression.SimpleRegression;

@NoArgsConstructor
public class LinearRegression extends TimeSeries<LinearRegressionForecast> implements ISimplePrediction {
    private SimpleRegression regression;
    private double slope;
    private double intercept;
    private double rSquare;

    public LinearRegression(double[] data) {
        super(data);
        fit(data);
    }

    @Override
    public void fit(double[] data) {
        super.fit(data);
        this.regression = new SimpleRegression();

        // Добавляем данные в формате (время, значение)
        for (int i = 0; i < data.length; i++) {
            regression.addData(i, data[i]);
        }

        this.slope = regression.getSlope();
        this.intercept = regression.getIntercept();
        this.rSquare = regression.getRSquare();
    }

    @Override
    public LinearRegressionForecast forecast(int forecastSize) {
        if (regression == null) {
            throw new IllegalStateException("Model not trained. Call fit() first.");
        }

        double[] forecast = new double[forecastSize];
        int lastIndex = data.length - 1;

        // Прогнозируем следующие значения
        for (int i = 0; i < forecastSize; i++) {
            forecast[i] = regression.predict(lastIndex + i + 1);
        }

        return new LinearRegressionForecast(
                forecast,
                slope,
                intercept,
                rSquare
        );
    }
}