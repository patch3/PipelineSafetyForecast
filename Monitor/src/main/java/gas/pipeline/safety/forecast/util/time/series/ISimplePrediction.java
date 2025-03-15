package gas.pipeline.safety.forecast.util.time.series;

public interface ISimplePrediction {
    void fit(double[] data);

    LinearRegressionForecast forecast(int forecastSize);
}
