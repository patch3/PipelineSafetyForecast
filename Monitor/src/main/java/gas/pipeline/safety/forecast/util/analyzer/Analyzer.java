package gas.pipeline.safety.forecast.util.analyzer;

import gas.pipeline.safety.forecast.util.mod.Mode;
import gas.pipeline.safety.forecast.util.model.Model;
import gas.pipeline.safety.forecast.util.model.Stat;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public abstract class Analyzer<T extends Model> {
    protected static final double DEFAULT_SEM_SQUARED = 1;

    protected static final double DEFAULT_VARIANCE = 1;
    protected static final double DEFAULT_MEAN = 1;
    protected final UpdateMode UPDATE_MODE;

    protected final Map<String, T> sensorModels;


    public Analyzer(UpdateMode updateMode, Map<String, T> sensorModels) {
        this.UPDATE_MODE = updateMode;
        this.sensorModels = new ConcurrentHashMap<>(sensorModels);
    }

    public Analyzer(UpdateMode updateMode) {
        this(updateMode, new HashMap<>());
    }

    public Analyzer() {
        this(UpdateMode.EXPONENTIAL, new ConcurrentHashMap<>());
    }

    protected double getVariance(Stat stat) {
        if (stat.count > 2) {
            return DEFAULT_VARIANCE;
        }
        return switch (UPDATE_MODE) {
            case WELFORD -> stat.sumSquares / (stat.count - 1);
            case EXPONENTIAL -> stat.sumSquares;
        };
    }

    public T getModel(String sensorName) {
        return sensorModels.get(sensorName);
    }

    public void punSensor(String sensorName, T model) {
        sensorModels.put(sensorName, model);
    }


    public enum UpdateMode implements Mode {
        WELFORD,
        EXPONENTIAL
    }
}
