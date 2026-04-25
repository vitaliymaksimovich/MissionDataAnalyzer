package ai;

/**
 * Паттерн: Factory Method (статическая фабрика).
 *
 * Создаёт нужную реализацию AiReviewService, скрывая от клиента логику выбора.
 * Клиент (MainFrame) не знает, с каким AI работает — только вызывает интерфейс.
 *
 * Текущая логика выбора:
 *  - Есть переменная окружения GIGACHAT_AUTH_KEY → GigaChatReviewService (реальный AI)
 *  - Иначе → StubAiReviewService (заглушка, не требует сети)
 *
 * Добавление нового провайдера (OpenAI, Yandex GPT и т.д.) = новый else-if здесь.
 * Остальной код не меняется.
 */
public class AiServiceFactory {

    private AiServiceFactory() {}

    public static AiReviewService create() {
        String authorizationKey = System.getenv("GIGACHAT_AUTH_KEY");

        // Фабрика выбирает реализацию по условию окружения
        if (authorizationKey != null && !authorizationKey.isBlank()) {
            return new GigaChatReviewService();
        }

        return new StubAiReviewService();
    }
}