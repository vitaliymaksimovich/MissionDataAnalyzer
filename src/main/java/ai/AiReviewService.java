package ai;

import model.Mission;

public interface AiReviewService {
    String generateReview(Mission mission);
    String generateBriefAnalysis(Mission mission);
    String generateDetailedAnalysis(Mission mission);
    String findProblems(Mission mission);
    String generateRecommendations(Mission mission);
    String generateStory(Mission mission);
}