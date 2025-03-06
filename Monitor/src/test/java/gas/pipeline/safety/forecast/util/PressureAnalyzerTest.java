/*
package gas.pipeline.safety.forecast.util;

import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static gas.pipeline.safety.forecast.config.constant.SensorStandartConst.OTHER_SENSOR_ID;
import static gas.pipeline.safety.forecast.config.constant.SensorStandartConst.SENSOR_ID;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.within;


class PressureAnalyzerTest {
    private PressureAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new PressureAnalyzer();
    }

    // Проверяет, что первые 10 измерений используются для калибровки и не вызывают утечек
    @Test
    void testCalibrationPhase() {
        for (int i = 0; i < 9; i++) {
            assertThat(analyzer.analyzePressure(SENSOR_ID, 10.0)).isFalse();
        }
        assertThat(analyzer.analyzePressure(SENSOR_ID, 10.0)).isFalse();
    }

    // Проверяет срабатывание утечки при превышении порога Z-скор (аномалия в одном измерении)
    @Test
    void testZScoreExceeded() {
        completeCalibration(100.0);
        assertThat(analyzer.analyzePressure(SENSOR_ID, 12.0)).isTrue();
    }

    // Проверяет срабатывание утечки при накоплении отклонений (CUSUM)
    @Test
    void testCusumExceeded() {
        completeCalibration(10.0);

        analyzer.analyzePressure(SENSOR_ID, 11.0); // +1 сигма
        analyzer.analyzePressure(SENSOR_ID, 11.0);
        analyzer.analyzePressure(SENSOR_ID, 11.0);

        assertThat(analyzer.analyzePressure(SENSOR_ID, 11.0)).isTrue();
    }

    // Проверяет, что статистика не обновляется при обнаружении утечки
    @Test
    void testStatsNotUpdatedWhenLeakDetected() {
        completeCalibration(100.0);
        val initialCount = analyzer.getSensorStats(SENSOR_ID).getCount();

        // Аномальное значение
        analyzer.analyzePressure(SENSOR_ID, 12.0);

        assertThat(analyzer.getSensorStats(SENSOR_ID).getCount()).isEqualTo(initialCount);
    }

    // Проверяет корректность вычисления суммы квадратов отклонений (variance)
    @Test
    void testVarianceCalculation() {
        // Данные с известной суммой квадратов отклонений
        for (int i = 0; i < 10; i++) {
            analyzer.analyzePressure(SENSOR_ID, i + 1.0);
        }

        PressureAnalyzer.SensorStats stats = analyzer.getSensorStats(SENSOR_ID);
        val expectedMean = 5.5;
        val expectedSumSquaredDiffs = 82.5;

        assertThat(stats.getMean()).isEqualTo(expectedMean);
        assertThat(stats.getVariance()).isCloseTo(expectedSumSquaredDiffs, within(0.0001));
    }

    // Проверяет независимость статистики для разных датчиков
    @Test
    void testMultipleSensors() {
        completeCalibration(SENSOR_ID, 10.0);
        completeCalibration(OTHER_SENSOR_ID, 20.0);

        PressureAnalyzer.SensorStats stats1 = analyzer.getSensorStats(SENSOR_ID);
        PressureAnalyzer.SensorStats stats2 = analyzer.getSensorStats(OTHER_SENSOR_ID);

        assertThat(stats1.getMean()).isEqualTo(10.0);
        assertThat(stats2.getMean()).isEqualTo(20.0);
    }

    // Проверяет обработку случая с нулевой дисперсией (все измерения одинаковы)
    @Test
    void testZeroVariance() {
        completeCalibration(10.0);

        // При stdDev = 0 любое отклонение - бесконечный Z-score
        assertThat(analyzer.analyzePressure(SENSOR_ID, 11.0)).isTrue();
    }

    private void completeCalibration(String sensorId, double value) {
        for (int i = 0; i < 10; i++) {
            analyzer.analyzePressure(sensorId, value);
        }
    }

    private void completeCalibration(double value) {
        completeCalibration(SENSOR_ID, value);
    }
}*/
