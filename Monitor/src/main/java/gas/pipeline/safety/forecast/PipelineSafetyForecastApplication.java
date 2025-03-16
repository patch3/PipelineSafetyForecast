package gas.pipeline.safety.forecast;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class PipelineSafetyForecastApplication {
    public static void main(String[] args) {
        SpringApplication.run(PipelineSafetyForecastApplication.class, args);
    }
}
