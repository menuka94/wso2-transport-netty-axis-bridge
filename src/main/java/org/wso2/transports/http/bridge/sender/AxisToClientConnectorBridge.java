/*
 *  Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.wso2.transports.http.bridge.sender;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.axis2.handlers.AbstractHandler;
import org.apache.axis2.transport.TransportSender;
import org.apache.axis2.transport.base.threads.WorkerPool;
import org.apache.axis2.transport.base.threads.WorkerPoolFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.transport.http.netty.contract.Constants;
import org.wso2.transport.http.netty.contract.HttpClientConnector;
import org.wso2.transport.http.netty.contract.HttpResponseFuture;
import org.wso2.transport.http.netty.contract.HttpWsConnectorFactory;
import org.wso2.transport.http.netty.contract.ServerConnectorException;
import org.wso2.transport.http.netty.contract.config.SenderConfiguration;
import org.wso2.transport.http.netty.contractimpl.DefaultHttpWsConnectorFactory;
import org.wso2.transport.http.netty.contractimpl.sender.channel.pool.ConnectionManager;
import org.wso2.transport.http.netty.message.HttpCarbonMessage;
import org.wso2.transport.http.netty.message.HttpCarbonRequest;
import org.wso2.transports.http.bridge.BridgeConstants;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

/**
 * {@code AxisToClientConnectorBridge} receives the outgoing axis2 {@code MessageContext}, convert it into a
 * {@code HttpCarbonMessage} and deliver it to the Http Client connector.
 */
public class AxisToClientConnectorBridge extends AbstractHandler implements TransportSender {

    private static final Logger LOG = LoggerFactory.getLogger(AxisToClientConnectorBridge.class);
    private HttpClientConnector clientConnector;

    private WorkerPool workerPool;

    @Override
    public void init(ConfigurationContext configurationContext, TransportOutDescription transportOutDescription) {
        HttpWsConnectorFactory httpWsConnectorFactory = new DefaultHttpWsConnectorFactory();
        SenderConfiguration senderConfiguration = new SenderConfiguration();
        ConnectionManager connectionManager = new ConnectionManager(senderConfiguration.getPoolConfiguration());
        clientConnector = httpWsConnectorFactory
                .createHttpClientConnector(new HashMap<>(), senderConfiguration, connectionManager);
        workerPool = WorkerPoolFactory.getWorkerPool(BridgeConstants.DEFAULT_WORKER_POOL_SIZE_CORE,
                BridgeConstants.DEFAULT_WORKER_POOL_SIZE_MAX,
                BridgeConstants.DEFAULT_WORKER_THREAD_KEEPALIVE_SEC,
                BridgeConstants.DEFAULT_WORKER_POOL_QUEUE_LENGTH,
                BridgeConstants.HTTP_WORKER_THREAD_GROUP_NAME,
                BridgeConstants.HTTP_WORKER_THREAD_ID);
    }

    @Override
    public InvocationResponse invoke(MessageContext msgCtx) throws AxisFault {

        HttpCarbonMessage httpCarbonMessage =
                (HttpCarbonMessage) msgCtx.getProperty(BridgeConstants.HTTP_CARBON_MESSAGE);

        if (httpCarbonMessage == null) {
            LOG.info("Carbon Message not found, " +
                    "sending " +
                    "requests originated from non HTTP transport is not supported yet");
            return InvocationResponse.ABORT;
        }

        URL url = getDestinationURL(msgCtx);
        if (url != null) {  // Outgoing request
            sendForward(msgCtx, httpCarbonMessage, url);
        } else { // Response submission back to the client
            sendBack(msgCtx, httpCarbonMessage);
        }
        return InvocationResponse.CONTINUE;
    }

    private void sendBack(MessageContext msgCtx, HttpCarbonMessage httpCarbonMessage) throws AxisFault {
        HttpCarbonRequest clientRequest =
                (HttpCarbonRequest) msgCtx.getProperty(BridgeConstants.HTTP_CLIENT_REQUEST_CARBON_MESSAGE);

        if (clientRequest == null) {
            throw new AxisFault("Original client request not found");
        }
        try {
            clientRequest.respond(httpCarbonMessage);
        } catch (ServerConnectorException e) {
            LOG.error("Error occurred while submitting the response back to the client", e);
        }
    }

    private void sendForward(MessageContext msgCtx, HttpCarbonMessage httpCarbonMessage, URL url) {
        int port = getOutboundReqPort(url);
        String host = url.getHost();
        setOutboundReqHeaders(httpCarbonMessage, port, host);
        setOutboundReqProperties(httpCarbonMessage, url, port, host);
        HttpResponseFuture future = clientConnector.send(httpCarbonMessage);
        future.setHttpConnectorListener(new ResponseListener(workerPool, msgCtx));
    }

    @Override
    public void cleanup(MessageContext messageContext) {
    }

    @Override
    public void stop() {
    }

    private URL getDestinationURL(MessageContext msgContext) throws AxisFault {

        String transportURL = (String) msgContext.getProperty(
                org.apache.axis2.Constants.Configuration.TRANSPORT_URL);

        EndpointReference endpointReference;
        if (transportURL != null) {
            endpointReference = new EndpointReference(transportURL);
        } else if (
                (msgContext.getTo() != null) && !msgContext.getTo().hasAnonymousAddress()) {
            endpointReference = msgContext.getTo();
        } else {
            return null;
        }

        try {
            return new URL(endpointReference.getAddress());
        } catch (MalformedURLException e) {
            throw new AxisFault("Malformed Endpoint url found", e);
        }
    }

    private void setOutboundReqHeaders(HttpCarbonMessage outboundRequest, int port, String host) {
        HttpHeaders headers = outboundRequest.getHeaders();
        setHostHeader(host, port, headers);
    }

    private void setOutboundReqProperties(HttpCarbonMessage outboundRequest, URL url, int port, String host) {
        outboundRequest.setProperty(Constants.HTTP_HOST, host);
        outboundRequest.setProperty(Constants.HTTP_PORT, port);
        String outboundReqPath = getOutboundReqPath(url);
        outboundRequest.setProperty(Constants.TO, outboundReqPath);
        outboundRequest.setProperty(Constants.PROTOCOL, url.getProtocol());
    }

    private void setHostHeader(String host, int port, HttpHeaders headers) {
        if (port == 80 || port == 443) {
            headers.set(HttpHeaderNames.HOST, host);
        } else {
            headers.set(HttpHeaderNames.HOST, host + ":" + port);
        }
    }

    private String getOutboundReqPath(URL url) {
        String toPath = url.getPath();
        String query = url.getQuery();
        if (query != null) {
            toPath = toPath + "?" + query;
        }
        return toPath;
    }

    private int getOutboundReqPort(URL url) {
        int port = 80;
        if (url.getPort() != -1) {
            port = url.getPort();
        } else if (url.getProtocol().equalsIgnoreCase(Constants.HTTPS_SCHEME)) {
            port = 443;
        }
        return port;
    }
}
