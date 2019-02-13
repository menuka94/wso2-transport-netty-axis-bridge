/**
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.transports.http.bridge;

import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.SessionContext;
import org.apache.axis2.description.TransportInDescription;
import org.apache.axis2.transport.TransportListener;
import org.apache.axis2.transport.base.threads.WorkerPool;
import org.apache.axis2.transport.base.threads.WorkerPoolFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.transport.http.netty.contract.HttpWsConnectorFactory;
import org.wso2.transport.http.netty.contract.ServerConnector;
import org.wso2.transport.http.netty.contract.ServerConnectorFuture;
import org.wso2.transport.http.netty.contract.config.ListenerConfiguration;
import org.wso2.transport.http.netty.contract.config.ServerBootstrapConfiguration;
import org.wso2.transport.http.netty.contractimpl.DefaultHttpWsConnectorFactory;

import java.util.HashMap;

/**
 * {@code Axis2HttpTransportListener} is the Axis2 Transport Listener implementation for HTTP transport.
 *
 */
public class Axis2HttpTransportListener implements TransportListener {

    private HttpWsConnectorFactory httpWsConnectorFactory;
    private ServerConnector serverConnector;
    private int port = 8280;
    private ConfigurationContext configurationContext;

    /** The thread pool for executing the messages passing through */
    private WorkerPool workerPool = null;

    private static final Logger LOG = LoggerFactory.getLogger(Axis2HttpTransportListener.class);

    @Override
    public void init(ConfigurationContext configurationContext, TransportInDescription transportInDescription)
            throws AxisFault {

        this.configurationContext = configurationContext;

        workerPool = WorkerPoolFactory.getWorkerPool(BridgeConstants.DEFAULT_WORKER_POOL_SIZE_CORE,
                BridgeConstants.DEFAULT_WORKER_POOL_SIZE_MAX,
                BridgeConstants.DEFAULT_WORKER_THREAD_KEEPALIVE_SEC,
                BridgeConstants.DEFAULT_WORKER_POOL_QUEUE_LENGTH,
                BridgeConstants.HTTP_WORKER_THREAD_GROUP,
                BridgeConstants.HTTP_WORKER_THREAD_ID);

        httpWsConnectorFactory = new DefaultHttpWsConnectorFactory();

        ListenerConfiguration listenerConfiguration = new ListenerConfiguration();
        listenerConfiguration.setPort(port);
        listenerConfiguration.setHost("localhost");
        serverConnector = httpWsConnectorFactory
                .createServerConnector(new ServerBootstrapConfiguration(new HashMap<>()), listenerConfiguration);
        ServerConnectorFuture serverConnectorFuture = serverConnector.start();
        serverConnectorFuture.setHttpConnectorListener(
                new ConnectorListenerToAxisBridge(configurationContext, workerPool));
        try {
            serverConnectorFuture.sync();
        } catch (InterruptedException e) {
            LOG.warn("Interrupted while waiting for server connector to start", e);
        }

    }

    @Override
    public void start() throws AxisFault {

    }

    @Override
    public void stop() throws AxisFault {

    }

    @Override
    public EndpointReference getEPRForService(String s, String s1) throws AxisFault {
        return null;
    }

    @Override
    public EndpointReference[] getEPRsForService(String s, String s1) throws AxisFault {
        return new EndpointReference[0];
    }

    @Override
    public SessionContext getSessionContext(MessageContext messageContext) {
        return null;
    }

    @Override
    public void destroy() {

    }
}
