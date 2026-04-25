package ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import model.Mission;
import model.Sorcerer;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class GigaChatReviewService implements AiReviewService {

    private static final String TOKEN_URL = "https://ngw.devices.sberbank.ru:9443/api/v2/oauth";
    private static final String CHAT_URL = "https://gigachat.devices.sberbank.ru/api/v1/chat/completions";
    private static final String CERTIFICATE_RESOURCE_PATH = "/certs/russian_trusted_root_ca_pem.crt";

    // Таймауты HTTP — чтобы запрос не висел бесконечно при проблемах с сетью
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(60);

    // Единый системный промпт — задаёт стиль всех ответов
    private static final String SYSTEM_PROMPT = """
            Ты — аналитик штаба магов из вселенной Jujutsu Kaisen. \
            Отвечай строго на русском языке. \
            Будь лаконичен: 3-6 предложений, без воды и повторов. \
            Используй конкретные данные из миссии (имена, числа, факты). \
            Не додумывай то, чего нет в данных. \
            Если результат миссии FAILURE — не приукрашивай, констатируй провал. \
            Отвечай только по теме запроса, не отвлекайся на посторонние темы.""";

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

    // --- методы интерфейса ---

    @Override
    public String generateBriefAnalysis(Mission mission) {
        return callSingle(mission, 500,
                "Дай краткий анализ миссии: угроза, действия участников, итог.");
    }

    @Override
    public String generateDetailedAnalysis(Mission mission) {
        return callSingle(mission, 700,
                "Дай подробный анализ миссии по пунктам: " +
                        "1) Угроза 2) Действия команды 3) Итог и последствия.");
    }

    @Override
    public String findProblems(Mission mission) {
        return callSingle(mission, 600,
                "Найди слабые места и ошибки в проведении миссии. Что можно было сделать лучше?");
    }

    @Override
    public String generateRecommendations(Mission mission) {
        return callSingle(mission, 600,
                "Дай 3-5 конкретных рекомендаций по улучшению тактики для подобных миссий.");
    }

    @Override
    public String generateStory(Mission mission) {
        String outcome = mission.getOutcome() != null ? mission.getOutcome() : "UNKNOWN";
        String moodHint = switch (outcome) {
            case "FAILURE" -> " История должна закончиться поражением и горечью.";
            case "PARTIAL_SUCCESS" -> " Финал неоднозначный — победа далась дорогой ценой.";
            default -> "";
        };
        return callSingle(mission, 600,
                "Напиши короткую атмосферную историю (5-7 предложений) от лица одного из участников миссии. " +
                        "Стиль — аниме/Jujutsu Kaisen." + moodHint);
    }

    @Override
    public String customQuestion(Mission mission, String question) {
        return callSingle(mission, 600,
                "Ответь на вопрос пользователя по данным этой миссии: " + question);
    }

    @Override
    public String analyzeBatch(List<Mission> missions) {
        if (missions == null || missions.isEmpty())
            return "Нет загруженных миссий для анализа.";
        if (authorizationKey == null || authorizationKey.isBlank())
            return "AI недоступен: не задана переменная GIGACHAT_AUTH_KEY.";
        try {
            String accessToken = getAccessToken();
            String prompt = "Дай общий анализ по всем загруженным миссиям: " +
                    "тенденции, сильные и слабые стороны команды, главные выводы.\n\n" +
                    buildBatchPrompt(missions);
            return requestReview(accessToken, prompt, 700);
        } catch (Exception e) {
            return "AI недоступен: " + e.getMessage();
        }
    }

    // --- вызов API ---

    private String callSingle(Mission mission, int maxTokens, String instruction) {
        if (mission == null) return "AI недоступен: данные миссии отсутствуют.";
        if (authorizationKey == null || authorizationKey.isBlank())
            return "AI недоступен: не задана переменная GIGACHAT_AUTH_KEY.";
        try {
            String accessToken = getAccessToken();
            String prompt = instruction + "\n\n" + buildPrompt(mission);
            return requestReview(accessToken, prompt, maxTokens);
        } catch (Exception e) {
            return "AI недоступен: " + e.getMessage();
        }
    }

    // --- HTTP ---

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
                return HttpClient.newBuilder()
                        .sslContext(ssl)
                        .connectTimeout(CONNECT_TIMEOUT)
                        .build();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Не удалось создать HTTP-клиент: " + e.getMessage(), e);
        }
    }

    private String getAccessToken() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(TOKEN_URL))
                .timeout(REQUEST_TIMEOUT)
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

    private String requestReview(String accessToken, String prompt, int maxTokens)
            throws IOException, InterruptedException {
        JsonNode body = objectMapper.createObjectNode()
                .put("model", model)
                .put("temperature", 0.3)
                .put("max_tokens", maxTokens)
                .set("messages", objectMapper.createArrayNode()
                        .add(objectMapper.createObjectNode()
                                .put("role", "system")
                                .put("content", SYSTEM_PROMPT))
                        .add(objectMapper.createObjectNode()
                                .put("role", "user")
                                .put("content", prompt)));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(CHAT_URL))
                .timeout(REQUEST_TIMEOUT)
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

    // --- построение промптов ---

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
            mission.getSorcerers().forEach(s ->
                    sb.append(safe(s.getName())).append(" (").append(safe(s.getRank())).append(") "));
            sb.append("\n");
        }

        if (mission.getTechniques() != null && !mission.getTechniques().isEmpty()) {
            sb.append("Техники: ");
            mission.getTechniques().forEach(t ->
                    sb.append(safe(t.getName())).append(" "));
            sb.append("\n");
        }

        if (mission.getCivilianImpact() != null) {
            sb.append("Гражданские — эвакуировано: ")
                    .append(mission.getCivilianImpact().getEvacuated())
                    .append(", пострадавших: ")
                    .append(mission.getCivilianImpact().getInjured()).append("\n");
        }

        if (mission.getEconomicAssessment() != null) {
            var ea = mission.getEconomicAssessment();
            sb.append("Экономика — общий ущерб: ").append(ea.getTotalDamageCost())
                    .append(", инфраструктура: ").append(ea.getInfrastructureDamage())
                    .append(", восстановление: ").append(ea.getRecoveryEstimateDays()).append(" дней\n");
        }

        if (mission.getOperationTags() != null && !mission.getOperationTags().isEmpty()) {
            sb.append("Теги: ").append(String.join(", ", mission.getOperationTags())).append("\n");
        }

        if (mission.getComment() != null && !mission.getComment().isBlank())
            sb.append("Комментарий: ").append(mission.getComment()).append("\n");

        return sb.toString();
    }

    private String buildBatchPrompt(List<Mission> missions) {
        StringBuilder sb = new StringBuilder("Загружено миссий: " + missions.size() + "\n\n");
        for (int i = 0; i < missions.size(); i++) {
            Mission m = missions.get(i);
            sb.append("--- Миссия ").append(i + 1).append(" ---\n");
            sb.append("ID: ").append(safe(m.getMissionId()));
            sb.append(", Результат: ").append(safe(m.getOutcome()));
            sb.append(", Ущерб: ").append(m.getDamageCost());
            if (m.getCurse() != null)
                sb.append(", Угроза: ").append(safe(m.getCurse().getThreatLevel()));
            if (m.getSorcerers() != null && !m.getSorcerers().isEmpty())
                sb.append(", Участники: ").append(
                        m.getSorcerers().stream().map(Sorcerer::getName).collect(Collectors.joining(", ")));
            sb.append("\n");
        }
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
