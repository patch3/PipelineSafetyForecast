package gas.pipeline.safety.forecast.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import gas.pipeline.safety.forecast.repository.SensorRepository;
import gas.pipeline.safety.forecast.service.models.LeakPredictionsService;
import lombok.val;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.HashMap;
import java.util.List;

@Controller
public class PredictionController {
    private final SensorRepository sensorRepository;
    private final LeakPredictionsService leakPredictionsService;
    private final ObjectMapper objectMapper;

    public PredictionController(SensorRepository sensorRepository,
                                LeakPredictionsService leakPredictionsService,
                                ObjectMapper objectMapper) {
        this.sensorRepository = sensorRepository;
        this.leakPredictionsService = leakPredictionsService;
        this.objectMapper = objectMapper;
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @GetMapping("/predictions")
    public String getPredictions(
            String sensorName,
            Model model
    ) throws JsonProcessingException {
        val sensorsPredictions = new HashMap<String, List<LeakPredictionsService.LeakPrediction>>();
        if (sensorName == null) {
            val sensors = sensorRepository.findAllSensorNames();
            sensors.forEach(sensor ->
                    sensorsPredictions.put(sensor, leakPredictionsService.generatePredictionsForPeriod(sensor))
            );
        } else {
            sensorsPredictions.put(sensorName, leakPredictionsService.generatePredictionsForPeriod(sensorName));
        }
        model.addAttribute("predictions", objectMapper.writeValueAsString(sensorsPredictions));
        return "predictions";
    }
}
