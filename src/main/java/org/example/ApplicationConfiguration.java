package org.example;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.Properties;

public class ApplicationConfiguration {

    public static final String MQ_QMGR = "MQ_QMGR";
    public static final String MQ_QMGR_HOST = "MQ_QMGR_HOST";
    public static final String MQ_QMGR_PORT = "MQ_QMGR_PORT";
    public static final String MQ_SRV_CHANNEL = "MQ_SRV_CHANNEL";
    public static final String MQ_APP_NAME = "MQ_APP_NAME";
    public static final String MQ_USER_NAME = "MQ_USER_NAME";
    public static final String MQ_USER_PASSWORD = "MQ_USER_PASSWORD";
    public static final String MQ_QUEUE_NAME = "MQ_QUEUE_NAME";

    public static final String MESSAGE_SIZE_BYTES = "MESSAGE_SIZE_BYTES";    // message size in bytes
    public static final String BATCH_SIZE_MESSAGES = "BATCH_SIZE_MESSAGES";    // number of messages in a batch
    public static final String SEND_THREADS = "SEND_THREADS"; // number of send threads
    public static final String RECEIVE_THREADS = "RECEIVE_THREADS"; // number of receive threads
    public static final String SEND_RATE_MPS = "SEND_RATE_MPS"; // send rate messages per second

    private final Properties properties;

    public ApplicationConfiguration() {
        this.properties = new Properties();
    }

    public void loadProperties(String file) throws IOException {
        try (FileInputStream is = new FileInputStream(file);) {
            this.properties.load(is);
        }
    }

    private Optional<Integer> getIntegerProperty(String key) {
        String value = this.properties.getProperty(key);
        if (value == null) {
            return Optional.empty();
        }

        try {
            Integer ival = Integer.valueOf(value);
            return Optional.of(ival);
        }
        catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid "+ key + " property value " + value);
        }
    }

    private Optional<String> getStringProperty(String key) {
        String value = this.properties.getProperty(key);
        return Optional.ofNullable(value);
    }

    public Optional<String> getQueueManager() {
        return getStringProperty(MQ_QMGR);
    }

    public Optional<String> getQueueManagerHost() {
        return getStringProperty(MQ_QMGR_HOST);
    }

    public Optional<Integer> getQueueManagerPort() {
        return getIntegerProperty(MQ_QMGR_PORT);
    }

    public Optional<String> getServerChannel() {
        return getStringProperty(MQ_SRV_CHANNEL);
    }

    public Optional<String> getMQUserName() {
        return getStringProperty(MQ_USER_NAME);
    }

    public Optional<String> getMQUserPassword() {
        return getStringProperty(MQ_USER_PASSWORD);
    }

    public Optional<String> getMQApplicationName() {
        return getStringProperty(MQ_APP_NAME);
    }

    public Optional<String> getQueueName() {
        return getStringProperty(MQ_QUEUE_NAME);
    }

    public Optional<Integer> getMessageSize() {
        return getIntegerProperty(MESSAGE_SIZE_BYTES);
    }

    public Optional<Integer> getBatchSizeMessages() {
        return getIntegerProperty(BATCH_SIZE_MESSAGES);
    }

    public Optional<Integer> getSendThreads() {
        return getIntegerProperty(SEND_THREADS);
    }

    public Optional<Integer> getReceiveThreads() {
        return getIntegerProperty(RECEIVE_THREADS);
    }

    public Optional<Integer> getSendRateMps() {
        return getIntegerProperty(SEND_RATE_MPS);
    }
}
