package shared.event;

import shared.model.bank.BankInterestRequest;

/**
 * @author Max Meijer
 * Created on 15/02/2019
 */
public interface BankRequestListener {
    void onBankRequest(BankInterestRequest request, String correlation);
}
