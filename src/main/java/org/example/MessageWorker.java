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
            Connection c1 = null;
            try {
                c1 = (c == null) ? qc.startConnection() : c;

                try (Session s = qc.createTransactedSession(c1);) {
                    Optional<Message> m = qc.receiveOneMessageNoCommit(s, queue);

                    if (m.isPresent()) {

                        if (work.test(m.get())) {
                            System.out.println("work committed...");
                            s.commit();
                        }

                    }
                }

            } catch (JMSException e) {
                throw new RuntimeException(e);

            } finally {
                if (c == null && c1 != null) {
                    try {
                        c1.close();
                    } catch (JMSException ignored) {
                    }
                }
            }
        };
    }

    public static Runnable receiveAllMessages(QueueConnector qc, Connection c, Queue queue, int commitCount, Predicate<Message> work, int timeout) {

        return () -> {
            Connection c1 = null;
            try {
                c1 = (c == null) ? qc.startConnection() : c;

                try (Session s = qc.createTransactedSession(c1);) {

                    List<Message> messages = qc.receiveAllMessages(s, queue, commitCount, work, timeout);
                    messages.forEach(work::test);

                } catch (JMSException e) {
                    throw new RuntimeException(e);
                }

            } catch (JMSException e) {
                throw new RuntimeException(e);

            } finally {
                if (c == null && c1 != null) {
                    try {
                        c1.close();
                    } catch (JMSException ignored) {
                    }
                }
            }
        };
    }

    @Contract(pure = true)
    public static @NotNull Runnable sendTextMessages(QueueConnector qc, Connection c, Queue q, List<String> messages, int commitCount, int mps) {
        return () -> {
            Connection c1 = null;

            try {
                c1 = (c == null) ? qc.startConnection() : c;

                try (Session s = qc.createTransactedSession(c1);) {
                    qc.sendTextMessages(s, q, messages, commitCount, mps);
                }

            } catch (JMSException e) {
                throw new RuntimeException(e);

            } finally {
                if (c == null && c1 != null) {
                    try {
                        c1.close();
                    } catch (JMSException ignored) {
                    }
                }
            }

        };
    }
}
