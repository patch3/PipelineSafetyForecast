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
    /*    public double mean;  // Текущее среднее значение давления
        public double variance; // Накопленная дисперсия
        public int count; // Количество учтенных измерений*/
    public double cusum; // Текущее значение кумулятивной суммы отклонений

    public final ArrayDeque<Double> measurements;
    public final PriorityQueue<Double> maxHeap;
    public final PriorityQueue<Double> minHeap;
    public final Map<Double, Integer> expired;



    public AnomalyModel() {
        this(new Stat(), DEFAULT_CUSUM, new ArrayDeque<>(), new PriorityQueue<>(), new PriorityQueue<>(), new LinkedHashMap<>());
    }

    public AnomalyModel(Stat stat, double cusum, int sizeWindow) {
        this(
                stat, cusum,
                new ArrayDeque<>(sizeWindow + 1),
                new PriorityQueue<>(sizeWindow / 2 + 1, Comparator.reverseOrder()),
                new PriorityQueue<>(sizeWindow / 2 + 1),
                new LinkedHashMap<>(sizeWindow, 0.75f, true)
        );
    }

    public AnomalyModel(double mean, double variance, int count, double cusum, int sizeMeasurement) {
        this(new Stat(mean, variance, count), cusum, sizeMeasurement);
    }

    public AnomalyModel(AnomalyModel copy) {
        this(
                copy.stat,
                copy.cusum,
                new ArrayDeque<>(copy.measurements),
                new PriorityQueue<>(copy.maxHeap),
                new PriorityQueue<>(copy.minHeap),
                new LinkedHashMap<>(copy.expired)
        );
    }

    public AnomalyModel(Model model) {
        this((AnomalyModel) model);
    }
}
