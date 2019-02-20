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
package org.wso2.transports.http.bridge;

/**
 * {@code BridgeConstants} contains the constants related to netty axis2 bridge.
 */
public class BridgeConstants {

    public static final String REMOTE_HOST = "REMOTE_HOST";
    /* Thread pool related constants */
    public static final int DEFAULT_WORKER_POOL_SIZE_CORE = 400;
    public static final int DEFAULT_WORKER_POOL_SIZE_MAX = 500;
    public static final int DEFAULT_WORKER_THREAD_KEEPALIVE_SEC = 60;
    public static final int DEFAULT_WORKER_POOL_QUEUE_LENGTH = -1;
    public static final String HTTP_WORKER_THREAD_GROUP_NAME = "HTTP Worker Thread Group";
    public static final String HTTP_WORKER_THREAD_ID = "HTTPWorker";

    public static final String HTTP_METHOD = "HTTP_METHOD";
    public static final String HTTP_STATUS_CODE = "HTTP_STATUS_CODE";
    public static final String HTTP_REASON_PHRASE = "HTTP_REASON_PHRASE";
    public static final String CONTENT_TYPE_HEADER = "Content-Type";
    public static final String SOAP_ACTION_HEADER = "SOAPAction";

    public static final String HTTP_STATUS_CODE_PROP = "HTTP_SC";
    public static final String HTTP_STATUS_CODE_DESCRIPTION_PROP = "HTTP_SC_DESC";

    public static final String REST_URL_POSTFIX = "REST_URL_POSTFIX";
    public static final String SERVICE_PREFIX = "SERVICE_PREFIX";

    public static final String REST_REQUEST_CONTENT_TYPE = "synapse.internal.rest.contentType";
    public static final String HTTP_CARBON_MESSAGE = "HTTP_CARBON_MESSAGE";
    public static final String HTTP_CLIENT_REQUEST_CARBON_MESSAGE = "HTTP_CLIENT_REQUEST_CARBON_MESSAGE";

}
