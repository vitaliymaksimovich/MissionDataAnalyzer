package ai;

import model.Mission;

import java.util.List;

public class StubAiReviewService implements AiReviewService {

    @Override
    public String generateBriefAnalysis(Mission mission) {
        return stub(mission, "краткий анализ");
    }

    @Override
    public String generateDetailedAnalysis(Mission mission) {
        return stub(mission, "подробный анализ");
    }

    @Override
    public String findProblems(Mission mission) {
        return stub(mission, "поиск проблем");
    }

    @Override
    public String generateRecommendations(Mission mission) {
        return stub(mission, "рекомендации");
    }

    @Override
    public String generateStory(Mission mission) {
        return stub(mission, "история");
    }

    @Override
    public String customQuestion(Mission mission, String question) {
        return stub(mission, "свой вопрос: " + question);
    }

    @Override
    public String analyzeBatch(List<Mission> missions) {
        if (missions == null || missions.isEmpty())
            return "Нет загруженных миссий для анализа.";
        return "AI (общий анализ) недоступен.\n"
                + "Загружено миссий: " + missions.size() + "\n\n"
                + "Для полного анализа подключите GigaChat API (переменная GIGACHAT_AUTH_KEY).";
    }

    private String stub(Mission mission, String type) {
        if (mission == null) return "AI недоступен: данные миссии отсутствуют.";
        return "AI (" + type + ") недоступен.\n"
                + "Миссия: " + mission.getMissionId() + "\n"
                + "Локация: " + mission.getLocation() + "\n"
                + "Результат: " + mission.getOutcome() + "\n\n"
                + "Для полного анализа подключите GigaChat API (переменная GIGACHAT_AUTH_KEY).";
    }
}
