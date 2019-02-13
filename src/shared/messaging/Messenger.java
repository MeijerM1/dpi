package shared.messaging;

import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.Serializable;
import java.util.Properties;

/**
 * @author Max Meijer
 * Created on 06/02/2019
 */
public class Messenger {

    private Connection connection; // to connect to the ActiveMQ
    private Session session; // session for creating messages, producers and

    private Destination sendDestination; // reference to a queue/topic destination
    private Destination receiveDestination;

    private MessageProducer producer; // for sending messages
    private MessageConsumer consumer; // for receiving messages

    private String queueName;
    private boolean isReceiver;

    public Messenger(boolean isReceiver, String queue) {
        this.queueName = queue;
        this.isReceiver = isReceiver;

        setup();
    }

    private void setup() {
        try {
            Properties props = new Properties();
            props.setProperty(Context.INITIAL_CONTEXT_FACTORY,
                    "org.apache.activemq.jndi.ActiveMQInitialContextFactory");

            props.setProperty(Context.PROVIDER_URL, "tcp://localhost:61616");

            props.put(("queue." + queueName), queueName);

            Context jndiContext = new InitialContext(props);
            ActiveMQConnectionFactory connectionFactory = (ActiveMQConnectionFactory) jndiContext
                    .lookup("ConnectionFactory");
            connectionFactory.setTrustAllPackages(true);
            connection = connectionFactory.createConnection();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            if(isReceiver) {
                // connect to the receiver destination
                receiveDestination = (Destination) jndiContext.lookup(queueName);
                consumer = session.createConsumer(receiveDestination);

                connection.start(); // this is needed to start receiving messages
            } else {
                // connect to the receiver destination
                sendDestination = (Destination) jndiContext.lookup(queueName);
                producer = session.createProducer(sendDestination);
            }



        } catch (NamingException | JMSException e) {
            e.printStackTrace();
        }

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

    public void addReceiver(MessageListener listener) {
        try {
            consumer.setMessageListener(listener);
        } catch (JMSException e) {
            System.out.println();
            e.printStackTrace();
        }
    }
}
