package shared.event;

import shared.model.bank.BankInterestReply;
import shared.model.loan.LoanRequest;

/**
 * @author Max Meijer
 * Created on 15/02/2019
 */
public interface BankReplyListener {
    void onBankReply(LoanRequest request, BankInterestReply reply, String correlation);
}
