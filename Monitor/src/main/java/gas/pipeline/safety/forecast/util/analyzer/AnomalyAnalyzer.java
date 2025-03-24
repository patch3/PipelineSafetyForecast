package gas.pipeline.safety.forecast.util.analyzer;

import gas.pipeline.safety.forecast.util.analyzer.filter.FilterStrategy;
import gas.pipeline.safety.forecast.util.analyzer.filter.MedianFilter;
import gas.pipeline.safety.forecast.util.model.AnomalyModel;
import gas.pipeline.safety.forecast.util.model.Stat;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static gas.pipeline.safety.forecast.util.model.AnomalyModel.DEFAULT_CUSUM;


/**
 * Анализатор давления для обнаружения утечек в трубопроводах.
 * Использует статистические методы (Z-скор и CUSUM) для выявления аномалий в показаниях датчиков давления.
 * <p>
 * Основной алгоритм:
 * - Собирает статистику по первым NUM_CALIBRATION_RECORDS измерениям для каждого датчика.
 * - Вычисляет отклонения последующих измерений от накопленной статистики.
 * - Срабатывает при превышении пороговых значений отклонения (Z-скор) или кумулятивной суммы (CUSUM).
 */
@Slf4j
public class AnomalyAnalyzer extends Analyzer<AnomalyModel> implements IAnomaly {
    private static final double EXP_SMOOTHING_ALPHA = 0.5;

    private final double cusumThreshold; // Порог для кумулятивного суммирования отклонений (CUSUM)
    private final double leakThreshold; // Порог Z-скор для мгновенного обнаружения аномалий
    private final double decayFactor;
    private final int numCalibrationRecords;
    private final int filterWindow;
    private final FilterStrategy filter;


    public AnomalyAnalyzer() {
        this(7.0, 4.0, 0.9, 20, 10,
                new MedianFilter(), UpdateMode.EXPONENTIAL, new ConcurrentHashMap<>());
    }

    public AnomalyAnalyzer(double cusumThreshold,
                           double leakThreshold,
                           double decayFactor,
                           int numCalibrationRecords,
                           int filterWindow,
                           FilterStrategy filterMode,
                           UpdateMode updateMode,
                           Map<String, AnomalyModel> sensorStats) {
        super(updateMode, sensorStats);
        this.cusumThreshold = cusumThreshold;
        this.leakThreshold = leakThreshold;
        this.decayFactor = decayFactor;
        this.numCalibrationRecords = numCalibrationRecords;
        this.filterWindow = filterWindow;
        this.filter = filterMode;
    }

    public AnomalyAnalyzer(double cusumThreshold,
                           double leakThreshold,
                           double decayFactor,
                           int numCalibrationRecords,
                           int filterWindow,
                           FilterStrategy filterMode,
                           UpdateMode updateMode) {
        this(
                cusumThreshold,
                leakThreshold,
                decayFactor,
                numCalibrationRecords,
                filterWindow,
                filterMode,
                updateMode,
                new ConcurrentHashMap<>()
        );
    }

    public AnomalyAnalyzer(AnomalyAnalyzer copy) {
        this(
                copy.cusumThreshold,
                copy.leakThreshold,
                copy.decayFactor,
                copy.numCalibrationRecords,
                copy.filterWindow,
                copy.filter,
                copy.UPDATE_MODE,
                copy.sensorModels.entrySet().stream()
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                e -> new AnomalyModel(e.getValue())
                        ))
        );
    }

    public AnomalyAnalyzer(IAnomaly anomaly) {
        this(cast(anomaly));
    }

    private static AnomalyAnalyzer cast(IAnomaly anomaly) {
        if (anomaly instanceof AnomalyAnalyzer) {
            return (AnomalyAnalyzer) anomaly;
        } else {
            throw new IllegalArgumentException("AnomalyAnalyzer can't cast to " + anomaly.getClass().getSimpleName());
        }
    }


    /**
     * Анализирует текущее показание давления для указанного датчика.
     *
     * @param sensorName уникальный идентификатор датчика
     * @param pressure   текущее значение давления
     * @return true - обнаружена утечка, false - аномалий нет
     */
    @Override
    public boolean analyzePressure(String sensorName, double pressure) {
        var model = sensorModels.get(sensorName);
        if (model == null) {
            // статистика для датчика
            model = new AnomalyModel(
                    Stat.builder()
                            .mean(DEFAULT_MEAN)
                            .sumSquares(DEFAULT_SEM_SQUARED)
                            .count(0)
                            .build(),
                    DEFAULT_CUSUM,
                    filterWindow
            );
            sensorModels.put(sensorName, model);
        }

        val filteredPressure = filter.applyFilter(model, pressure);

        // Первые NUM_CALIBRATION_RECORDS измерений для накопления статистики
        if (model.stat.count < numCalibrationRecords) {
            updateStats(model.stat, filteredPressure);
            return false;
        }
        var isLeak = checkAnomaly(model, filteredPressure);
        // Обновляем статистику только при нормальных показаниях
        if (!isLeak) {
            updateStats(model.stat, filteredPressure);
        }
        return isLeak;
    }


    /**
     * Проверяет показание на аномалию с использованием Z-скор и CUSUM.
     *
     * @param model объект модели анализа аномалий
     * @param value проверяемое значение давления
     * @return true - обнаружена аномалия, false - нормальное значение
     */
    protected boolean checkAnomaly(AnomalyModel model, double value) {
        if (model.stat.count <= 1) return false;

        // расчет стандартного отклонения
        val stdDev = Math.sqrt(model.stat.sumSquares / (model.stat.count - 1));
        // количество сигм от среднего
        val zScore = Math.abs((value - model.stat.mean) / stdDev);

        // кумулятивная сумма отклонений с "дрейфом" 1 сигмы
        model.cusum = Math.max(0, model.stat.count + (value - model.stat.mean) / stdDev - 1);

        return zScore > leakThreshold || model.cusum > cusumThreshold;
    }

    /**
     * Обновляет статистики (среднее и дисперсию) для датчика.
     */
    protected void updateStats(Stat stat, double value) {
        switch (UPDATE_MODE) {
            case EXPONENTIAL -> updateExponential(stat, value);
            case WELFORD -> updateWelford(stat, value);
        }
    }

    /**
     * Алгоритм Уэлфорда (как в текущей реализации).
     */
    protected void updateWelford(Stat stat, double value) {
        val oldMean = stat.mean;
        val delta = value - oldMean;
        stat.mean = oldMean + delta / (stat.count + 1);
        val newDelta = value - stat.mean;

        if (stat.count > 0) {
            stat.sumSquares += delta * newDelta;
        }
        stat.count += 1;
    }

    /**
     * Экспоненциальное сглаживание с DECAY_FACTOR.
     */
    protected void updateExponential(Stat stat, double value) {
        if (stat.count == 0) {
            stat.mean = value;
            stat.sumSquares = 0;
        } else {
            // Обновление среднего
            stat.mean = stat.mean * (1 - decayFactor) + value * decayFactor;
            // Обновление суммы квадратов отклонений
            val delta = value - stat.mean;
            stat.sumSquares = (1 - decayFactor) * stat.sumSquares + decayFactor * delta * delta;
        }
        stat.count += 1;
    }


    private void pruneHeap(PriorityQueue<Double> heap, Map<Double, Integer> expired) {
        while (!heap.isEmpty() && expired.getOrDefault(heap.peek(), 0) > 0) {
            val val = heap.poll();
            val count = expired.get(val);
            if (count == 1) {
                expired.remove(val);
            } else {
                expired.put(val, count - 1);
            }
        }
    }
}