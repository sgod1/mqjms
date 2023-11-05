package org.example;

//import jakarta.jms.Connection;
//import jakarta.jms.JMSException;
//import jakarta.jms.Message;
//import jakarta.jms.Queue;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Queue;
import java.io.IOException;

import static org.example.ApplicationWorker.receiveMessages;
import static org.example.ApplicationWorker.sendMessages;

public class App
{
    public static void main(String[] args) throws JMSException, IOException {

        ApplicationConfiguration appcfg = new ApplicationConfiguration();

        final String propertiesFile = "app.properties";
        System.out.println("Using " + propertiesFile + " configuration file");

        appcfg.loadProperties(propertiesFile);

        QueueConnector qc = new QueueConnector(appcfg);
        Queue q1 = qc.createQueue1();

        Connection c = null;
        if (qc.getThreadsShareConnection()) {
            System.out.println("\tOne connection for all threads...\n");

            c = qc.startConnection();

        } else {
            System.out.println("\nOne connection per thread...\n");
        }

        // send messages
        long sdura = sendMessages(qc, q1, c, appcfg);

        // receive messages
        long rdura = receiveMessages(qc, q1, c, appcfg);

        if (c != null) {
            c.close();
        }
    }
}
