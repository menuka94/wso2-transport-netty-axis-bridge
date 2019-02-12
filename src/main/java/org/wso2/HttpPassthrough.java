package org.wso2;

import org.apache.log4j.BasicConfigurator;
import org.wso2.transport.http.netty.contract.Constants;
import org.wso2.transport.http.netty.contract.HttpWsConnectorFactory;
import org.wso2.transport.http.netty.contract.ServerConnector;
import org.wso2.transport.http.netty.contract.ServerConnectorFuture;
import org.wso2.transport.http.netty.contract.config.ListenerConfiguration;
import org.wso2.transport.http.netty.contract.config.SenderConfiguration;
import org.wso2.transport.http.netty.contract.config.ServerBootstrapConfiguration;
import org.wso2.transport.http.netty.contractimpl.DefaultHttpWsConnectorFactory;

import java.util.HashMap;

import static org.wso2.transport.http.netty.contract.Constants.HTTPS_SCHEME;

public class HttpPassthrough {

    private static final int LISTENER_PORT = 9090;
    private static final String KEYSTORE_PATH = System.getProperty("keystorepath",
            HttpPassthrough.class.getResource("/keystore/wso2carbon.jks").getFile());
    private static final String KEYSTORE_PASS = System.getProperty("keystorepass", "wso2carbon");

    private static final boolean SSL = System.getProperty("ssl") != null;
    private static final float HTTP_VERSION = (Integer.parseInt(System.getProperty("version", "1")) == 1)
            ? Constants.HTTP_1_1 : Constants.HTTP_2_0;
    private static final String SCHEME = SSL ? HTTPS_SCHEME : Constants.HTTP_SCHEME;
    private static final String SERVER_HOST = System.getProperty("host", "localhost");
    private static final int SERVER_PORT = Integer.parseInt(System.getProperty("port", "9191"));
    private static final String SERVER_PATH = System.getProperty("path", "/hello/sayHello");
    private static final String TRUSTSTORE_PATH = System.getProperty("truststorepath",
            HttpPassthrough.class.getResource("/truststore/client-truststore.jks").getFile());
    private static final String TRUSTSTORE_PASS = System.getProperty("truststorepass", "wso2carbon");

    public static void main(String[] args) throws InterruptedException {
        BasicConfigurator.configure();
        HttpWsConnectorFactory factory = new DefaultHttpWsConnectorFactory();

//
//        SenderConfiguration senderConfiguration =
//                HttpUtil.getSenderConfiguration(HTTP_VERSION, SCHEME, TRUSTSTORE_PATH, TRUSTSTORE_PASS);

        ListenerConfiguration listenerConfiguration = new ListenerConfiguration();
        listenerConfiguration.setScheme(SCHEME);
        listenerConfiguration.setVersion(String.valueOf(HTTP_VERSION));
        listenerConfiguration.setPort(LISTENER_PORT);

        if (SCHEME.equals(HTTPS_SCHEME)) {
            listenerConfiguration.setKeyStoreFile(KEYSTORE_PATH);
            listenerConfiguration.setKeyStorePass(KEYSTORE_PASS);
        }

        ServerConnector connector =
                factory.createServerConnector(new ServerBootstrapConfiguration(new HashMap<>()), listenerConfiguration);
        ServerConnectorFuture future = connector.start();

        SenderConfiguration senderConfiguration = new SenderConfiguration();
        senderConfiguration.setScheme(SCHEME);
        senderConfiguration.setHttpVersion(String.valueOf(HTTP_VERSION));
        if (SCHEME.equals(HTTPS_SCHEME)) {
            senderConfiguration.setTrustStoreFile(TRUSTSTORE_PATH);
            senderConfiguration.setTrustStorePass(TRUSTSTORE_PASS);
        }


        future.setHttpConnectorListener(
                new PassthroughMessageProcessor(senderConfiguration, SERVER_HOST, SERVER_PORT, SERVER_PATH));
        future.sync();
    }
}
