package org.example;

//import jakarta.jms.JMSException;
//import jakarta.jms.Message;
//import jakarta.jms.TextMessage;

import org.jetbrains.annotations.NotNull;

import javax.jms.*;
import javax.jms.Queue;

import java.lang.IllegalStateException;
import java.util.*;
import java.util.function.Predicate;

public class ApplicationWorker {

    public static long sendMessages(QueueConnector qc, Queue q1, Connection c, @NotNull ApplicationConfiguration appcfg) {

        // send message batch
        int msgSizeBytes = appcfg.getMessageSize().orElseThrow(() -> new IllegalArgumentException("message size required"));
        int batchSizeMessages = appcfg.getBatchSizeMessages().orElseThrow(() -> new IllegalArgumentException("message size required"));

        int sendTrheads = appcfg.getSendThreads().orElseThrow(() -> new IllegalArgumentException("send threads required"));
        int sendCommitCount = appcfg.getSendCommitCount().orElseThrow(() -> new IllegalArgumentException("send commit count required"));

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
            sendThreadsList.add(new Thread(MessageWorker.sendTextMessages(qc, c, q1, sendSublist, sendCommitCount)));
            msgIdx = sendMessagesPerThread;
        }

        sendThreadsList.forEach(Thread::start);
        sendThreadsList.forEach(ApplicationWorker::joinThread);

        long sendEndMs = new Date().getTime();
        long sendDuration = sendEndMs - sendStartMs;
        displaySendProcessingTime(sendDuration, appcfg);

        return sendDuration;
    }

    public static long receiveMessages(QueueConnector qc, Queue q1, Connection c, @NotNull ApplicationConfiguration appcfg) {

        int receiveThreads = appcfg.getReceiveThreads().orElseThrow(() -> new IllegalArgumentException("receive threads required"));
        int receiveCommitCount = appcfg.getReceiveCommitCount().orElseThrow(() -> new IllegalArgumentException("receive commut count required"));

        Predicate<Message> fastwork = getFastwork();

        long receiveStartMs = new Date().getTime();

        List<Thread> receiveThreadList = new ArrayList<>();
        for (int r = 0; r < receiveThreads; r++) {
            receiveThreadList.add(new Thread(MessageWorker.receiveAllMessages(qc, c, q1, receiveCommitCount, fastwork)));
        }

        receiveThreadList.forEach(Thread::start);
        receiveThreadList.forEach(ApplicationWorker::joinThread);

        long receiveEndMs = new Date().getTime();
        long receiveDuration = receiveEndMs - receiveStartMs;
        displayReceiveProcessingTime(receiveDuration, appcfg);

        return receiveDuration;
    }

    private static void joinThread(@NotNull Thread thread) {
        try {
            thread.join();
        } catch (InterruptedException ignored) {
        }
    }

    public static Optional<TextMessage> getTextMessage(Message message) {
        return message instanceof TextMessage ? Optional.of((TextMessage) message) : Optional.empty();
    }

    @NotNull
    public static Predicate<Message> getFastwork() {

        return (m) -> {
            try {
                String mstring = getTextMessage(m).orElseThrow().getText();
//                System.out.println("fast work... " + mstring.length());
                return true;

            } catch (JMSException e) {
                throw new RuntimeException(e);
            }
            catch (NoSuchElementException e) {
                return false;
            }
        };
    }

    @NotNull
    public static Predicate<Message> getSlowwork(long delay) {

        return (m) -> {
            try {
                String mstring = getTextMessage(m).orElseThrow().getText();
                System.out.println("slow work message... " + mstring + ", sleeping for " + delay + " milliseconds");

                Thread.sleep(delay);

                System.out.println("slow work done... " + mstring);
                return true;

            } catch (JMSException | InterruptedException e) {
                throw new RuntimeException(e);

            } catch (NoSuchElementException e) {
                return false;
            }
        };
    }

    public static @NotNull String createTextMessage(int msgSizeBytes) {
        if (msgSizeBytes <= 0 || msgSizeBytes > 100 * 1024) {
            throw new IllegalArgumentException("message size out of bounds");
        }

        StringBuilder sb = new StringBuilder(msgSizeBytes);

        for (int i = 0; i < msgSizeBytes; i++) {
            sb.append('a');
        }

        final String buf = sb.toString();
        if (buf.length() != msgSizeBytes) {
            throw new IllegalStateException("failed to create string buffer of " + msgSizeBytes + " bytes");
        }

        return buf;
    }

    public static void displaySendProcessingTime(long elapsedTimeMs, @NotNull ApplicationConfiguration cfg) {
        System.out.println("\n\n\tTotal send processing time " + elapsedTimeMs + " ms");
        System.out.println("\tmessage size: " + cfg.getMessageSize().get() + " bytes");
        System.out.println("\tbatch size: " + cfg.getBatchSizeMessages().get() + " messages");
        System.out.println("\tsend threads: " + cfg.getSendThreads().get());
        System.out.println("\tsend commit count: " + cfg.getSendCommitCount().get());
        System.out.println();
    }

    public static void displayReceiveProcessingTime(long elapsedTimeMs, @NotNull ApplicationConfiguration cfg) {
        System.out.println("\n\n\tTotal receive processing time " + elapsedTimeMs + " ms");
        System.out.println("\tmessage size: " + cfg.getMessageSize().get() + " bytes");
        System.out.println("\tbatch size: " + cfg.getBatchSizeMessages().get() + " messages");
        System.out.println("\treceive threads: " + cfg.getReceiveThreads().get());
        System.out.println("\treceive commit count: " + cfg.getReceiveCommitCount().get());
        System.out.println();
    }

    public static void displayConnectTime(long elapsedTimeMs, @NotNull ApplicationConfiguration cfg) {
        System.out.println("\n\tTotal connect time " + elapsedTimeMs + " ms");
        if (cfg.getSSLCipherSuite().isPresent()) {
            System.out.println("\tSSLCipherSuite " + cfg.getSSLCipherSuite().get());

            if (cfg.getKeystore().isPresent()) {
                System.out.println("\tClient authentication enabled");
            } else {
                System.out.println("\tNo client authentication");
            }

        } else {
            System.out.println("\tNo SSL");
        }
        System.out.println();
    }
}
