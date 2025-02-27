package gas.pipeline.safety.forecast.core.logger;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.EmbeddedValueResolver;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

@Slf4j
@Component
public class ValueAnnotationLogger implements BeanPostProcessor {
    private final ConfigurableListableBeanFactory beanFactory;


    public ValueAnnotationLogger(ConfigurableListableBeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    // перехват и логирование ошибки инициализации данных из конфиг файлов
    @Override
    public Object postProcessBeforeInitialization(Object bean, @NonNull String beanName) throws BeansException {
        val beanClass = bean.getClass();
        for (Field field : beanClass.getDeclaredFields()) {
            val valueAnnotation = AnnotationUtils.getAnnotation(field, Value.class);
            if (valueAnnotation != null) {
                String expression = valueAnnotation.value();
                if (expression.contains(":")) { // Check if default value is provided
                    String propertyName = expression.substring(2, expression.indexOf(":")); // Extract property name
                    String defaultValue = expression.substring(expression.indexOf(":") + 1, expression.length() - 1); // Extract default value
                    String resolvedValue = resolveExpression(expression);

                    if (resolvedValue == null || resolvedValue.equals(defaultValue)) {
                        log.warn("Property '{}' not found, using default value: {} for field '{}' in bean '{}'",
                                propertyName, defaultValue, field.getName(), beanName);
                    }
                }
            }
        }
        return bean;
    }

    private String resolveExpression(String expression) {
        val valueResolver = new EmbeddedValueResolver(beanFactory);
        return valueResolver.resolveStringValue(expression);
    }

    @Override
    public Object postProcessAfterInitialization(@NonNull Object bean, @NonNull  String beanName) throws BeansException {
        return bean;
    }
}
