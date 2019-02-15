package shared.gateway;

import broker.loanbroker.LoanBrokerFrame;
import shared.event.BankReplyListener;
import shared.messaging.MessageReceiverGateway;
import shared.messaging.MessageSenderGateway;
import shared.model.bank.BankInterestReply;
import shared.model.bank.BankInterestRequest;
import shared.model.loan.LoanRequest;
import shared.util.IdGenerator;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author Max Meijer
 * Created on 13/02/2019
 */
@SuppressWarnings("Duplicates")
public class BankAppGateway {
    private MessageReceiverGateway receiver;
    private MessageSenderGateway sender;

    private HashMap<String, LoanRequest> requests = new HashMap<>();

    private List<BankReplyListener> listeners = new ArrayList<>();

    public BankAppGateway(String senderQueue,
                          String receivedQueue) {
        receiver = new MessageReceiverGateway(receivedQueue);
        sender = new MessageSenderGateway(senderQueue);

        receiver.addReceiver(new MessageListener() {
            @Override
            public void onMessage(Message message) {
                handleMessgae(message);
            }
        });
    }

    public void handleMessgae(Message message) {
        ObjectMessage obj = (ObjectMessage) message;

        try {
            Serializable object = obj.getObject();

            if(object instanceof BankInterestReply) {
                BankInterestReply reply = (BankInterestReply) object;

                onBankReplyArrived(reply, message.getJMSCorrelationID());
            } else {
                System.out.println("Invalid object send to BankAppGateway, ignoring");
            }

        } catch (JMSException e) {
            e.printStackTrace();
        }

    }

    public void sendBankRequest(LoanRequest loanRequest,BankInterestRequest bankRequest, String correlation) {
        requests.put(correlation, loanRequest);

        Message message = sender.createMessage(bankRequest);

        try {
            message.setJMSCorrelationID(correlation);
        } catch (JMSException e) {
            e.printStackTrace();
        }

        sender.sendMessage(message);
    }

    public void onBankReplyArrived(BankInterestReply reply, String correlation) {
        LoanRequest request = requests.get(correlation);

        for (BankReplyListener listener : listeners) {
            listener.onBankReply(request, reply, correlation);
        }
    }

    public void addReplyListneer(BankReplyListener listener) {
        listeners.add(listener);
    }
}
