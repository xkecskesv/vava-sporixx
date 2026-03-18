package sk.sporixx.service;

public interface StatisticsService {

    String getMonthlySummary(Long userId, String month);

    String getCategoryBreakdown(Long userId, String month);

    String getTrendSummary(Long userId);
}