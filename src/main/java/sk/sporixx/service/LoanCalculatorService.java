package sk.sporixx.service;

public interface LoanCalculatorService {

    double calculateMonthlyPayment(double principal, double annualInterestRate, int months);

    double calculateTotalInterest(double principal, double annualInterestRate, int months);

    String generateRepaymentSummary(double principal, double annualInterestRate, int months);
}