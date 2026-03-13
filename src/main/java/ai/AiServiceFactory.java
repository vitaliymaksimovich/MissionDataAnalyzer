package ai;

public class AiServiceFactory {

    private AiServiceFactory() {
    }

    public static AiReviewService create() {
        String authorizationKey = System.getenv("GIGACHAT_AUTH_KEY");

        if (authorizationKey != null && !authorizationKey.isBlank()) {
            return new GigaChatReviewService();
        }

        return new StubAiReviewService();
    }
}