package gas.pipeline.safety.lot;


import gas.pipeline.safety.lot.config.ApplicationConfig;
import gas.pipeline.safety.lot.sensor.FakeSensor;
import gas.pipeline.safety.lot.sensor.Sensor;
import lombok.extern.java.Log;
import lombok.val;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static gas.pipeline.safety.lot.config.ApplicationConfig.*;

@Log
public class Main {
    private static URL url;

    private static Sensor sensor;

    static {
        boolean connected = false;

        for (int c = 0; !connected && c < CONNECT_MAX; c++) {
            try {
                url = new URI(ApplicationConfig.CONNECT_URL).toURL();
                log.info("Connected to: " + url);

                connected = true;

                if (ApplicationConfig.SENSOR_REAL) {
                    sensor = new Sensor();
                } else {
                    sensor = new FakeSensor();
                }
            } catch (URISyntaxException e) {
                System.err.println("Invalid URL: " + ApplicationConfig.CONNECT_URL);
                System.exit(-1);
            } catch (MalformedURLException e) {
                log.warning("Malformed URL: " + ApplicationConfig.CONNECT_URL);
                try {
                    Thread.sleep(CONNECT_DELAY);
                } catch (InterruptedException ex) {
                    log.warning("Interrupted while waiting for retry");
                }
            }
        }
        if (!connected) {
            System.err.println("Could not connect to " + ApplicationConfig.CONNECT_URL);
            System.exit(-1);
        }
    }

    public static void main(String[] args) {
        //noinspection InfiniteLoopStatement
        while (true) { // датчик все время отправляет данные пока работает
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setDoInput(true);

                val sensorIdEncoded = URLEncoder.encode(ApplicationConfig.SENSOR_ID, StandardCharsets.UTF_8);
                val pressureEncoded = URLEncoder.encode(String.valueOf(sensor.getPressure()), StandardCharsets.UTF_8);
                val postDate = String.format(
                        "{\"sensorId\": \"%s\", \"pressure\": \"%s\"}",
                        sensorIdEncoded, pressureEncoded
                );

                log.info("Request: " + postDate);

                val postDataBytes = postDate.getBytes(StandardCharsets.UTF_8);
                conn.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
                conn.setRequestProperty("Content-Type", "application/json");

                // на сервере проверка времени отправки
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                conn.setRequestProperty("X-Request-Timestamp", timestamp);

                try (val os = conn.getOutputStream()) {
                    os.write(postDataBytes);
                }

                val responseCode = conn.getResponseCode();
                log.info("Response Code: " + responseCode);
            } catch (IOException ex) {
                log.warning("I/O Exception: " + ex.getMessage());
            } finally {
                if (conn != null)
                    conn.disconnect();
            }

            try {
                Thread.sleep(SENDING_DELAY);
            } catch (InterruptedException e) {
                log.warning("Interrupted while waiting for retry");
            }
        }
    }
}