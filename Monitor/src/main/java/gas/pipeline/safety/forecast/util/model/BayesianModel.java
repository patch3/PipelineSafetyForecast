package gas.pipeline.safety.forecast.util.model;

import lombok.*;

@Builder
@ToString
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
public class BayesianModel extends Model {
    public static final double DEFAULT_PROBABILITY = 0.001;

    // Статистики для режима утечки
    public final Stat leak;
    // Статистики для нормального режима работы
    public final Stat normal;

    @Setter
    private double leakProbability;

    public BayesianModel() {
        this(new Stat(), new Stat(), DEFAULT_PROBABILITY);
    }

    public BayesianModel(BayesianModel copy) {
        this(copy.leak, copy.normal, copy.leakProbability);
    }

    public BayesianModel(double mean, double sumSquares, double leakProbability) {
        this(new Stat(mean, sumSquares), new Stat(mean, sumSquares), leakProbability);
    }

    /**
     * Возвращает текущую вероятность утечки для датчика.
     *
     * @return вероятность утечки (0.0, если данных недостаточно)
     */
    public double getLeakProbability() {
        // Требуется минимум 2 наблюдения для обеих статистик
        if (leak.count < 2 || normal.count < 2) {
            return DEFAULT_PROBABILITY; // Недостаточно данных для надежной оценки
        }
        return leakProbability;
    }
}
