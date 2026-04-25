package ai;

import model.Mission;

import java.util.List;

public interface AiReviewService {
    String generateBriefAnalysis(Mission mission);
    String generateDetailedAnalysis(Mission mission);
    String findProblems(Mission mission);
    String generateRecommendations(Mission mission);
    String generateStory(Mission mission);
    String customQuestion(Mission mission, String question);
    String analyzeBatch(List<Mission> missions);
}
