/*
package gas.pipeline.safety.forecast.util;

import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static gas.pipeline.safety.forecast.config.constant.SensorStandartConst.SENSOR_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BayesianLeakModelTest {
    private BayesianLeakModel model;

    @BeforeEach
    void setUp() {
        this.model = new BayesianLeakModel();
    }

    // Проверяет, что вероятность утечки увеличивается при добавлении данных с меткой утечки
    @Test
    void shouldIncreaseProbabilityWhenLeakDetected() {
        model.update(SENSOR_ID, false, 150.0);
        model.update(SENSOR_ID, false, 150.0);
        model.update(SENSOR_ID, false, 150.0);
        model.update(SENSOR_ID, true, 100.0);
        model.update(SENSOR_ID, true, 100.0);
        val probAfterLeak = model.getLeakProbability(SENSOR_ID);

        assertTrue(probAfterLeak > 0.0, "Вероятность должна стать больше 0 после утечки");
    }

    // Проверяет, что вероятность утечки снижается при добавлении нормальных данных после утечек
    @Test
    void shouldDecreaseProbabilityWhenNoLeaks() {
        // Обучение модели на смешанных данных (утечки и нормальные значения)
        model.update(SENSOR_ID, false, 105.0);
        model.update(SENSOR_ID, false, 100.0);
        model.update(SENSOR_ID, true, 90.0);
        model.update(SENSOR_ID, true, 30.0);

        val initialProb = model.getLeakProbability(SENSOR_ID);
        assertTrue(initialProb > 0, "После утечек вероятность должна быть > 0");

        // Добавление нормальных данных
        model.update(SENSOR_ID, false, 102.0);
        model.update(SENSOR_ID, false, 98.0);

        val updatedProb = model.getLeakProbability(SENSOR_ID);
        assertTrue(updatedProb < initialProb,
                "Вероятность должна снизиться после нормальных данных. Было: " + initialProb + ", Стало: " + updatedProb);
    }

    // Проверяет, что аномальные значения без метки утечки повышают вероятность (из-за схожести с паттерном утечки)
    @Test
    void shouldIncreaseProbabilityForLeakPattern() {
        model.update(SENSOR_ID, true, 190.0);
        model.update(SENSOR_ID, true, 200.0);

        model.update(SENSOR_ID, false, 100.0);
        model.update(SENSOR_ID, false, 105.0);
        val initialProb = model.getLeakProbability(SENSOR_ID);

        // Аномальное значение
        model.update(SENSOR_ID, false, 200.0);
        val updatedProb = model.getLeakProbability(SENSOR_ID);

        assertTrue(updatedProb > initialProb,
                "Аномальное значение должно увеличить вероятность. Было: " + initialProb + ", Стало: " + updatedProb);
    }

    // Проверяет обработку первого измерения (вероятность должна быть 0, так как недостаточно данных)
    @Test
    void shouldHandleFirstReading() {
        model.update(SENSOR_ID, false, 100.0);
        assertEquals(0.0, model.getLeakProbability(SENSOR_ID), 0.001,
                "При первом измерении недостаточно данных для оценки вероятности");
    }

    // Проверяет корректность вычисления функции правдоподобия для нормального распределения
    @Test
    void shouldCalculateGaussianLikelihoodCorrectly() {
        val mean = 100.0;
        val variance = 25.0;
        val x = 105.0;

        val expected = (1 / (Math.sqrt(2 * Math.PI * variance)))
                * Math.exp(-Math.pow(x - mean, 2) / (2 * variance));

        val actual = model.calculateGaussianLikelihood(x, mean, variance);

        assertEquals(expected, actual, 1e-6, "Расчет likelihood не совпадает с ожидаемым");
    }
}*/
