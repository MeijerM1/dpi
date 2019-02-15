package shared.gateway;

import shared.event.BankRequestListener;
import shared.event.LoanReplyListener;
import shared.messaging.MessageReceiverGateway;
import shared.messaging.MessageSenderGateway;
import shared.model.bank.BankInterestReply;
import shared.model.bank.BankInterestRequest;
import shared.model.loan.LoanReply;
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
public class LoanBrokerAppGateway {
    private MessageReceiverGateway receiver;
    private MessageSenderGateway sender;
    private IdGenerator generator;
    private HashMap<String, LoanRequest> loanRequests = new HashMap<>();
    private HashMap<BankInterestRequest, String> bankRequests = new HashMap<>();

    private List<BankRequestListener> bankListeners = new ArrayList<>();
    private List<LoanReplyListener> loanListeners = new ArrayList<>();

    public LoanBrokerAppGateway(String senderQueue, String receiverQueue) {
        generator = new IdGenerator();
        sender = new MessageSenderGateway(senderQueue);
        receiver = new MessageReceiverGateway(receiverQueue);

        receiver.addReceiver(new MessageListener() {
            @Override
            public void onMessage(Message message) {
                handleMessage(message);
            }
        });
    }

    private void handleMessage(Message message) {
        ObjectMessage obj = (ObjectMessage) message;

        try {
            Serializable object = obj.getObject();
            if (object instanceof BankInterestRequest) {
                BankInterestRequest request = (BankInterestRequest) object;
                onBankRequest(request, message.getJMSCorrelationID());
            } else if(object instanceof LoanReply) {
                LoanReply reply = (LoanReply) object;
                onLoanReplyArrived(reply, message.getJMSCorrelationID());
            } else {
                System.out.print("Invalid object send to LoanBrokerAppGateway, ignoring");
            }

        } catch (JMSException e) {
            e.printStackTrace();
        }

    }

    public void applyForLoan(LoanRequest request) {
        Message message = sender.createMessage(request);

        String id = generator.generateString();

        loanRequests.put(id, request);

        try {
            message.setJMSCorrelationID(id);
        } catch (JMSException e) {
            // YOLO
            e.printStackTrace();
        }

        sender.sendMessage(message);
    }

    private void onBankRequest(BankInterestRequest request, String correlation) {
        bankRequests.put(request, correlation);

        for (BankRequestListener listener :
                bankListeners) {
            listener.onBankRequest(request, correlation);
        }
    }

    private void onLoanReplyArrived(LoanReply reply, String correlation) {
        LoanRequest request = loanRequests.get(correlation);

        for (LoanReplyListener listener :
                loanListeners) {
            listener.onLoanReply(request, reply);
        }
    }

    public void sendBankInterestReply(BankInterestRequest request, BankInterestReply reply) {
        String correlation = bankRequests.get(request);

        Message message = sender.createMessage(reply);

        try {
            message.setJMSCorrelationID(correlation);
        } catch (JMSException e) {
            e.printStackTrace();
        }

        sender.sendMessage(message);
    }

    public void addBankRequestListener(BankRequestListener listener) {
        bankListeners.add(listener);
    }

    public void addLoanReplyListener(LoanReplyListener listener) {
        loanListeners.add(listener);
    }
}

