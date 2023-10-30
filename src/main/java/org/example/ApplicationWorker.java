package org.example;

import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import org.jetbrains.annotations.NotNull;

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
                System.out.println("fast work... " + mstring);
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
}
