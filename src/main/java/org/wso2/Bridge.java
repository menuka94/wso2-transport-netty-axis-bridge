package org.wso2;

import io.netty.handler.codec.http.HttpHeaders;
import org.wso2.transport.http.netty.contract.HttpConnectorListener;
import org.wso2.transport.http.netty.message.HttpCarbonMessage;

public class Bridge implements HttpConnectorListener {
    public void onMessage(HttpCarbonMessage httpCarbonMessage) {

    }

    public void onError(Throwable throwable) {

    }
}
