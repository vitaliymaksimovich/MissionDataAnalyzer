package ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import model.Mission;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.UUID;

public class GigaChatReviewService implements AiReviewService {

    private static final String TOKEN_URL = "https://ngw.devices.sberbank.ru:9443/api/v2/oauth";
    private static final String CHAT_URL = "https://gigachat.devices.sberbank.ru/api/v1/chat/completions";
    private static final String CERTIFICATE_RESOURCE_PATH = "/certs/russian_trusted_root_ca_pem.crt";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    private final String authorizationKey;
    private final String scope;
    private final String model;

    public GigaChatReviewService() {
        this.objectMapper = new ObjectMapper();
        this.httpClient = createSecureHttpClient();

        this.authorizationKey = System.getenv("GIGACHAT_AUTH_KEY");
        this.scope = readEnvOrDefault("GIGACHAT_SCOPE", "GIGACHAT_API_PERS");
        this.model = readEnvOrDefault("GIGACHAT_MODEL", "GigaChat");
    }

    @Override
    public String generateReview(Mission mission) {
        if (mission == null) {
            return "AI-обзор недоступен: данные миссии отсутствуют.";
        }

        if (authorizationKey == null || authorizationKey.isBlank()) {
            return "AI-обзор недоступен: не задана переменная окружения GIGACHAT_AUTH_KEY.";
        }

        try {
            String accessToken = getAccessToken();
            String prompt = buildPrompt(mission);
            return requestReview(accessToken, prompt);
        } catch (Exception e) {
            return "AI-обзор недоступен: " + e.getMessage();
        }
    }

    private HttpClient createSecureHttpClient() {
        try {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");

            try (InputStream certInputStream = getClass().getResourceAsStream(CERTIFICATE_RESOURCE_PATH)) {
                if (certInputStream == null) {
                    throw new IllegalStateException("Файл сертификата не найден: " + CERTIFICATE_RESOURCE_PATH);
                }

                Certificate certificate = certificateFactory.generateCertificate(certInputStream);

                KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                keyStore.load(null, null);
                keyStore.setCertificateEntry("gigachat-root", certificate);

                TrustManagerFactory trustManagerFactory =
                        TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                trustManagerFactory.init(keyStore);

                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, trustManagerFactory.getTrustManagers(), new SecureRandom());

                return HttpClient.newBuilder()
                        .sslContext(sslContext)
                        .build();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Не удалось создать защищённый HTTP-клиент: " + e.getMessage(), e);
        }
    }

    private String getAccessToken() throws IOException, InterruptedException {
        String requestBody = "scope=" + scope;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(TOKEN_URL))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .header("RqUID", UUID.randomUUID().toString())
                .header("Authorization", "Basic " + authorizationKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IllegalStateException("не удалось получить access token (HTTP " + response.statusCode() + ")");
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode tokenNode = root.get("access_token");

        if (tokenNode == null || tokenNode.asText().isBlank()) {
            throw new IllegalStateException("access token отсутствует в ответе GigaChat");
        }

        return tokenNode.asText();
    }

    private String requestReview(String accessToken, String prompt) throws IOException, InterruptedException {
        JsonNode requestBody = objectMapper.createObjectNode()
                .put("model", model)
                .put("temperature", 0.3)
                .put("max_tokens", 200)
                .set("messages", objectMapper.createArrayNode()
                        .add(objectMapper.createObjectNode()
                                .put("role", "system")
                                .put("content", "Ты помощник, который кратко анализирует миссии. Дай короткий, понятный обзор миссии на русском языке в 3-5 предложениях."))
                        .add(objectMapper.createObjectNode()
                                .put("role", "user")
                                .put("content", prompt)));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(CHAT_URL))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + accessToken)
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IllegalStateException("ошибка запроса к GigaChat (HTTP " + response.statusCode() + ")");
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode contentNode = root.path("choices").path(0).path("message").path("content");

        if (contentNode.isMissingNode() || contentNode.asText().isBlank()) {
            throw new IllegalStateException("пустой ответ от GigaChat");
        }

        return contentNode.asText();
    }

    private String buildPrompt(Mission mission) {
        StringBuilder sb = new StringBuilder();

        sb.append("Ты аналитик штаба магов.\n");
        sb.append("Проанализируй миссию и напиши краткий аналитический обзор.\n");
        sb.append("Ответ должен содержать оценку угрозы, действий магов и итог миссии с мотивацией на будущее. 4-5 предложений.\n\n");

        sb.append("Данные миссии:\n");
        sb.append("ID миссии: ").append(safe(mission.getMissionId())).append("\n");
        sb.append("Дата: ").append(safe(mission.getDate())).append("\n");
        sb.append("Локация: ").append(safe(mission.getLocation())).append("\n");
        sb.append("Результат: ").append(safe(mission.getOutcome())).append("\n");
        sb.append("Стоимость ущерба: ").append(mission.getDamageCost()).append("\n");

        if (mission.getCurse() != null) {
            sb.append("Проклятие: ").append(safe(mission.getCurse().getName())).append("\n");
            sb.append("Уровень угрозы: ").append(safe(mission.getCurse().getThreatLevel())).append("\n");
        }

        if (mission.getComment() != null && !mission.getComment().isBlank()) {
            sb.append("Комментарий: ").append(mission.getComment()).append("\n");
        }

        return sb.toString();
    }

    private String readEnvOrDefault(String envName, String defaultValue) {
        String value = System.getenv(envName);
        return (value == null || value.isBlank()) ? defaultValue : value;
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "N/A" : value;
    }
}