package org.example;

import jakarta.jms.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Predicate;

import static org.example.ApplicationWorker.getFastwork;
import static org.example.ApplicationWorker.getSlowwork;

public class App
{
    public static void main(String[] args) throws JMSException, InterruptedException, IOException {

        ApplicationConfiguration appcfg = new ApplicationConfiguration();

        final String propertiesFile = "app.properties";
        appcfg.loadProperties(propertiesFile);

        QueueConnector qc = new QueueConnector(appcfg);
        Queue q1 = qc.createQueue1();
        Connection c = qc.startConnection();

        // send message batch
        List<String> textMessages = new ArrayList<>();

        for (int mc = 0; mc < 10; mc++) {
            textMessages.add("hello world " + new Date().getTime() + "-" + mc);
        }

        qc.sendTextMessages(c, q1, textMessages);

        // receive messages
        Predicate<Message> slowwork = getSlowwork(5 * 1000);
        Predicate<Message> fastwork = getFastwork();

        Runnable slowrun = MessageWorker.receieveOneMessage(qc, c, q1, slowwork);
        Runnable fastrun = MessageWorker.receieveOneMessage(qc, c, q1, fastwork);

        Thread t1  = Thread.ofVirtual().start(slowrun);
        Thread t2  = Thread.ofVirtual().start(slowrun);

        Thread t3  = Thread.ofVirtual().start(fastrun);
        Thread t4  = Thread.ofVirtual().start(fastrun);
        Thread t5  = Thread.ofVirtual().start(fastrun);
        Thread t6  = Thread.ofVirtual().start(fastrun);
        Thread t7  = Thread.ofVirtual().start(fastrun);
        Thread t8  = Thread.ofVirtual().start(fastrun);
        Thread t9  = Thread.ofVirtual().start(fastrun);
        Thread t10  = Thread.ofVirtual().start(fastrun);

        t1.join();
        t2.join();
        t3.join();
        t4.join();
        t5.join();
        t6.join();
        t7.join();
        t8.join();
        t9.join();
        t10.join();

        // receive all messages
        List<Message> rmsgs = qc.receiveAllMessages(c, q1);
        if (!rmsgs.isEmpty()) {
            System.out.println("received " + rmsgs.size() + " remaining messages...");
        } else {
            System.out.println("no more messages..");
        }

        for (Message rm : rmsgs) {
            if (rm instanceof TextMessage) {
                System.out.println("text message... " + ((TextMessage) rm).getText());
            }
        }

        // stop and close connection
        c.stop();
        c.close();
    }
}
