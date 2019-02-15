package shared.gateway;

import broker.loanbroker.LoanBrokerFrame;
import client.loanclient.LoanClientFrame;
import org.apache.commons.logging.Log;
import shared.event.LoanRequestListener;
import shared.messaging.MessageReceiverGateway;
import shared.messaging.MessageSenderGateway;
import shared.model.loan.LoanReply;
import shared.model.loan.LoanRequest;
import shared.util.IdGenerator;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Max Meijer
 * Created on 13/02/2019
 */
public class LoanClientAppGateway {
    private MessageReceiverGateway receiver;
    private MessageSenderGateway sender;
    private List<LoanRequestListener> listeners = new ArrayList<>();


    public LoanClientAppGateway(String senderQueue,
                                String receiverQueue) {

        receiver = new MessageReceiverGateway(receiverQueue);
        sender = new MessageSenderGateway(senderQueue);

        receiver.addReceiver(new MessageListener() {
            @Override
            public void onMessage(Message message) {
                handleMessage(message);
            }
        });
    }

    public void onLoanRequestArrived(LoanRequest request, String correlation) {
        for (LoanRequestListener listener :
                listeners) {
            listener.onLoanRequest(request, correlation);
        }
    }

    public void sendLoanReply(LoanReply reply, String correlation) {
        Message message = sender.createMessage(reply);

        try {
            message.setJMSCorrelationID(correlation);
        } catch (JMSException e) {
            e.printStackTrace();
        }

        sender.sendMessage(message);
    }



    public void handleMessage(Message message) {
        ObjectMessage obj = (ObjectMessage) message;

        try {
            Serializable object = obj.getObject();
            if (object instanceof LoanRequest) {
                LoanRequest request = (LoanRequest) object;
                onLoanRequestArrived(request, message.getJMSCorrelationID());
            } else {
                System.out.print("Invalid object send to LoanClientAppGateway, ignoring");
            }

        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    public void addLoanRequestListener(LoanRequestListener listener) {
        listeners.add(listener);
    }

}
