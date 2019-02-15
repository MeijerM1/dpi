package shared.messaging;

import javax.jms.JMSException;
import javax.jms.Message;
import java.io.Serializable;

/**
 * @author Max Meijer
 * Created on 13/02/2019
 */
public class MessageSenderGateway extends Messenger {
    public MessageSenderGateway(String queue) {
        super(false, queue);
    }

    public Message createMessage(Serializable object) {
        try {
            return session.createObjectMessage(object);
        } catch (JMSException e) {
            e.printStackTrace();
        }

        return null;
    }

    public void sendMessage(Message message) {
        try {
            producer.send(message);
            System.out.println("Message send with corr. id: " + message.getJMSCorrelationID());
        } catch (JMSException e) {
            System.out.println("Error sending message");
            e.printStackTrace();
        }
    }

}
