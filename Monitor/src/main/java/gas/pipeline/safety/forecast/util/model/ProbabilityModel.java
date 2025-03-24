package gas.pipeline.safety.forecast.util.model;

import lombok.*;

@Builder
@ToString
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
public class ProbabilityModel extends Model {
    public static final double DEFAULT_PROBABILITY = 0.001;
    private static final double SMOOTHING_ALPHA = 1.0;

    // Статистики для режима утечки
    public final Stat leak;
    // Статистики для нормального режима работы
    public final Stat normal;

    @Setter
    private double leakProbability;

    public ProbabilityModel() {
        this(new Stat(), new Stat(), DEFAULT_PROBABILITY);
    }

    public ProbabilityModel(ProbabilityModel copy) {
        this(copy.leak, copy.normal, copy.leakProbability);
    }

    public ProbabilityModel(double mean, double sumSquares, double leakProbability) {
        this(new Stat(mean, sumSquares), new Stat(mean, sumSquares), leakProbability);
    }

    /**
     * Возвращает текущую вероятность утечки для датчика.
     *
     * @return вероятность утечки (0.0, если данных недостаточно)
     */
    public double getLeakProbability() {
        // Применяем сглаживание Лапласа
        double smoothedLeakCount = leak.count + SMOOTHING_ALPHA;
        double smoothedTotal = leak.count + normal.count + 2 * SMOOTHING_ALPHA;

        // Если данных недостаточно, возвращаем дефолтное значение
        if (smoothedTotal <= 0) {
            return DEFAULT_PROBABILITY;
        }

        return smoothedLeakCount / smoothedTotal;
    }
}
