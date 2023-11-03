package org.example;

//import jakarta.jms.Connection;
//import jakarta.jms.JMSException;
//import jakarta.jms.Message;
//import jakarta.jms.Queue;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Queue;
import java.io.IOException;
import java.util.Date;

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

        // send messages
        long sdura = sendMessages(qc, q1, c, appcfg);

        // receive messages
        long rdura = receiveMessages(qc, q1, c, appcfg);

        c.stop();
        c.close();
    }
}
