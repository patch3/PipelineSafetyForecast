package gas.pipeline.safety.forecast.util;

import lombok.val;
import org.apache.commons.math3.distribution.NormalDistribution;

import java.util.HashMap;
import java.util.Map;

/**
 * Модель для байесовского прогнозирования утечек в газопроводах.
 * Использует данные с датчиков давления для вычисления апостериорной вероятности утечки.
 * Обновляет статистики (среднее, дисперсию) в реальном времени и применяет формулу Байеса.
 */
public class BayesianLeakModel {
    private final Map<String, Double> leakProbabilities;

    // Статистики для нормального режима работы
    private final Map<String, Double> normalMean;
    private final Map<String, Double> normalVariance;
    private final Map<String, Long> normalCount;

    // Статистики для режима утечки
    private final Map<String, Double> leakMean;
    private final Map<String, Double> leakVariance;
    private final Map<String, Long> leakCount;

    public BayesianLeakModel() {
        this.leakProbabilities = new HashMap<>();
        this.leakMean          = new HashMap<>();
        this.normalMean        = new HashMap<>();
        this.normalVariance    = new HashMap<>();
        this.normalCount       = new HashMap<>();
        this.leakVariance      = new HashMap<>();
        this.leakCount         = new HashMap<>();
    }

    public BayesianLeakModel(BayesianLeakModel copy) {
        this.leakProbabilities = new HashMap<>(copy.leakProbabilities);
        this.normalMean        = new HashMap<>(copy.normalMean);
        this.normalVariance    = new HashMap<>(copy.normalVariance);
        this.normalCount       = new HashMap<>(copy.normalCount);
        this.leakMean          = new HashMap<>(copy.leakMean);
        this.leakVariance      = new HashMap<>(copy.leakVariance);
        this.leakCount         = new HashMap<>(copy.leakCount);
    }

    /**
     * Обновляет статистики и пересчитывает вероятность утечки для датчика.
     *
     * @param sensorName идентификатор датчика
     * @param isLeak     флаг наличия утечки в текущем измерении
     * @param pressure   значение давления с датчика
     */
    public void update(String sensorName, boolean isLeak, double pressure) {
        // Обновление статистик в зависимости от режима (утечка/норма)
        if (isLeak) {
            updateStats(sensorName, pressure, leakMean, leakVariance, leakCount);
        } else {
            updateStats(sensorName, pressure, normalMean, normalVariance, normalCount);
        }

        // Расчет апостериорной вероятности по формуле Байеса
        val prior = calculatePrior(sensorName); // априорная вероятность утечки
        val likelihoodLeak = calculateGaussianLikelihood(
                pressure,
                leakMean.getOrDefault(sensorName, 0.0), // если данных нет, используется 0.0
                leakVariance.getOrDefault(sensorName, 1.0) // предотвращение нулевой дисперсии
        );
        val likelihoodNormal = calculateGaussianLikelihood(
                pressure,
                normalMean.getOrDefault(sensorName, 0.8), // базовое давление в норме
                normalVariance.getOrDefault(sensorName, 1.0)
        );

        // Формула Байеса: P(Утечка|Данные) = (P(Данные|Утечка) * P(Утечка)) / P(Данные)
        val posterior = (likelihoodLeak * prior) /
                (likelihoodLeak * prior + likelihoodNormal * (1 - prior));

        leakProbabilities.put(sensorName, posterior);
    }


    /**
     * Обновляет статистики (среднее и дисперсию) для датчика с использованием алгоритма Уэлфорда.
     *
     * @param sensorName идентификатор датчика
     * @param value      текущее значение давления
     * @param meanMap    карта для хранения средних значений
     * @param varMap     карта для хранения дисперсий
     * @param countMap   карта для хранения количества наблюдений
     */
    private void updateStats(String sensorName, double value,
                             Map<String, Double> meanMap,
                             Map<String, Double> varMap,
                             Map<String, Long> countMap) {
        val count = countMap.getOrDefault(sensorName, 0L);
        val oldMean = meanMap.getOrDefault(sensorName, 0.0);

        // Алгоритм Уэлфорда для инкрементального расчета среднего
        val newMean = oldMean + (value - oldMean) / (count + 1);
        meanMap.put(sensorName, newMean);

        // Инкрементальный расчет дисперсии (накапливаемая сумма квадратов отклонений)
        if (count > 0) {
            val oldVar = varMap.getOrDefault(sensorName, 0.0);
            val newVar = oldVar + (value - oldMean) * (value - newMean);
            varMap.put(sensorName, newVar);
        }
        countMap.put(sensorName, count + 1);
    }

    /**
     * Вычисляет вероятность значения по нормальному распределению.
     *
     * @param x        измеренное значение
     * @param mean     среднее распределения
     * @param variance дисперсия распределения
     * @return значение плотности вероятности (likelihood)
     */
    protected double calculateGaussianLikelihood(double x, double mean, double variance) {
        // Защита от нулевой или отрицательной дисперсии
        if (variance <= 0)
            variance = 1e-6;

        val dist = new NormalDistribution(mean, Math.sqrt(variance));
        return dist.density(x);
    }

    /**
     * Возвращает текущую вероятность утечки для датчика.
     *
     * @param sensorName идентификатор датчика
     * @return вероятность утечки (0.0, если данных недостаточно)
     */
    public double getLeakProbability(String sensorName) {
        // Требуется минимум 2 наблюдения для обеих статистик
        if (leakCount.getOrDefault(sensorName, 0L) < 2
                || normalCount.getOrDefault(sensorName, 0L) < 2) {
            return 0.0; // Недостаточно данных для надежной оценки
        }
        return leakProbabilities.getOrDefault(sensorName, 0.0);
    }

    /**
     * Вычисляет априорную вероятность утечки на основе исторических данных.
     *
     * @param sensorName идентификатор датчика
     * @return отношение числа утечек к общему количеству наблюдений (минимум 1%)
     */
    private double calculatePrior(String sensorName) {
        val normal = normalCount.getOrDefault(sensorName, 0L);
        val leak = leakCount.getOrDefault(sensorName, 0L);
        val total = normal + leak;


        if (total == 0) return 0.01; // для избежания нулевых вероятностей
        return (double) leak / total;
    }
}