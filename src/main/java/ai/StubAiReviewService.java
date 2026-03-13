package ai;

import model.Mission;

public class StubAiReviewService implements AiReviewService {

    @Override
    public String generateReview(Mission mission) {

        if (mission == null) {
            return "AI-обзор недоступен: данные миссии отсутствуют.";
        }

        return "AI-обзор пока недоступен.\n\n"
                + "Миссия: " + mission.getMissionId() + "\n"
                + "Локация: " + mission.getLocation() + "\n"
                + "Результат: " + mission.getOutcome() + "\n\n"
                + "Для получения полноценного анализа подключите GigaChat API.";
    }
}