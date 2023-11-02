package org.example;

//import jakarta.jms.JMSException;
//import jakarta.jms.Message;
//import jakarta.jms.TextMessage;

import org.jetbrains.annotations.NotNull;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Predicate;

public class ApplicationWorker {

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

    public static void displayConnectTime(long elapsedTimeMs, ApplicationConfiguration cfg) {
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
