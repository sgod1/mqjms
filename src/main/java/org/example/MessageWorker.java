package org.example;

//import jakarta.jms.*;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.jms.*;
import java.util.List;
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

    public static Runnable receiveAllMessages(QueueConnector qc, Connection c, Queue queue, int commitCount, Predicate<Message> work) {

        return () -> {
            try (Session s = qc.createTransactedSession(c);) {

                // todo: pass work into receiveAllMessages
                List<Message> messages = qc.receiveAllMessages(s, queue, commitCount);
                messages.forEach(work::test);

//                System.out.println("\t\treceived " + messages.size() + " messages");

            } catch (JMSException e) {
                throw new RuntimeException(e);
            }
        };
    }

    @Contract(pure = true)
    public static @NotNull Runnable sendTextMessages(QueueConnector qc, Connection c, Queue q, List<String> messages, int commitCount) {
        return () -> {
            qc.sendTextMessages(c, q, messages, commitCount);
        };
    }
}
