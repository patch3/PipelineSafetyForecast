package gas.pipeline.safety.forecast.util.analyzer;

import gas.pipeline.safety.forecast.util.model.ProbabilityModel;
import gas.pipeline.safety.forecast.util.model.Stat;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.math3.distribution.NormalDistribution;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static gas.pipeline.safety.forecast.util.model.ProbabilityModel.DEFAULT_PROBABILITY;

/**
 * Модель для байесовского прогнозирования утечек в газопроводах.
 * Использует данные с датчиков давления для вычисления апостериорной вероятности утечки.
 * Обновляет статистики (среднее, дисперсию) в реальном времени и применяет формулу Байеса.
 */
@Slf4j
public class BayesianTheorem extends Analyzer<ProbabilityModel> implements IProbability {
    private final double LEAK_DECAY_FACTOR;
    private final double NORMAL_DECAY_FACTOR;


    public BayesianTheorem(double LEAK_DECAY_FACTOR,
                           double NORMAL_DECAY_FACTOR,
                           UpdateMode UPDATE_MODE,
                           Map<String, ProbabilityModel> model) {
        super(UPDATE_MODE, model);
        this.LEAK_DECAY_FACTOR = LEAK_DECAY_FACTOR;
        this.NORMAL_DECAY_FACTOR = NORMAL_DECAY_FACTOR;
    }

    public BayesianTheorem() {
        this(0.7, 0.2, UpdateMode.EXPONENTIAL);
    }

    public BayesianTheorem(double leakDecayFactor, double normalDecayFactor, UpdateMode updateMode) {
        this(leakDecayFactor, normalDecayFactor, updateMode, new ConcurrentHashMap<>());
    }

    public BayesianTheorem(BayesianTheorem copy) {
        this(
                copy.LEAK_DECAY_FACTOR,
                copy.NORMAL_DECAY_FACTOR,
                copy.UPDATE_MODE,
                copy.sensorModels.entrySet().stream()
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                e -> new ProbabilityModel(e.getValue()) // Глубокая копия BayesianModel
                        ))
        );
    }

    public BayesianTheorem(IProbability probabilityCopy) {
        this(cast(probabilityCopy));
    }

    private static BayesianTheorem cast(IProbability probability) {
        if (probability instanceof BayesianTheorem) {
            return (BayesianTheorem) probability;
        } else {
            throw new IllegalArgumentException("BayesianTheorem can't cast to " + probability.getClass().getSimpleName());
        }
    }


    /**
     * Обновляет статистики и пересчитывает вероятность утечки для датчика.
     *
     * @param sensorName идентификатор датчика
     * @param isArtifact флаг наличия утечки в текущем измерении
     * @param pressure   значение давления с датчика
     */
    public void update(String sensorName, boolean isArtifact, double pressure) {
        log.debug("Updating sensor {} with pressure={}, isArtifact={}", sensorName, pressure, isArtifact);

        var model = sensorModels.get(sensorName);
        if (model == null) {
            model = ProbabilityModel.builder()
                    .leak(Stat.builder()
                            .mean(pressure)
                            .sumSquares(pressure)
                            .count(0)
                            .build())
                    .normal(Stat.builder()
                            .mean(pressure)
                            .sumSquares(pressure)
                            .count(0)
                            .build())
                    .leakProbability(DEFAULT_PROBABILITY)
                    .build();
            sensorModels.put(sensorName, model);
        }

        // Обновление статистик в зависимости от режима (утечка/норма)`
        if (isArtifact) {
            updateStats(model.leak, pressure, true);
        } else {
            updateStats(model.normal, pressure, false);
        }

        // Расчет апостериорной вероятности по формуле Байеса
        val prior = getPrior(model); // априорная вероятность утечки
        val likelihoodLeak = calculateGaussianLikelihood(
                pressure, model.leak.mean, getVariance(model.leak)
        );
        val likelihoodNormal = calculateGaussianLikelihood(
                pressure, model.normal.mean, getVariance(model.normal)
        );

        // Формула Байеса: P(Утечка|Данные) = (P(Данные|Утечка) * P(Утечка)) / P(Данные)
        //(likelihoodLeak * prior) / (likelihoodLeak * prior + likelihoodNormal * (1 - prior));

        val denominator = likelihoodLeak * prior + likelihoodNormal * (1 - prior); // вдруг основание равно нулю
        model.setLeakProbability((denominator == 0) ? prior : (likelihoodLeak * prior) / denominator);

        log.info("Прогноз вероятности для {} = {}", sensorName, model.getLeakProbability());
    }


    /**
     * Обновляет статистики (среднее и дисперсию) для датчика.
     */
    protected void updateStats(Stat stat, double value, boolean isArtifact) {
        switch (UPDATE_MODE) {
            case EXPONENTIAL -> updateExponential(stat, value, isArtifact);
            case WELFORD -> updateWelford(stat, value);
        }
    }

    /**
     * Обновление: Экспоненциальное сглаживание.
     */
    protected void updateExponential(Stat stat, double value, boolean isArtifact) {
        val decay = isArtifact ? LEAK_DECAY_FACTOR : NORMAL_DECAY_FACTOR;

        stat.mean = (stat.count == 0) ? value : stat.mean + decay * (value - stat.mean);

        if (stat.count > 0) {
            val delta = value - stat.mean;
            stat.sumSquares = (1 - decay) * stat.sumSquares + decay * (delta * delta);
        }
        stat.count += 1;
        //log.info("Экспоненциальное обновление: newMean={}, sumSquares={}", stat.mean, stat.sumSquares);
    }

    /**
     * Обновление: Алгоритм Уэлфорда.
     */
    protected void updateWelford(Stat stat, double value) {
        val oldMean = stat.mean;
        stat.mean = oldMean + (value - oldMean) / (stat.count + 1);

        // Обновление суммы квадратов
        if (stat.count > 0) {
            val delta = value - oldMean;
            val delta2 = value - stat.mean;
            stat.sumSquares = stat.sumSquares + delta * delta2;
        }
        stat.count += 1;
        log.debug("Обновление Уэлфорда: newMean={}, sumSquares={}", stat.mean, stat.sumSquares);
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
            variance = DEFAULT_PROBABILITY;

        val dist = new NormalDistribution(mean, Math.sqrt(variance));
        return dist.density(x);
    }


    /**
     * Вычисляет априорную вероятность утечки на основе исторических данных.
     * Сглаживание лапласа
     *
     * @param model модель со статистикой
     * @return отношение числа утечек к общему количеству наблюдений (минимум 1%)
     */
    protected double getPrior(ProbabilityModel model) {
        val total = model.normal.count + model.leak.count;
        if (model.leak.count == 0 || total == 0) return DEFAULT_PROBABILITY;

        // Добавляем сглаживание Лапласа для стабильности
        val leakCount = model.leak.count + 1.0;
        val normalCount = model.normal.count + 1.0;
        return leakCount / (leakCount + normalCount);
    }
}