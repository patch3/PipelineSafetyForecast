package gas.pipeline.safety.lot.sensor;

import lombok.val;

import java.util.logging.Logger;

public class Sensor {
    protected static final Logger logger = Logger.getLogger(Sensor.class.getName());

    public double getPressure() {
        val pressure = 1.0;
        // TODO реализовать получение давления
        logger.info("Get pressure: " + pressure);
        return pressure;
    }
}
