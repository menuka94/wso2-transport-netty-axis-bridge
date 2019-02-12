package org.wso2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.transport.http.netty.contract.*;
import org.wso2.transport.http.netty.contract.config.SenderConfiguration;
import org.wso2.transport.http.netty.contractimpl.DefaultHttpWsConnectorFactory;
import org.wso2.transport.http.netty.message.HttpCarbonMessage;

import java.util.HashMap;

public class PassthroughMessageProcessor implements HttpConnectorListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(PassthroughMessageProcessor.class);

    private HttpClientConnector clientConnector;
    private String serverHost;
    private int serverPort;
    private String serverPath;

    public PassthroughMessageProcessor(SenderConfiguration senderConfiguration, String serverHost, int serverPort, String serverPath) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.serverPath = serverPath;
        HttpWsConnectorFactory httpWsConnectorFactory = new DefaultHttpWsConnectorFactory();
        clientConnector = httpWsConnectorFactory.createHttpClientConnector(new HashMap<>(), senderConfiguration);
    }

    @Override
    public void onMessage(HttpCarbonMessage httpRequestMessage) {
        httpRequestMessage.setProperty(Constants.HTTP_HOST, serverHost);
        httpRequestMessage.setProperty(Constants.HTTP_PORT, serverPort);
        httpRequestMessage.setProperty(Constants.TO, serverPath);

        try {
            HttpResponseFuture future = clientConnector.send(httpRequestMessage);
            future.setHttpConnectorListener(new HttpConnectorListener() {
                @Override
                public void onMessage(HttpCarbonMessage httpResponse) {
                    try {
                        httpRequestMessage.respond(httpResponse);
                    } catch (ServerConnectorException e) {
                        LOGGER.error("Error occurred during message notification: {}", e.getMessage());
                    }
                }

                @Override
                public void onError(Throwable throwable) {
                    LOGGER.error("Error occurred during message notification: {}", throwable.getMessage());
                }
            });
        } catch (Exception e) {
            LOGGER.error("Error occurred during message processing: {}", e.getMessage());
        }
    }

    @Override
    public void onError(Throwable throwable) {
        LOGGER.error("Error occurred during message notification: {}", throwable.getMessage());
    }
}
