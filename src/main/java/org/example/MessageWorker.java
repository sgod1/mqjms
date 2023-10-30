package org.example;

import jakarta.jms.*;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Predicate;

public class MessageWorker {

    public static Predicate<Message> defaultWork = (Message m) -> {
        if (m instanceof TextMessage) {
            try {
                String mstring = ((TextMessage) m).getText();
                System.out.println("default work... " + mstring);
                return true;

            } catch (JMSException e) {
                throw new RuntimeException(e);
            }
        }
        return false;
    };

    @Contract(pure = true)
    public static @NotNull Runnable receieveOneMessage(QueueConnector qc, Connection c, Queue queue) {
        return receieveOneMessage(qc, c, queue, defaultWork);
    }

    @Contract(pure = true)
    public static @NotNull Runnable receieveOneMessage(QueueConnector qc, Connection c, Queue queue, Predicate<Message> work) {

        return () -> {
            try (Session s = qc.createTransactedSession(c);) {
                Optional<Message> m = qc.receiveOneMessageNoCommit(s, queue);

                if (m.isPresent()) {

                    if (work.test(m.get())) {
                        System.out.println("work committed...");
                        s.commit();
                    }

                } else {
                    System.out.println("no messages...");
                }

            } catch (JMSException e) {
                throw new RuntimeException(e);
            }
        };
    }
}
