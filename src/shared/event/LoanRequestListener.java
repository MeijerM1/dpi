package shared.event;

import shared.model.loan.LoanRequest;

/**
 * @author Max Meijer
 * Created on 15/02/2019
 */
public interface LoanRequestListener {
    void onLoanRequest(LoanRequest loanRequest, String correlation);
}
