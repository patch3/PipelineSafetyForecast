package gas.pipeline.safety.forecast.util.analyzer;

import gas.pipeline.safety.forecast.util.model.AnomalyModel;
import gas.pipeline.safety.forecast.util.model.Stat;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.ArrayList;
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

    private final double CUSUM_THRESHOLD; // Порог для кумулятивного суммирования отклонений (CUSUM)
    private final double LEAK_THRESHOLD; // Порог Z-скор для мгновенного обнаружения аномалий
    private final double DECAY_FACTOR;
    private final int NUM_CALIBRATION_RECORDS;
    private final int FILTER_WINDOW;
    private final FilterMode FILTER_TYPE;


    public AnomalyAnalyzer() {
        this(7.0, 4.0, 0.9, 20, 10,
                FilterMode.MOVING_AVERAGE, UpdateMode.EXPONENTIAL, new ConcurrentHashMap<>());
    }

    public AnomalyAnalyzer(double cusumThreshold,
                           double leakThreshold,
                           double decayFactor,
                           int numCalibrationRecords,
                           int filterWindow,
                           FilterMode filterMode,
                           UpdateMode updateMode,
                           Map<String, AnomalyModel> sensorStats) {
        super(updateMode, sensorStats);
        this.CUSUM_THRESHOLD = cusumThreshold;
        this.LEAK_THRESHOLD = leakThreshold;
        this.DECAY_FACTOR = decayFactor;
        this.NUM_CALIBRATION_RECORDS = numCalibrationRecords;
        this.FILTER_WINDOW = filterWindow;
        this.FILTER_TYPE = filterMode;
    }

    public AnomalyAnalyzer(double cusumThreshold,
                           double leakThreshold,
                           double decayFactor,
                           int numCalibrationRecords,
                           int filterWindow,
                           FilterMode filterMode,
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
                copy.CUSUM_THRESHOLD,
                copy.LEAK_THRESHOLD,
                copy.DECAY_FACTOR,
                copy.NUM_CALIBRATION_RECORDS,
                copy.FILTER_WINDOW,
                copy.FILTER_TYPE,
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
                    FILTER_WINDOW
            );
            sensorModels.put(sensorName, model);
        }

        val filteredPressure = applyFilter(model, pressure);

        // Первые NUM_CALIBRATION_RECORDS измерений для накопления статистики
        if (model.stat.count < NUM_CALIBRATION_RECORDS) {
            updateStats(model.stat, filteredPressure);
            return false;
        }
        val isLeak = checkAnomaly(model, filteredPressure);
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

        return zScore > LEAK_THRESHOLD || model.cusum > CUSUM_THRESHOLD;
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
        stat.mean = oldMean + (value - oldMean) / (stat.count + 1);
        val newDelta = value - stat.mean;

        if (stat.count > 0) {
            stat.sumSquares += (value - oldMean) * newDelta;
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
            stat.mean = stat.mean * (1 - DECAY_FACTOR) + value * DECAY_FACTOR;
            // Обновление суммы квадратов отклонений
            val delta = value - stat.mean;
            stat.sumSquares = (1 - DECAY_FACTOR) * stat.sumSquares + DECAY_FACTOR * delta * delta;
        }
        stat.count += 1;
    }


    protected double applyFilter(AnomalyModel model, double pressure) {
        return switch (FILTER_TYPE) {
            case MOVING_AVERAGE -> applyMovingAverage(model, pressure);
            case MEDIAN -> applyMedianFilter(model, pressure);
        };
    }

    /**
     * Сглаживание по медиане
     */
    protected double applyMovingAverage(AnomalyModel model, double value) {
        model.measurements.addLast(value);
        if (model.measurements.size() > FILTER_WINDOW) {
            model.measurements.removeFirst();
        }
        return model.measurements.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(value);
    }

    /**
     * Сглаживание по медиане
     */
    protected double applyMedianFilter(AnomalyModel model, double value) {
        // Добавление нового элемента
        model.measurements.add(value);

        if (model.maxHeap.isEmpty() || value <= model.maxHeap.peek()) {
            model.maxHeap.offer(value);
        } else {
            model.minHeap.offer(value);
        }

        // Удаление старого элемента при переполнении окна
        if (model.measurements.size() > FILTER_WINDOW) {
            val oldest = model.measurements.poll();
            model.expired.put(oldest, model.expired.getOrDefault(oldest, 0) + 1);
        }

        // Удаление устаревших элементов из куч
        pruneHeap(model.maxHeap, model.expired);
        pruneHeap(model.minHeap, model.expired);


        // Балансировка куч после очистки
        while (model.maxHeap.size() > model.minHeap.size() + 1) {
            model.minHeap.offer(model.maxHeap.poll());
        }
        while (model.minHeap.size() > model.maxHeap.size()) {
            model.maxHeap.offer(model.minHeap.poll());
        }

        // Вычисление медианы
        if (model.maxHeap.isEmpty() && model.minHeap.isEmpty()) {
            return 0.0; // Обработка случая пустого окна
        }

        if (model.maxHeap.size() == model.minHeap.size()) {
            return (model.maxHeap.peek() + model.minHeap.peek()) / 2.0;
        } else {
            return model.maxHeap.peek();
        }
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


    /**
     * Предсказание следующей итерации давления
     */
    @Override
    public double predictNextPressure(AnomalyModel model) {
        if (model == null || model.stat == null) {
            return 0.0;
        } else if (model.stat.count < NUM_CALIBRATION_RECORDS) {
            return model.stat.mean;
        }

        if (model.measurements == null || model.measurements.isEmpty()) {
            return model.stat.mean;
        }

        val filteredData = new ArrayList<>(model.measurements);
        // защита от одинаковых значений (предотвращает деление на ноль)
        if (filteredData.stream().allMatch(v -> v.equals(filteredData.getFirst()))) {
            return filteredData.getFirst();
        }

        // расчет тренда с ограничениями
        val x = new double[filteredData.size()];
        val y = new double[filteredData.size()];
        for (int i = 0; i < filteredData.size(); i++) {
            x[i] = i;
            y[i] = filteredData.get(i);
        }

        var slope = calculateSlop(x, y);

        // ограничение скорости изменения на основе статистики
        val maxAllowedSlope = EXP_SMOOTHING_ALPHA * Math.sqrt(model.stat.sumSquares);
        slope = Math.max(-maxAllowedSlope, Math.min(slope, maxAllowedSlope));

        // Прогноз и физические ограничения
        var predicted = y[y.length - 1] + slope;
        predicted = Math.max(0.0, predicted); // Давление не может быть отрицательным

        log.info("Прогноз давления для: slope={}, predicted={}", slope, predicted);
        return predicted;
    }

    /**
     * Линейная регрессия
     */
    protected double calculateSlop(double[] x, double[] y) {
        val n = x.length;
        if (n < 2) return 0.0;

        double sumXY = 0, sumX = 0, sumY = 0, sumX2 = 0;
        for (int i = 0; i < n; i++) {
            sumXY += x[i] * y[i];
            sumX += x[i];
            sumY += y[i];
            sumX2 += x[i] * x[i];
        }

        val denominator = n * sumX2 - sumX * sumX;
        if (Math.abs(denominator) < 1e-10) return 0.0;

        return (n * sumXY - sumX * sumY) / denominator;
    }


    public enum FilterMode implements Analyzer.Mode {
        MOVING_AVERAGE,
        MEDIAN
    }
}
