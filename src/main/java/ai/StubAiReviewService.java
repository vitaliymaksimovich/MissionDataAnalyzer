package ai;

import model.Mission;

public class StubAiReviewService implements AiReviewService {

    @Override
    public String generateReview(Mission mission) {
        return stub(mission, "обзор");
    }

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

    private String stub(Mission mission, String type) {
        if (mission == null) return "AI недоступен: данные миссии отсутствуют.";
        return "AI (" + type + ") недоступен.\n"
                + "Миссия: " + mission.getMissionId() + "\n"
                + "Локация: " + mission.getLocation() + "\n"
                + "Результат: " + mission.getOutcome() + "\n\n"
                + "Для полного анализа подключите GigaChat API (переменная GIGACHAT_AUTH_KEY).";
    }
}