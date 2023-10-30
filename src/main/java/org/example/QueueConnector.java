package org.example;

import com.ibm.mq.jakarta.jms.MQQueue;
import com.ibm.mq.jakarta.jms.MQQueueConnectionFactory;
import com.ibm.msg.client.jakarta.wmq.WMQConstants;
import jakarta.jms.*;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.Optional.*;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class QueueConnector {

    private final Optional<String> qmgr = ofNullable(System.getenv("MQ_QMGR"));
    private final Optional<String> qmhost = ofNullable(System.getenv("MQ_QMGR_HOST"));
    private final Optional<String> qmport = ofNullable(System.getenv("MQ_QMGR_PORT"));
    private final Optional<String> srvchannel = ofNullable(System.getenv("MQ_SRV_CHANNEL"));
    private final Optional<String> mqappname = ofNullable(System.getenv("MQ_APP_NAME"));
    private final Optional<String> userid = ofNullable(System.getenv("MQ_USER_ID"));
    private final Optional<String> password = ofNullable(System.getenv("MQ_USER_PASSWORD"));
    private final Optional<String> queue1 = ofNullable(System.getenv("MQ_QUEUE_1"));

    private final MQQueueConnectionFactory cf = new MQQueueConnectionFactory();

    public QueueConnector() throws JMSException {
        this.configureQueueConnectionFactory();
    }

    private void configureQueueConnectionFactory() throws JMSException {
        this.cf.setTransportType(WMQConstants.WMQ_CM_CLIENT);
        this.cf.setQueueManager(qmgr.orElseThrow(() -> new IllegalArgumentException("qmgr name required")));
        this.cf.setHostName(qmhost.orElseThrow(() -> new IllegalArgumentException("qmgr host required")));
        this.cf.setPort(Integer.parseInt(qmport.orElseThrow(() -> new IllegalArgumentException("qmgr port required"))));
        this.cf.setChannel(srvchannel.orElseThrow(() -> new IllegalArgumentException("server channel required")));
        this.cf.setAppName(mqappname.orElseThrow(() -> new IllegalArgumentException("app name required")));
    }

    public Connection startConnection() throws JMSException {
        String u = userid.orElseThrow(() -> new IllegalArgumentException("mq user id required"));
        String p = password.orElseThrow(() -> new IllegalArgumentException("mq user password required"));

        Connection c = this.cf.createConnection(u, p);
        c.start();

        System.out.println("connected...");
        return c;
    }

    public Queue createQueue1() throws JMSException {
        // refactor
        return new MQQueue("queue://qm1/dev.q1?persistence=1&targetClient=0");
    }

    public Session createTransactedSession(@NotNull Connection c) throws JMSException {
        return c.createSession(true, Session.SESSION_TRANSACTED);
    }

    public Message createTextMessage(@NotNull Session s, String textMessage) {
        try {
            return s.createTextMessage(textMessage);
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Message> convertMessages(Session s, @NotNull List<String> textMessages) {
        return textMessages.stream().map((tm) -> createTextMessage(s, tm)).toList();
    }

    public void sendTextMessages(Connection c, Queue queue, List<String> textMessages) {

        try (Session s  = this.createTransactedSession(c);) {

            this.sendMessages(s, queue, this.convertMessages(s, textMessages));

        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendMessages(@NotNull Session s, Queue queue, @NotNull List<Message> messages) throws JMSException {
        MessageProducer mp = s.createProducer(queue);
        for (Message m : messages) {
            mp.send(m);
        }
        s.commit();

        System.out.println("sent " + messages.size() + " messages");
        System.out.println("session committed...");
    }

    public void sendOneMessage(@NotNull Session s, Queue queue, Message message) throws JMSException {
        MessageProducer producer = s.createProducer(queue);
        producer.send(message);
        s.commit();
    }

    public Optional<Message> receiveOneMessageNoCommit(Session s, Queue queue) throws JMSException {
        return receiveOneMessage(s, queue, false);
    }

    public Optional<Message> receiveOneMessageCommit(Session s, Queue queue) throws JMSException {
        return receiveOneMessage(s, queue, true);
    }

    public Optional<Message> receiveOneMessage(@NotNull Session s, Queue queue, boolean commit) throws JMSException {
        MessageConsumer consumer = s.createConsumer(queue);
        Message message = consumer.receiveNoWait();

        if (message != null) {
            if (commit) {
                s.commit();
            }
            return Optional.of(message);
        }

        return Optional.empty();
    }

    public List<Message> receiveAllMessages(Connection c, Queue queue) {
        try (Session s = createTransactedSession(c);) {
            return receiveAllMessages(s, queue);

        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Message> receiveAllMessages(Session s, Queue queue) throws JMSException {
        return this.receiveAllMessages(s, queue, 0);
    }

    public List<Message> receiveAllMessages(@NotNull Session s, Queue queue, int commitCount) throws JMSException {
        MessageConsumer consumer = s.createConsumer(queue);
        List<Message> messages = new ArrayList<>();

        Message m = consumer.receiveNoWait();

        int mcount = 0;
        while (m != null) {
            mcount++;
            messages.add(m);

            if (commitCount > 0 && mcount >= commitCount) {
                s.commit();
                mcount = 0;
            }

            m = consumer.receiveNoWait();
        }

        if (!messages.isEmpty()) {
            if (commitCount == 0 || mcount > 0) {
                s.commit();
            }
        }

        return messages;
    }
}
