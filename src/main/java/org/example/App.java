package org.example;

//import jakarta.jms.Connection;
//import jakarta.jms.JMSException;
//import jakarta.jms.Message;
//import jakarta.jms.Queue;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Queue;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.example.ApplicationWorker.receiveMessages;
import static org.example.ApplicationWorker.sendMessages;

public class App
{
    public static void main(String[] args) throws JMSException, IOException {

        boolean recieveOnly = false;
        boolean sendOnly = false;

        for (String a : args) {
            if (a.equalsIgnoreCase("rcv") || a.equalsIgnoreCase("receive")) {
                recieveOnly = true;

            } else if (a.equalsIgnoreCase("snd") || a.equalsIgnoreCase("send")) {
                sendOnly = true;
            }
        }

        boolean send = !recieveOnly || sendOnly;
        boolean receive = !sendOnly || recieveOnly;

        ApplicationConfiguration appcfg = new ApplicationConfiguration();

        final String propertiesFile = "app.properties";
        System.out.println("Using " + propertiesFile + " configuration file");

        appcfg.loadProperties(propertiesFile);

        QueueConnector qc = new QueueConnector(appcfg);
        Queue q1 = qc.createQueue1();

        Optional<Connection> c = qc.startThreadShareConnection();

        // send messages
        if (send) {
            long sdura = sendMessages(qc, q1, c.orElse(null), appcfg);
        }

        // receive messages
        if (receive) {
            int receiveTimeout = -1;
            long rdura = receiveMessages(qc, q1, c.orElse(null), appcfg, receiveTimeout);
        }

        if (c.isPresent()) {
            c.get().close();
        }
    }
}
