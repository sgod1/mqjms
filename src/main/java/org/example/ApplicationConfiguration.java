package org.example;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.Properties;

public class ApplicationConfiguration {

    // mq
    public static final String MQ_QMGR = "MQ_QMGR";
    public static final String MQ_QMGR_HOST = "MQ_QMGR_HOST";
    public static final String MQ_QMGR_PORT = "MQ_QMGR_PORT";
    public static final String MQ_SRV_CHANNEL = "MQ_SRV_CHANNEL";
    public static final String MQ_APP_NAME = "MQ_APP_NAME";
    public static final String MQ_USER_NAME = "MQ_USER_NAME";
    public static final String MQ_USER_PASSWORD = "MQ_USER_PASSWORD";
    public static final String MQ_QUEUE_NAME = "MQ_QUEUE_NAME";
    public static final String MQ_CCDT_URL = "MQ_CCDT_URL";
    public static final String MQ_SSL_CIPHER_SUITE = "MQ_SSL_CIPHER_SUITE";

    // ssl
    public static final String javax_net_ssl_keyStore = "JAVAX_NET_SSL_KEY_STORE";
    public static final String javax_net_ssl_keyStorePassword = "JAVAX_NET_SSL_KEY_STORE_PASSWORD";
    public static final String javax_net_ssl_trustStore = "JAVAX_NET_SSL_TRUST_STORE";
    public static final String javax_net_ssl_trustStorePassword = "JAVAX_NET_SSL_TRUST_STORE_PASSWORD";

    // perf
    public static final String MESSAGE_SIZE_BYTES = "MESSAGE_SIZE_BYTES";    // message size in bytes
    public static final String BATCH_SIZE_MESSAGES = "BATCH_SIZE_MESSAGES";    // number of messages in a batch
    public static final String SEND_THREADS = "SEND_THREADS"; // number of send threads
    public static final String RECEIVE_THREADS = "RECEIVE_THREADS"; // number of receive threads
    public static final String SEND_RATE_MPS = "SEND_RATE_MPS"; // send rate messages per second

    public static final String SEND_COMMIT_COUNT = "SEND_COMMIT_COUNT"; // send commit message count
    public static final String RECEIVE_COMMIT_COUNT = "RECEIVE_COMMIT_COUNT"; // send commit message count

    public static final String THREADS_SHARE_CONNECTION = "THREADS_SHARE_CONNECTION"; // share connection between threads

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

    public Optional<Integer> getSendCommitCount() {
        return getIntegerProperty(SEND_COMMIT_COUNT);
    }

    public Optional<Integer> getReceiveCommitCount() {
        return getIntegerProperty(RECEIVE_COMMIT_COUNT);
    }

    public Optional<String> getCCDTUrl() { return getStringProperty(MQ_CCDT_URL);}

    public Optional<String> getSSLCipherSuite() {
        return getStringProperty(MQ_SSL_CIPHER_SUITE);
    }

    public Optional<String> getTruststore() {
        return this.getStringProperty(javax_net_ssl_trustStore);
    }

    public Optional<String> getTruststorePassword() {
        return this.getStringProperty(javax_net_ssl_trustStorePassword);
    }

    public Optional<String> getKeystore() {
        return this.getStringProperty(javax_net_ssl_keyStore);
    }

    public Optional<String> getKeystorePassword() {
        return this.getStringProperty(javax_net_ssl_keyStorePassword);
    }

    public Optional<String> getThreadsShareConnection() {
        return this.getStringProperty(THREADS_SHARE_CONNECTION);
    }
}
