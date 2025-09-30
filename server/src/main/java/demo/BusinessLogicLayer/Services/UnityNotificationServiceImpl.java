package demo.BusinessLogicLayer.Services;

import com.fasterxml.jackson.databind.ObjectMapper; // For JSON conversion
import demo.BusinessLogicLayer.Services.UnityNotificationService;
import org.springframework.stereotype.Service;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
// Consider adding logging framework e.g. import org.slf4j.Logger; import org.slf4j.LoggerFactory;

@Service
public class UnityNotificationServiceImpl implements UnityNotificationService {

    // private static final Logger logger = LoggerFactory.getLogger(UnityNotificationServiceImpl.class);
    private final String UNITY_SIMULATOR_URL = "http://localhost:8085"; // Adjust if Unity is not on localhost

    @Override
    public void sendLockerStatus(String lockerId, boolean isLocked) {
        try {
            URL url = new URL(UNITY_SIMULATOR_URL + "/lockerUpdate"); // Define a specific endpoint if desired
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000); // 5 seconds
            conn.setReadTimeout(5000);    // 5 seconds

            Map<String, Object> payload = new HashMap<>();
            payload.put("lockerId", lockerId);
            payload.put("isLocked", isLocked);

            ObjectMapper objectMapper = new ObjectMapper();
            String jsonInputString = objectMapper.writeValueAsString(payload);

            try(OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInputString.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            // logger.info("Unity notification sent for lockerId: {}. Response Code: {}", lockerId, responseCode);
            // Handle response (e.g., check for 2xx success codes) if necessary

            conn.disconnect();

        } catch (Exception e) {
            // logger.error("Error sending notification to Unity for lockerId: {}: {}", lockerId, e.getMessage(), e);
            // Consider a retry mechanism or dead-letter queue for critical notifications
            e.printStackTrace(); // Replace with robust error handling
        }
    }
}