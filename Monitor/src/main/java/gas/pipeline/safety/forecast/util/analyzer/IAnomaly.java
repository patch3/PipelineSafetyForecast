package gas.pipeline.safety.forecast.util.analyzer;

public interface IAnomaly {
    boolean analyzePressure(String sensorName, double pressure);
}
