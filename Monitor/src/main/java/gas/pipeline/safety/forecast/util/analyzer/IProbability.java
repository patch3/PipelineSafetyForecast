package gas.pipeline.safety.forecast.util.analyzer;

import gas.pipeline.safety.forecast.util.model.ProbabilityModel;

public interface IProbability {
    void update(String sensorName, boolean isArtifact, double pressure);

    ProbabilityModel getModel(String sensorName);

}
