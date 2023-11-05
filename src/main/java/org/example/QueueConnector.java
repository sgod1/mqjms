package org.example;

//import com.ibm.mq.jakarta.jms.MQQueue;
//import com.ibm.mq.jakarta.jms.MQQueueConnectionFactory;
//import com.ibm.msg.client.jakarta.wmq.WMQConstants;
//import jakarta.jms.*;

import com.ibm.mq.jms.MQConnectionFactory;
import com.ibm.mq.jms.MQQueue;
import com.ibm.msg.client.wmq.WMQConstants;
import org.jetbrains.annotations.NotNull;

import javax.jms.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.example.ApplicationWorker.displayConnectTime;

public class QueueConnector {

    private final ApplicationConfiguration cfg;
    private final MQConnectionFactory cf = new MQConnectionFactory();

    public QueueConnector(ApplicationConfiguration cfg) throws JMSException {
        this.cfg = cfg;
        this.configureQueueConnectionFactory();
    }

    private void configureQueueConnectionFactory() throws JMSException {
        this.cf.setTransportType(WMQConstants.WMQ_CM_CLIENT);

        if (cfg.getCCDTUrl().isEmpty()) {
            this.cf.setQueueManager(cfg.getQueueManager().orElseThrow(() -> new IllegalArgumentException("qmgr name required")));
            this.cf.setHostName(cfg.getQueueManagerHost().orElseThrow(() -> new IllegalArgumentException("qmgr host required")));
            this.cf.setPort(cfg.getQueueManagerPort().orElseThrow(() -> new IllegalArgumentException("qmgr port required")));
            this.cf.setChannel(cfg.getServerChannel().orElseThrow(() -> new IllegalArgumentException("server channel required")));
            this.cf.setAppName(cfg.getMQApplicationName().orElseThrow(() -> new IllegalArgumentException("app name required")));

            if (cfg.getSSLCipherSuite().isPresent()) {
                cf.setSSLCipherSuite(cfg.getSSLCipherSuite().get());
            }

        } else {
            try {
                cf.setCCDTURL(new URL(cfg.getCCDTUrl().get()));
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
            this.cf.setQueueManager(cfg.getQueueManager().orElseThrow(() -> new IllegalArgumentException("qmgr name required")));
        }

        if (cfg.getTruststore().isPresent()) {
            System.setProperty("javax.net.ssl.trustStore", cfg.getTruststore().get());
            System.setProperty("javax.net.ssl.trustStorePassword", cfg.getTruststorePassword().orElseThrow(() -> new IllegalArgumentException("trust store password required")));
        }

        if (cfg.getKeystore().isPresent()) {
            System.setProperty("javax.net.ssl.keyStore", cfg.getKeystore().get());
            System.setProperty("javax.net.ssl.keyStorePassword", cfg.getKeystorePassword().orElseThrow(() -> new IllegalArgumentException("key store password required")));
        }
    }

    public Connection startConnection() throws JMSException {

        final String u = cfg.getMQUserName().orElse("");
        final String p = cfg.getMQUserPassword().orElse("");

        long connectStartMs = new Date().getTime();

        Connection c = this.cf.createConnection(u,p);
        c.start();

//        System.out.println("connected...");

//        long connectEndMs = new Date().getTime();
//        long connectDuration = connectEndMs - connectStartMs;
//        displayConnectTime(connectDuration, this.cfg);

        return c;
    }

    public boolean getThreadsShareConnection() {
        return this.cfg.getThreadsShareConnection().isEmpty() || !this.cfg.getThreadsShareConnection().get().equalsIgnoreCase("no");
    }

    public Queue createQueue1() throws JMSException {

        final String queue = cfg.getQueueName().orElseThrow(() -> new IllegalArgumentException("queue name required"));
        final String qmgr = cfg.getQueueManager().orElseThrow(() -> new IllegalArgumentException("qmgr name required"));

        // todo: message persistence param, message priority param
        final String queueUrl = "queue://" + qmgr + "/" + queue + "?persistence=2&targetClient=0";

        return new MQQueue(queueUrl);
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
        return textMessages.stream().map((tm) -> createTextMessage(s, tm)).collect(Collectors.toList());
    }

    public void sendTextMessages(@NotNull Session s, Queue queue, List<String> textMessages, int commitCount) {

        try {
            this.sendMessages(s, queue, this.convertMessages(s, textMessages), commitCount);

        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendMessages(@NotNull Session s, Queue queue, @NotNull List<Message> messages, int commitCount) throws JMSException {
        MessageProducer mp = s.createProducer(queue);

        int mcount = 0;
        for (Message m : messages) {
            mp.send(m);

            mcount++;
            if (commitCount > 0 && mcount >= commitCount) {
                s.commit();
                System.out.print(" sent " + mcount + " msgs;");

                mcount = 0;
            }
        }

        if (mcount > 0) {
            s.commit();
            System.out.print(" sent " + mcount + " msgs;");
        }
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
                System.out.print(" rcv " + mcount + " msgs;");
                mcount = 0;
            }

            m = consumer.receiveNoWait();
        }

        if (!messages.isEmpty()) {
            if (commitCount == 0 || mcount > 0) {
                s.commit();
            }

            if (commitCount == 0) {
                System.out.print(" rcv " + messages.size() + " msgs;");

            } else if (mcount > 0) {
                System.out.print(" rcv " + mcount + " msgs;");
            }
        }

        return messages;
    }
}
