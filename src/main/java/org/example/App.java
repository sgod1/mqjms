package org.example;

//import jakarta.jms.Connection;
//import jakarta.jms.JMSException;
//import jakarta.jms.Message;
//import jakarta.jms.Queue;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Predicate;

import static org.example.ApplicationWorker.*;

public class App
{
    public static void main(String[] args) throws JMSException, IOException {

        ApplicationConfiguration appcfg = new ApplicationConfiguration();

        final String propertiesFile = "app.properties";
        System.out.println("Using " + propertiesFile + " configuration file");

        appcfg.loadProperties(propertiesFile);

        long connectStartMs = new Date().getTime();

        QueueConnector qc = new QueueConnector(appcfg);
        Queue q1 = qc.createQueue1();
        Connection c = qc.startConnection();

        long connectEndMs = new Date().getTime();
        long connectDuration = connectEndMs - connectStartMs;
        displayConnectTime(connectDuration, appcfg);

        // send message batch
        int msgSizeBytes = appcfg.getMessageSize().orElseThrow(() -> new IllegalArgumentException("message size required"));
        int batchSizeMessages = appcfg.getBatchSizeMessages().orElseThrow(() -> new IllegalArgumentException("message size required"));

        int sendTrheads = appcfg.getSendThreads().orElseThrow(() -> new IllegalArgumentException("send threads required"));
        int receiveThreads = appcfg.getReceiveThreads().orElseThrow(() -> new IllegalArgumentException("receive threads required"));

        int sendCommitCount = appcfg.getSendCommitCount().orElseThrow(() -> new IllegalArgumentException("send commit count required"));
        int receiveCommitCount = appcfg.getReceiveCommitCount().orElseThrow(() -> new IllegalArgumentException("receive commut count required"));

        // send messages
        String msg = ApplicationWorker.createTextMessage(msgSizeBytes);

        List<String> textMessages = new ArrayList<>();

        for (int mc = 0; mc < batchSizeMessages; mc++) {
            textMessages.add(msg);
        }

        int sendMessagesPerThread = batchSizeMessages / sendTrheads;
        List<Thread> sendThreadsList = new ArrayList<>();

        long sendStartMs = new Date().getTime();

        int msgIdx = 0;
        for (int st = 0; st < sendTrheads; st++) {
            List<String> sendSublist = new ArrayList<>(textMessages.subList(msgIdx, msgIdx + sendMessagesPerThread));
            // java 21
//            sendThreadsList.add(Thread.ofVirtual().start(MessageWorker.sendTextMessages(qc, c, q1, sendSublist, sendCommitCount)));
            sendThreadsList.add(new Thread(MessageWorker.sendTextMessages(qc, c, q1, sendSublist, sendCommitCount)));
            msgIdx = sendMessagesPerThread;
        }

        // java 11
        sendThreadsList.forEach(Thread::start);

        sendThreadsList.forEach((t) -> {
            try {
                t.join();
            } catch (InterruptedException ignored) {
            }
        });

        long sendEndMs = new Date().getTime();
        long sendDuration = sendEndMs - sendStartMs;
        displaySendProcessingTime(sendDuration, appcfg);

        // receive messages
        Predicate<Message> fastwork = getFastwork();

        long receiveStartMs = new Date().getTime();

        List<Thread> receiveThreadList = new ArrayList<>();
        for (int r = 0; r < receiveThreads; r++) {
            // java 21
//            receiveThreadList.add(Thread.ofVirtual().start(MessageWorker.receiveAllMessages(qc, c, q1, receiveCommitCount, fastwork)));
            receiveThreadList.add(new Thread(MessageWorker.receiveAllMessages(qc, c, q1, receiveCommitCount, fastwork)));
        }

        // java 11
        receiveThreadList.forEach(Thread::start);

        receiveThreadList.forEach((t) -> {
            try {
                t.join();
            } catch (InterruptedException ignored) {
            }
        });

        long receiveEndMs = new Date().getTime();
        long receiveDuration = receiveEndMs - receiveStartMs;
        displayReceiveProcessingTime(receiveDuration, appcfg);

        // stop and close connection
        c.stop();
        c.close();
    }
}
