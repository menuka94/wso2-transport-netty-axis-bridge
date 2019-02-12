package org.wso2;

import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.transport.http.netty.contract.HttpConnectorListener;
import org.wso2.transport.http.netty.message.HttpCarbonMessage;

import java.util.Map;

public class TransportListener implements HttpConnectorListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransportListener.class);

    @Override
    public void onMessage(HttpCarbonMessage httpMessage) {
        HttpHeaders headers = httpMessage.getHeaders();
        HttpContent httpContent = httpMessage.getHttpContent();
        Map<String, Object> properties = httpMessage.getProperties();
    }

    @Override
    public void onError(Throwable throwable) {

    }
}
