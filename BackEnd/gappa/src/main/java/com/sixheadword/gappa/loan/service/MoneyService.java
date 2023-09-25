package com.sixheadword.gappa.loan.service;

import com.sixheadword.gappa.account.Account;
import com.sixheadword.gappa.account.repository.AccountRepository;
import com.sixheadword.gappa.loan.Loan;
import com.sixheadword.gappa.loan.dto.request.FailLoanRequestDto;
import com.sixheadword.gappa.loan.dto.request.RedemptionRequestDto;
import com.sixheadword.gappa.loan.dto.request.SuccessLoanRequestDto;
import com.sixheadword.gappa.loan.repository.LoanRepository;
import com.sixheadword.gappa.loanHistory.entity.LoanHistory;
import com.sixheadword.gappa.loanHistory.entity.Type;
import com.sixheadword.gappa.loanHistory.repository.LoanHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.time.LocalDateTime;

@Service
@Transactional
@RequiredArgsConstructor
public class MoneyService {

    private final LoanRepository loanRepository;
    private final LoanHistoryRepository loanHistoryRepository;
    private final AccountRepository accountRepository;
    private final EntityManager em;

    // 대출 실행
    public void successLoan (SuccessLoanRequestDto successLoanRequestDto){
        Loan loan = loanRepository.findById(successLoanRequestDto.getLoanSeq()).orElse(null);
        if(loan != null){
            // 대출 상태 및 실행일자 변경
            loan.setStatus('O');
            loan.setStartDate(LocalDateTime.now());
            // 대출금 이체 실행
            transfer(loan, 0L, 1);
            loanRepository.save(loan);
        }else{
            throw new IllegalArgumentException("대출 실행에 실패했습니다.");
        }
    }

    // 대출 취소
    public void failLoan(FailLoanRequestDto failLoanRequestDto){
        // 대출 상태 변경
        Loan loan = loanRepository.findById(failLoanRequestDto.getLoanSeq()).orElse(null);
        if(loan != null){
            loan.setStatus('F');
            loanRepository.save(loan);
        }else{
            throw new IllegalArgumentException("대출 실행 취소에 실패했습니다.");
        }
    }

    // 대출금 상환
    public void redemptionMoney(RedemptionRequestDto redemptionRequestDto){
        // 현 대출 건
        Loan loan = loanRepository.findById(redemptionRequestDto.getLoanSeq()).orElse(null);
        if(loan != null){
            // 대출 내역 저장(연체중)
            if(loan.getStatus() == 'D'){
                // 상환금 이체 실행
                transfer(loan, redemptionRequestDto.getAmount(), 0);
                // 내역 저장
                LoanHistory loanHistory = new LoanHistory();
                loanHistory.setLoan(loan);
                loanHistory.setType(Type.INTEREST);
                loanHistory.setAmount(redemptionRequestDto.getAmount());
                loanHistory.setOldRedemptionMoney(loan.getRedemptionMoney());
                loanHistory.setNewRedemptionMoney(loan.getRedemptionMoney() + redemptionRequestDto.getAmount());
                loanHistory.setTransactionDate(LocalDateTime.now());
                loanHistoryRepository.save(loanHistory);
                // 대출 내역의 상환금 업데이트
                Long totalRedemptionMoney = loan.getRedemptionMoney() + redemptionRequestDto.getAmount();
                // 현재까지의 총 상환금이 '(대출원금+이자)*연체일자' 를 넘어가면 상환 완료
                int lateDate = LocalDateTime.now().compareTo(loan.getRedemptionDate());
                if(totalRedemptionMoney >= (loan.getPrincipal() + loan.getInterest()) * lateDate) {
                    loan.setStatus('C');
                    loan.setExpiredDate(LocalDateTime.now());
                }
                loan.setRedemptionMoney(totalRedemptionMoney);
            }
            // 대출 내역 저장(진행중)
            else if(loan.getStatus() == 'O'){
                // 상환금 이체 실행
                transfer(loan, redemptionRequestDto.getAmount(), 0);
                // 내역 저장
                LoanHistory loanHistory = new LoanHistory();
                loanHistory.setLoan(loan);
                loanHistory.setType(Type.REDEMPTION);
                loanHistory.setAmount(redemptionRequestDto.getAmount());
                loanHistory.setOldRedemptionMoney(loan.getRedemptionMoney());
                loanHistory.setNewRedemptionMoney(loan.getRedemptionMoney() + redemptionRequestDto.getAmount());
                loanHistory.setTransactionDate(LocalDateTime.now());
                loanHistoryRepository.save(loanHistory);
                // 대출 내역의 상환금 업데이트
                Long totalRedemptionMoney = loan.getRedemptionMoney() + redemptionRequestDto.getAmount();
                // 현재까지의 총 상환금이 대출원금을 넘어가면 상환 완료
                if(totalRedemptionMoney >= loan.getPrincipal()){
                    loan.setStatus('C');
                    loan.setExpiredDate(LocalDateTime.now());
                }
                loan.setRedemptionMoney(totalRedemptionMoney);
            }
        }else{
            throw new IllegalArgumentException("대출 건 조회에 실패했습니다.");
        }

    }

    // 송급 및 입금 실행
    @Transactional
    public void transfer(Loan loan, Long amount, int type){
        // 채무자
        Account fromUserAccount = accountRepository.findPrimaryByUserSeq(loan.getFromUser().getUserSeq());
        // 채권자
        Account toUserAccount = accountRepository.findPrimaryByUserSeq(loan.getToUser().getUserSeq());

        // type = 0) 상환에 의한 송금 및 입금
        if(type == 0){
            if(fromUserAccount.getBalance() >= amount){
                // 채무자가 송금
                fromUserAccount.setMinusBalance(amount);
                // 채권자는 입금
                toUserAccount.setAddBalance(amount);
            }else {
                throw new IllegalArgumentException("현재 계좌에 잔액이 부족합니다.");
            }
        }
        // type = 1) 대출 실행에 의한 송금 및 입금
        if(type == 1){
            // 잔액 있는지 확인
            if(toUserAccount.getBalance() >= loan.getRedemptionMoney()){
                // 채권자가 송금
                toUserAccount.setMinusBalance(loan.getRedemptionMoney());
                // 채무자는 입금
                fromUserAccount.setAddBalance(loan.getRedemptionMoney());
            }else {
                throw new IllegalArgumentException("현재 계좌에 잔액이 부족합니다.");
            }
        }
    }
}
