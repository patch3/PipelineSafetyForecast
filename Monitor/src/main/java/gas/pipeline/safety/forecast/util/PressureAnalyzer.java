package gas.pipeline.safety.forecast.util;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.val;

import java.util.*;


/**
 * Анализатор давления для обнаружения утечек в трубопроводах.
 * Использует статистические методы (Z-скор и CUSUM) для выявления аномалий в показаниях датчиков давления.
 * <p>
 * Основной алгоритм:
 * - Собирает статистику по первым NUM_CALIBRATION_RECORDS измерениям для каждого датчика.
 * - Вычисляет отклонения последующих измерений от накопленной статистики.
 * - Срабатывает при превышении пороговых значений отклонения (Z-скор) или кумулятивной суммы (CUSUM).
 */
public class PressureAnalyzer {
    private final double CUSUM_THRESHOLD; // Порог для кумулятивного суммирования отклонений (CUSUM)
    private final double LEAK_THRESHOLD; // Порог Z-скор для мгновенного обнаружения аномалий
    private final int NUM_CALIBRATION_RECORDS;
    private final int FILTER_WINDOW;
    private final FilterType FILTER_TYPE;

    @Getter
    private final Map<String, SensorStats> sensorStats;
    private final Map<String, ArrayDeque<Double>> sensorMeasurements;


    public PressureAnalyzer() {
        this(7, 4, 20, 10, FilterType.MOVING_AVERAGE);
    }

    public PressureAnalyzer(double cusumThreshold,
                            double leakThreshold,
                            int numCalibrationRecords,
                            int filterWindow,
                            FilterType filterType) {
        this.CUSUM_THRESHOLD = cusumThreshold;
        this.LEAK_THRESHOLD = leakThreshold;
        this.NUM_CALIBRATION_RECORDS = numCalibrationRecords;
        this.FILTER_WINDOW = filterWindow;
        this.FILTER_TYPE = filterType;
        this.sensorStats = new HashMap<>();
        this.sensorMeasurements = new HashMap<>();
    }

    public PressureAnalyzer(PressureAnalyzer copy) {
        this.CUSUM_THRESHOLD = copy.CUSUM_THRESHOLD;
        this.LEAK_THRESHOLD = copy.LEAK_THRESHOLD;
        this.NUM_CALIBRATION_RECORDS = copy.NUM_CALIBRATION_RECORDS;
        this.FILTER_WINDOW = copy.FILTER_WINDOW;
        this.FILTER_TYPE = copy.FILTER_TYPE;
        this.sensorStats = new HashMap<>(copy.sensorStats);
        this.sensorMeasurements = new HashMap<>(copy.sensorMeasurements);
    }

    /**
     * Анализирует текущее показание давления для указанного датчика.
     *
     * @param sensorName уникальный идентификатор датчика
     * @param pressure   текущее значение давления
     * @return true - обнаружена утечка, false - аномалий нет
     */
    public boolean analyzePressure(String sensorName, double pressure) {
        val filteredPressure = applyFilter(sensorName, pressure);

        // статистика для датчика
        val stats = sensorStats.computeIfAbsent(sensorName,
                k -> SensorStats.builder()
                        .count(0)
                        .mean(filteredPressure)
                        .variance(0)
                        .cusum(0)
                        .build()
        );

        // Первые NUM_CALIBRATION_RECORDS измерений для накопления статистики
        if (stats.getCount() < NUM_CALIBRATION_RECORDS) {
            updateStats(stats, pressure);
            return false;
        }

        val isLeak = checkAnomaly(stats, pressure);

        // Обновляем статистику только при нормальных показаниях
        if (!isLeak) {
            updateStats(stats, pressure);
        } else {
            return isLeak; // для точки дебага
        }
        return isLeak;
    }

    /**
     * Обновляет статистические показатели для датчика.
     * Используется алгоритм устойчивого вычисления среднего и дисперсии.
     *
     * @param stats объект статистики датчика
     * @param value новое значение давления
     */
    private void updateStats(SensorStats stats, double value) {
        val newCount = stats.getCount() + 1;
        // Вычисление дельты для инкрементального среднего
        val delta = value - stats.getMean();
        val newMean = stats.getMean() + delta / newCount;
        val newDelta = value - newMean;

        val newVariance = ((stats.getVariance() * (stats.getCount() - 1)) + delta * newDelta) / newCount;

        stats.setMean(newMean);
        stats.setVariance(newVariance);
        stats.setCount(newCount);
    }

    /**
     * Проверяет показание на аномалию с использованием Z-скор и CUSUM.
     *
     * @param stats объект статистики датчика
     * @param value проверяемое значение давления
     * @return true - обнаружена аномалия, false - нормальное значение
     */
    private boolean checkAnomaly(SensorStats stats, double value) {
        if (stats.getCount() <= 1) return false;

        // расчет стандартного отклонения
        val stdDev = Math.sqrt(stats.getVariance() / (stats.getCount() - 1));
        // количество сигм от среднего
        val zScore = Math.abs((value - stats.getMean()) / stdDev);

        // кумулятивная сумма отклонений с "дрейфом" 0.5 сигмы
        val cusum = Math.max(0, stats.getCusum() + (value - stats.getMean()) / stdDev - 0.5);

        stats.setCusum(cusum);

        return zScore > LEAK_THRESHOLD || cusum > CUSUM_THRESHOLD;
    }

    private double applyFilter(String sensorName, double pressure) {
        return switch (FILTER_TYPE) {
            case MOVING_AVERAGE -> applyMovingAverage(sensorName, pressure);
            case MEDIAN -> applyMedianFilter(sensorName, pressure);
        };
    }

    /**
     * Фильтрация по скользящей средней
     */
    private double applyMovingAverage(String sensorName, double value) {
        Deque<Double> measurements = sensorMeasurements.computeIfAbsent(
                sensorName,
                k -> new ArrayDeque<>(FILTER_WINDOW)
        );
        measurements.addLast(value);
        if (measurements.size() > FILTER_WINDOW) {
            measurements.removeFirst();
        }
        return measurements.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(value);
    }

    /**
     * Фильтрация по медиане
     */
    private double applyMedianFilter(String sensorName, double value) {
        val measurements = sensorMeasurements.computeIfAbsent(
                sensorName,
                k -> new ArrayDeque<>(FILTER_WINDOW)
        );
        measurements.addLast(value);
        if (measurements.size() > FILTER_WINDOW) {
            measurements.removeFirst();
        }
        val sorted = new ArrayList<>(measurements);
        Collections.sort(sorted);

        val middle = sorted.size() / 2;
        if (sorted.size() % 2 == 0) {
            return (sorted.get(middle - 1) + sorted.get(middle)) / 2.0;
        } else {
            return sorted.get(middle);
        }
    }

    public double predictNextPressure(String sensorName) {
        val stats = sensorStats.get(sensorName);
        if (stats == null || stats.getCount() < NUM_CALIBRATION_RECORDS) {
            return stats != null ? stats.getMean() : 0.0;
        }
        val measurements = sensorMeasurements.get(sensorName);
        if (measurements == null || measurements.isEmpty()) {
            return stats.getMean();
        }
        val x = new double[measurements.size()];
        val y = new double[measurements.size()];

        var i = 0;
        for (val measure : measurements) {
            x[i] = i;
            y[i] = measure;
            i++;
        }
        val slop = calculateSlop(x, y);
        return y[y.length - 1] + slop;
    }

    /**
     * Линейная регрессия
     */
    private double calculateSlop(double[] x, double[] y) {
        val n = x.length;
        double sumXY = 0, sumX = 0, sumY = 0, sumX2 = 0;
        for (int i = 0; i < n; i++) {
            sumXY += x[i] * y[i];
            sumX += x[i];
            sumY += y[i];
            sumX2 += x[i] * x[i];
        }
        return (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
    }


    public SensorStats getSensorStats(String sensorName) {
        return sensorStats.get(sensorName);
    }

    public enum FilterType {
        MOVING_AVERAGE,
        MEDIAN;

        public static Optional<FilterType> fromString(String text) {
            if (text == null) return Optional.empty();
            for (FilterType type : FilterType.values()) {
                if (type.name().equalsIgnoreCase(text)) {
                    return Optional.of(type);
                }
            }
            return Optional.empty();
        }
    }

    @Data
    @Builder
    public static class SensorStats {
        private double mean;  // Текущее среднее значение давления
        private double variance; // Накопленная дисперсия
        private int count; // Количество учтенных измерений
        private double cusum; // Текущее значение кумулятивной суммы отклонений
    }
}
