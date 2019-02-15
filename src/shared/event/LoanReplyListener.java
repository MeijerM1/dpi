package shared.event;

import shared.model.loan.LoanReply;
import shared.model.loan.LoanRequest;

/**
 * @author Max Meijer
 * Created on 15/02/2019
 */
public interface LoanReplyListener {

    void onLoanReply(LoanRequest request, LoanReply reply);
}
