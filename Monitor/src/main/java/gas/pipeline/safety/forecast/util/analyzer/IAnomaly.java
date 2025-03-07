package gas.pipeline.safety.forecast.util.analyzer;

import gas.pipeline.safety.forecast.util.model.AnomalyModel;

public interface IAnomaly {
    boolean analyzePressure(String sensorName, double pressure);

    double predictNextPressure(AnomalyModel model);
}
