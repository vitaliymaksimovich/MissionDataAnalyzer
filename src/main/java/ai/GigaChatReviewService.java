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

    // --- публичные методы интерфейса ---

    @Override
    public String generateReview(Mission mission) {
        return callWithPrompt(mission,
                "Дай короткий понятный обзор миссии на русском языке в 3-5 предложениях.");
    }

    @Override
    public String generateBriefAnalysis(Mission mission) {
        return callWithPrompt(mission,
                "Дай краткий анализ миссии в 3-4 предложениях. " +
                        "Оцени угрозу, действия магов и итог.");
    }

    @Override
    public String generateDetailedAnalysis(Mission mission) {
        return callWithPrompt(mission,
                "Дай подробный анализ миссии. Разбери каждый этап: " +
                        "угрозу, действия участников, применённые техники, итог и последствия.");
    }

    @Override
    public String findProblems(Mission mission) {
        return callWithPrompt(mission,
                "Найди проблемы и слабые места в проведении миссии. " +
                        "Что пошло не так? Какие ошибки допустили маги?");
    }

    @Override
    public String generateRecommendations(Mission mission) {
        return callWithPrompt(mission,
                "Дай конкретные рекомендации для улучшения результатов подобных миссий. " +
                        "Что стоит изменить в тактике, составе команды, подготовке?");
    }

    @Override
    public String generateStory(Mission mission) {
        return callWithPrompt(mission,
                "Напиши короткую художественную историю (5-7 предложений) " +
                        "от лица участника этой миссии. Атмосферно, в стиле аниме.");
    }

    // --- общий метод вызова API ---

    private String callWithPrompt(Mission mission, String instruction) {
        if (mission == null) return "AI недоступен: данные миссии отсутствуют.";
        if (authorizationKey == null || authorizationKey.isBlank())
            return "AI недоступен: не задана переменная GIGACHAT_AUTH_KEY.";
        try {
            String accessToken = getAccessToken();
            String prompt = instruction + "\n\n" + buildPrompt(mission);
            return requestReview(accessToken, prompt);
        } catch (Exception e) {
            return "AI недоступен: " + e.getMessage();
        }
    }

    // --- приватные методы (HTTP, промпт) ---

    private HttpClient createSecureHttpClient() {
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            try (InputStream is = getClass().getResourceAsStream(CERTIFICATE_RESOURCE_PATH)) {
                if (is == null)
                    throw new IllegalStateException("Сертификат не найден: " + CERTIFICATE_RESOURCE_PATH);
                Certificate cert = cf.generateCertificate(is);
                KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
                ks.load(null, null);
                ks.setCertificateEntry("gigachat-root", cert);
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tmf.init(ks);
                SSLContext ssl = SSLContext.getInstance("TLS");
                ssl.init(null, tmf.getTrustManagers(), new SecureRandom());
                return HttpClient.newBuilder().sslContext(ssl).build();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Не удалось создать HTTP-клиент: " + e.getMessage(), e);
        }
    }

    private String getAccessToken() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(TOKEN_URL))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .header("RqUID", UUID.randomUUID().toString())
                .header("Authorization", "Basic " + authorizationKey)
                .POST(HttpRequest.BodyPublishers.ofString("scope=" + scope, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200)
            throw new IllegalStateException("Не удалось получить токен (HTTP " + response.statusCode() + ")");

        JsonNode tokenNode = objectMapper.readTree(response.body()).get("access_token");
        if (tokenNode == null || tokenNode.asText().isBlank())
            throw new IllegalStateException("access_token отсутствует в ответе");

        return tokenNode.asText();
    }

    private String requestReview(String accessToken, String prompt) throws IOException, InterruptedException {
        JsonNode body = objectMapper.createObjectNode()
                .put("model", model)
                .put("temperature", 0.3)
                .put("max_tokens", 400)
                .set("messages", objectMapper.createArrayNode()
                        .add(objectMapper.createObjectNode()
                                .put("role", "system")
                                .put("content", "Ты аналитик штаба магов. Отвечай на русском языке."))
                        .add(objectMapper.createObjectNode()
                                .put("role", "user")
                                .put("content", prompt)));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(CHAT_URL))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + accessToken)
                .POST(HttpRequest.BodyPublishers.ofString(
                        objectMapper.writeValueAsString(body), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200)
            throw new IllegalStateException("Ошибка GigaChat (HTTP " + response.statusCode() + ")");

        JsonNode content = objectMapper.readTree(response.body())
                .path("choices").path(0).path("message").path("content");
        if (content.isMissingNode() || content.asText().isBlank())
            throw new IllegalStateException("Пустой ответ от GigaChat");

        return content.asText();
    }

    private String buildPrompt(Mission mission) {
        StringBuilder sb = new StringBuilder("Данные миссии:\n");
        sb.append("ID: ").append(safe(mission.getMissionId())).append("\n");
        sb.append("Дата: ").append(safe(mission.getDate())).append("\n");
        sb.append("Локация: ").append(safe(mission.getLocation())).append("\n");
        sb.append("Результат: ").append(safe(mission.getOutcome())).append("\n");
        sb.append("Ущерб: ").append(mission.getDamageCost()).append("\n");
        if (mission.getCurse() != null) {
            sb.append("Проклятие: ").append(safe(mission.getCurse().getName())).append("\n");
            sb.append("Уровень угрозы: ").append(safe(mission.getCurse().getThreatLevel())).append("\n");
        }
        if (mission.getSorcerers() != null && !mission.getSorcerers().isEmpty()) {
            sb.append("Участники: ");
            mission.getSorcerers().forEach(s -> sb.append(s.getName()).append(" (").append(s.getRank()).append(") "));
            sb.append("\n");
        }
        if (mission.getComment() != null && !mission.getComment().isBlank())
            sb.append("Комментарий: ").append(mission.getComment()).append("\n");
        return sb.toString();
    }

    private String readEnvOrDefault(String name, String def) {
        String v = System.getenv(name);
        return (v == null || v.isBlank()) ? def : v;
    }

    private String safe(String v) {
        return (v == null || v.isBlank()) ? "N/A" : v;
    }
}