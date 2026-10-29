package com.bank.lms.controller;

import com.bank.lms.entity.LoanApplication;
import com.bank.lms.entity.RepaymentSchedule;
import com.bank.lms.entity.User;
import com.bank.lms.repository.LoanApplicationRepository;
import com.bank.lms.service.LoanService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/loans")
@RequiredArgsConstructor
public class LoanController {

    private final LoanApplicationRepository loanRepository;
    private final LoanService loanService;

    // Apply/Assign a loan (Officer can assign to any customerId; Customer creates under own ID)
    @PostMapping("/apply")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'OFFICER')")
    public ResponseEntity<LoanApplication> applyLoan(
            @RequestBody LoanApplication loan,
            @AuthenticationPrincipal User user) {

        if ("ROLE_CUSTOMER".equals(user.getRole())) {
            loan.setCustomerId(user.getCustomerId());
        }
        loan.setStatus("PENDING");
        return new ResponseEntity<>(loanRepository.save(loan), HttpStatus.CREATED);
    }

    // Get all loans (Officer: all loans or filter by customerId; Customer: own loans)
    @GetMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'OFFICER')")
    public ResponseEntity<List<LoanApplication>> getLoans(
            @RequestParam(required = false) Long customerId,
            @AuthenticationPrincipal User user) {

        if ("ROLE_CUSTOMER".equals(user.getRole())) {
            return ResponseEntity.ok(loanRepository.findByCustomerId(user.getCustomerId()));
        }

        if (customerId != null) {
            return ResponseEntity.ok(loanRepository.findByCustomerId(customerId));
        }

        return ResponseEntity.ok(loanRepository.findAll());
    }

    @PostMapping("/{loanId}/disburse")
    @PreAuthorize("hasRole('OFFICER')")
    public ResponseEntity<List<RepaymentSchedule>> disburseLoan(@PathVariable Long loanId) {
        return ResponseEntity.ok(loanService.disburseAndGenerateSchedule(loanId));
    }

    @GetMapping("/{loanId}/schedule")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'OFFICER')")
    public ResponseEntity<List<RepaymentSchedule>> getSchedule(@PathVariable Long loanId) {
        return ResponseEntity.ok(loanService.getSchedule(loanId));
    }

    // Officer marks an installment as RECEIVED/PAID
    @PostMapping("/schedule/{scheduleId}/pay")
    @PreAuthorize("hasRole('OFFICER')")
    public ResponseEntity<RepaymentSchedule> receiveEmiPayment(@PathVariable Long scheduleId) {
        return ResponseEntity.ok(loanService.recordEmiPayment(scheduleId));
    }

    // Officer processes an advance principal prepayment
    @PostMapping("/{loanId}/advance-pay")
    @PreAuthorize("hasRole('OFFICER')")
    public ResponseEntity<List<RepaymentSchedule>> advancePayment(
            @PathVariable Long loanId,
            @RequestBody AdvancePaymentRequest req) {
        return ResponseEntity.ok(loanService.processAdvancePayment(loanId, req.getAmount()));
    }

    @DeleteMapping("/{loanId}")
    @PreAuthorize("hasRole('OFFICER')")
    public ResponseEntity<?> deleteLoan(@PathVariable Long loanId) {
        LoanApplication loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found: " + loanId));
        loanRepository.delete(loan);
        return ResponseEntity.ok().build();
    }

    @Data
    public static class AdvancePaymentRequest {
        private BigDecimal amount;
    }
}