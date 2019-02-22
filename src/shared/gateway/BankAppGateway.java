package shared.gateway;

import broker.loanbroker.LoanBrokerFrame;
import shared.event.BankReplyListener;
import shared.messaging.BankReplyAggregator;
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
    private static int nextAggregationId = 1;
    private MessageReceiverGateway receiver;

    private MessageSenderGateway abnSender;
    private MessageSenderGateway raboSender;
    private MessageSenderGateway ingSender;

    private HashMap<String, LoanRequest> requests = new HashMap<>();
    private List<BankReplyListener> listeners = new ArrayList<>();
    private List<BankReplyAggregator> aggregators = new ArrayList<>();

    public BankAppGateway(String senderQueue,
                          String receivedQueue) {
        receiver = new MessageReceiverGateway(receivedQueue);

        abnSender = new MessageSenderGateway("BrokerAbnBank");
        raboSender= new MessageSenderGateway("BrokerRaboBank");
        ingSender = new MessageSenderGateway("BrokerIngBank");

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

                onBankReplyArrived(reply, message.getJMSCorrelationID(), message.getIntProperty("aggregation"));
            } else {
                System.out.println("Invalid object send to BankAppGateway, ignoring");
            }

        } catch (JMSException e) {
            e.printStackTrace();
        }

    }

    public void sendBankRequest(LoanRequest loanRequest,
                                BankInterestRequest bankRequest,
                                String correlation) {

        requests.put(correlation, loanRequest);

        Message message = abnSender.createMessage(bankRequest);

        try {
            message.setIntProperty("aggregation", nextAggregationId);

            message.setJMSCorrelationID(correlation);
        } catch (JMSException e) {
            e.printStackTrace();
        }

        int expectedMessages = 0;

        if(loanRequest.getAmount() <= 100000 && loanRequest.getTime() <= 10) {
            ingSender.sendMessage(message);
            expectedMessages++;
        }

        if(loanRequest.getAmount() >= 200000 && loanRequest.getAmount() <= 300000 && loanRequest.getTime() <= 20) {
            abnSender.sendMessage(message);
            expectedMessages++;
        }

        if(loanRequest.getAmount() <= 250000 && loanRequest.getTime() <= 15) {
            raboSender.sendMessage(message);
            expectedMessages++;
        }

        BankReplyAggregator aggregator = new BankReplyAggregator(expectedMessages, nextAggregationId, correlation);

        aggregators.add(aggregator);
        nextAggregationId++;
    }

    public void onBankReplyArrived(BankInterestReply reply, String correlation, int aggregation) {
        LoanRequest request = requests.get(correlation);

        BankReplyAggregator agg = aggregators.stream()
                .filter(a -> a.getAggregation() == aggregation)
                .findFirst()
                .get();

        agg.addReply(reply);

        if(agg.isReplyComplete()) {
            BankInterestReply bestReply = agg.getBestOffer();

            for (BankReplyListener listener : listeners) {
                listener.onBankReply(request, bestReply, correlation);
            }
        }

    }

    public void addReplyListneer(BankReplyListener listener) {
        listeners.add(listener);
    }
}
