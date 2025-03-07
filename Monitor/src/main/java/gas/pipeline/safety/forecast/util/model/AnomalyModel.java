package gas.pipeline.safety.forecast.util.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.*;

@ToString
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
public class AnomalyModel extends Model {
    public static final double DEFAULT_CUSUM = 0.5;

    public Stat stat;
    public double cusum; // Текущее значение кумулятивной суммы отклонений

    public final ArrayDeque<Double> measurements;
    public final PriorityQueue<Double> maxHeap;
    public final PriorityQueue<Double> minHeap;
    public final Map<Double, Integer> expired;

    public final ArrayDeque<Double> maErrors;
    public final int arimaQ;



    public AnomalyModel() {
        this(new Stat(),
                DEFAULT_CUSUM,
                new ArrayDeque<>(),
                new PriorityQueue<>(),
                new PriorityQueue<>(),
                new LinkedHashMap<>(),
                new ArrayDeque<>(11),
                10
        );
    }

    public AnomalyModel(Stat stat, double cusum, int sizeWindow, int arimaQ) {
        this(
                stat, cusum,
                new ArrayDeque<>(sizeWindow + 1),
                new PriorityQueue<>(sizeWindow / 2 + 1, Comparator.reverseOrder()),
                new PriorityQueue<>(sizeWindow / 2 + 1),
                new LinkedHashMap<>(sizeWindow, 0.75f, true),
                new ArrayDeque<>(11),
                10
        );
    }

    public AnomalyModel(double mean,
                        double variance,
                        int count,
                        double cusum,
                        int sizeMeasurement,
                        int arimaQ) {
        this(new Stat(mean, variance, count), cusum, sizeMeasurement, arimaQ);
    }

    public AnomalyModel(AnomalyModel copy) {
        this(
                copy.stat,
                copy.cusum,
                copy.measurements,
                copy.maxHeap,
                copy.minHeap,
                copy.expired,
                copy.maErrors,
                copy.arimaQ
        );
    }

    public AnomalyModel(Model model) {
        this((AnomalyModel) model);
    }
}
