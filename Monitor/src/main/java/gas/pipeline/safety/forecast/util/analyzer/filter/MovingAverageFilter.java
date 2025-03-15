package gas.pipeline.safety.forecast.util.analyzer.filter;

import gas.pipeline.safety.forecast.util.model.AnomalyModel;

public class MovingAverageFilter extends FilterStrategy {
    @Override
    public double applyFilter(AnomalyModel model, double value) {
        model.measurements.addLast(value);
        if (model.measurements.size() > model.sizeWindow) {
            model.measurements.removeFirst();
        }
        return model.measurements.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(value);
    }
}