package com.bank.lms.service;

import com.bank.lms.entity.LoanApplication;
import com.bank.lms.entity.RepaymentSchedule;
import com.bank.lms.repository.LoanApplicationRepository;
import com.bank.lms.repository.RepaymentScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LoanService {

    private final LoanApplicationRepository loanRepository;
    private final RepaymentScheduleRepository scheduleRepository;

    @Transactional
    public List<RepaymentSchedule> disburseAndGenerateSchedule(Long loanId) {
        LoanApplication loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found with ID: " + loanId));

        loan.setStatus("DISBURSED");
        loanRepository.save(loan);

        double principal = loan.getPrincipalAmount().doubleValue();
        double monthlyRate = (loan.getAnnualInterestRate().doubleValue() / 12.0) / 100.0;
        int tenure = loan.getTenureMonths();

        double emi = (principal * monthlyRate * Math.pow(1 + monthlyRate, tenure))
                / (Math.pow(1 + monthlyRate, tenure) - 1);
        BigDecimal emiAmount = BigDecimal.valueOf(emi).setScale(2, RoundingMode.HALF_UP);

        List<RepaymentSchedule> scheduleList = new ArrayList<>();
        BigDecimal remainingBalance = loan.getPrincipalAmount();

        for (int i = 1; i <= tenure; i++) {
            BigDecimal interestPart = remainingBalance.multiply(BigDecimal.valueOf(monthlyRate))
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal principalPart = emiAmount.subtract(interestPart)
                    .setScale(2, RoundingMode.HALF_UP);
            remainingBalance = remainingBalance.subtract(principalPart)
                    .setScale(2, RoundingMode.HALF_UP);

            RepaymentSchedule item = RepaymentSchedule.builder()
                    .loanId(loanId)
                    .installmentNo(i)
                    .dueDate(LocalDate.now().plusMonths(i))
                    .emiAmount(emiAmount)
                    .interestComponent(interestPart)
                    .principalComponent(principalPart)
                    .outstandingBalance(remainingBalance.max(BigDecimal.ZERO))
                    .status("UNPAID")
                    .build();

            scheduleList.add(item);
        }

        return scheduleRepository.saveAll(scheduleList);
    }

    public List<RepaymentSchedule> getSchedule(Long loanId) {
        return scheduleRepository.findByLoanIdOrderByInstallmentNoAsc(loanId);
    }

    // Mark an individual EMI installment as PAID
    @Transactional
    public RepaymentSchedule recordEmiPayment(Long scheduleId) {
        RepaymentSchedule installment = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("Schedule record not found: " + scheduleId));

        installment.setStatus("PAID");
        installment.setPaidAt(LocalDateTime.now());
        installment.setPaymentReference("TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        RepaymentSchedule saved = scheduleRepository.save(installment);

        // Check if all installments for this loan are now paid
        List<RepaymentSchedule> remainingUnpaid = scheduleRepository.findByLoanIdOrderByInstallmentNoAsc(installment.getLoanId())
                .stream()
                .filter(s -> "UNPAID".equals(s.getStatus()))
                .toList();

        if (remainingUnpaid.isEmpty()) {
            LoanApplication loan = loanRepository.findById(installment.getLoanId()).orElse(null);
            if (loan != null) {
                loan.setStatus("CLOSED");
                loanRepository.save(loan);
            }
        }

        return saved;
    }

    // Process Advance Principal Prepayment & Foreclosure
    @Transactional
    public List<RepaymentSchedule> processAdvancePayment(Long loanId, BigDecimal advanceAmount) {
        LoanApplication loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found with ID: " + loanId));

        List<RepaymentSchedule> schedule = scheduleRepository.findByLoanIdOrderByInstallmentNoAsc(loanId);

        // 1. Find all unpaid installments
        List<RepaymentSchedule> unpaidInstallments = schedule.stream()
                .filter(s -> "UNPAID".equals(s.getStatus()))
                .toList();

        if (unpaidInstallments.isEmpty()) {
            loan.setStatus("CLOSED");
            loanRepository.save(loan);
            return schedule;
        }

        // 2. Identify the true current principal balance prior to the first unpaid EMI
        int firstUnpaidIndex = schedule.indexOf(unpaidInstallments.get(0));
        BigDecimal currentPrincipalBalance = (firstUnpaidIndex == 0)
                ? loan.getPrincipalAmount()
                : schedule.get(firstUnpaidIndex - 1).getOutstandingBalance();

        // 3. FULL FORECLOSURE / PREPAYMENT:
        // If advance payment is >= remaining principal balance
        if (advanceAmount.compareTo(currentPrincipalBalance) >= 0) {
            scheduleRepository.deleteAll(unpaidInstallments);

            loan.setStatus("CLOSED");
            loanRepository.save(loan);

            return scheduleRepository.findByLoanIdOrderByInstallmentNoAsc(loanId);
        }

        // 4. PARTIAL PREPAYMENT:
        // Reduce principal balance and delete unneeded future installments
        BigDecimal remainingBalance = currentPrincipalBalance.subtract(advanceAmount);
        List<RepaymentSchedule> toUpdate = new ArrayList<>();
        List<RepaymentSchedule> toDelete = new ArrayList<>();

        for (RepaymentSchedule item : unpaidInstallments) {
            if (remainingBalance.compareTo(BigDecimal.ZERO) <= 0) {
                toDelete.add(item);
            } else if (remainingBalance.compareTo(item.getPrincipalComponent()) <= 0) {
                item.setPrincipalComponent(remainingBalance);
                item.setOutstandingBalance(BigDecimal.ZERO);
                item.setEmiAmount(remainingBalance.add(item.getInterestComponent()).setScale(2, RoundingMode.HALF_UP));
                remainingBalance = BigDecimal.ZERO;
                toUpdate.add(item);
            } else {
                remainingBalance = remainingBalance.subtract(item.getPrincipalComponent());
                item.setOutstandingBalance(remainingBalance.setScale(2, RoundingMode.HALF_UP));
                toUpdate.add(item);
            }
        }

        if (!toDelete.isEmpty()) {
            scheduleRepository.deleteAll(toDelete);
        }
        if (!toUpdate.isEmpty()) {
            scheduleRepository.saveAll(toUpdate);
        }

        return scheduleRepository.findByLoanIdOrderByInstallmentNoAsc(loanId);
    }
}