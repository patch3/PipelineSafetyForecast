package gas.pipeline.safety.forecast.util.analyzer.filter;

import gas.pipeline.safety.forecast.util.model.AnomalyModel;

public abstract class FilterStrategy {
    public abstract double applyFilter(AnomalyModel model, double value);
}