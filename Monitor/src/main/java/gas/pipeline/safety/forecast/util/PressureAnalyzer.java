package gas.pipeline.safety.forecast.util;

import lombok.Builder;
import lombok.Data;
import lombok.val;

import java.util.HashMap;
import java.util.Map;


/**
 * Анализатор давления для обнаружения утечек в трубопроводах.
 * Использует статистические методы (Z-скор и CUSUM) для выявления аномалий в показаниях датчиков давления.
 * <p>
 * Основной алгоритм:
 * - Собирает статистику по первым 10 измерениям для каждого датчика (калибровка).
 * - Вычисляет отклонения последующих измерений от накопленной статистики.
 * - Срабатывает при превышении пороговых значений отклонения (Z-скор) или кумулятивной суммы (CUSUM).
 */
public class PressureAnalyzer {
    /**
     * Порог для кумулятивного суммирования отклонений (CUSUM)
     */
    private final double CUSUM_THRESHOLD;
    /**
     * Порог Z-скор для мгновенного обнаружения аномалий
     */
    private final double LEAK_THRESHOLD;

    private final int NUM_CALIBRATION_RECORDS;

    // Статистика по каждому датчику: ключ - идентификатор датчика
    private final Map<String, SensorStats> sensorStats = new HashMap<>();



    public PressureAnalyzer() {
        this.CUSUM_THRESHOLD = 7.0;
        this.LEAK_THRESHOLD = 4.0;
        this.NUM_CALIBRATION_RECORDS = 20;
    }

    public PressureAnalyzer(double cusumThreshold,
                            double leakThreshold,
                            int numCalibrationRecords) {
        this.CUSUM_THRESHOLD = cusumThreshold;
        this.LEAK_THRESHOLD = leakThreshold;
        this.NUM_CALIBRATION_RECORDS = numCalibrationRecords;
    }

    /**
     * Анализирует текущее показание давления для указанного датчика.
     *
     * @param sensorName уникальный идентификатор датчика
     * @param pressure текущее значение давления
     * @return true - обнаружена утечка, false - аномалий нет
     */
    public boolean analyzePressure(String sensorName, double pressure) {
        // Получаем или создаем статистику для датчика
        val stats = sensorStats.computeIfAbsent(sensorName,
                k -> SensorStats.builder()
                        .count(0)
                        .mean(pressure)
                        .variance(0)
                        .cusum(0)
                        .build()
        );

        // Калибровка: первые NUM_CALIBRATION_RECORDS измерений для накопления статистики
        if (stats.getCount() < NUM_CALIBRATION_RECORDS) {
            updateStats(stats, pressure);
            return false;
        }

        // Проверка на аномалию
        val isLeak = checkAnomaly(stats, pressure);

        // Обновляем статистику только при нормальных показаниях
        if (!isLeak) {
            updateStats(stats, pressure);
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

        stats.setMean(newMean);
        // Обновление дисперсии методом Welford
        stats.setVariance(stats.getVariance() + delta * newDelta);
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
        // Расчет стандартного отклонения
        val stdDev = Math.sqrt(stats.getVariance() / stats.getCount());
        // Z-скор: количество сигм от среднего
        val zScore = Math.abs((value - stats.getMean()) / stdDev);

        // CUSUM: кумулятивная сумма отклонений с "дрейфом" 0.5 сигмы
        val cusum = Math.max(0, stats.getCusum() + (value - stats.getMean()) / stdDev - 0.5);
        stats.setCusum(cusum);



        // Условие срабатывания: превышение любого из порогов
        return zScore > LEAK_THRESHOLD || cusum > CUSUM_THRESHOLD;
    }

    public SensorStats getSensorStats(String sensorId) {
        return sensorStats.get(sensorId);
    }

    /**
     * Для хранения статистики датчика.
     */
    @Data
    @Builder
    public static class SensorStats {
        /**
         * Текущее среднее значение давления
         */
        private double mean;
        /**
         * Накопленная дисперсия
         */
        private double variance;
        /**
         * Количество учтенных измерений
         */
        private int count;
        /**
         * Текущее значение кумулятивной суммы отклонений
         */
        private double cusum;
    }
}
