package gas.pipeline.safety.forecast.util.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Builder
@ToString
@EqualsAndHashCode
@AllArgsConstructor
public class Stat {
    public double mean;
    public double sumSquares;
    public long count;

    public Stat() {
        this(0, 0, 0);
    }

    public Stat(double mean, double sumSquares) {
        this(mean, sumSquares, 0);
    }

    public Stat(Stat copy) {
        this(copy.mean, copy.sumSquares, copy.count);
    }
}
