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
package org.wso2.transports.http.bridge.listener;

import org.apache.axiom.soap.SOAP11Constants;
import org.apache.axiom.soap.SOAP12Constants;
import org.apache.axiom.util.UIDGenerator;
import org.apache.axis2.Constants;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.transport.TransportUtils;
import org.wso2.transport.http.netty.message.HttpCarbonMessage;
import org.wso2.transports.http.bridge.BridgeConstants;

import java.util.Map;
import java.util.TreeMap;

/**
 * {@code RequestUtils} contains utilities which is used in request message flow.
 */
public class RequestUtils {

    static MessageContext convertCarbonMsgToAxis2MsgCtx(ConfigurationContext axis2ConfigurationCtx,
                                                        HttpCarbonMessage incomingCarbonMsg) {
        MessageContext msgCtx = new MessageContext();
        msgCtx.setMessageID(UIDGenerator.generateURNString());
        msgCtx.setProperty(MessageContext.CLIENT_API_NON_BLOCKING,
                Boolean.FALSE);
        msgCtx.setConfigurationContext(axis2ConfigurationCtx);

        msgCtx.setTransportOut(axis2ConfigurationCtx.getAxisConfiguration()
                .getTransportOut(Constants.TRANSPORT_HTTP));
        msgCtx.setTransportIn(axis2ConfigurationCtx.getAxisConfiguration()
                .getTransportIn(Constants.TRANSPORT_HTTP));
        msgCtx.setIncomingTransportName(Constants.TRANSPORT_HTTP);

        msgCtx.setServerSide(true);
        msgCtx.setProperty(Constants.Configuration.TRANSPORT_IN_URL,
                incomingCarbonMsg.getProperty(org.wso2.transport.http.netty.contract.Constants.REQUEST_URL));
        msgCtx.setProperty(MessageContext.REMOTE_ADDR,
                incomingCarbonMsg.getProperty(org.wso2.transport.http.netty.contract.Constants.REMOTE_ADDRESS));
        msgCtx.setProperty(BridgeConstants.REMOTE_HOST,
                incomingCarbonMsg.getProperty(org.wso2.transport.http.netty.contract.Constants.ORIGIN_HOST));

        // http transport header names are case insensitive
        Map<String, String> headers = new TreeMap<>(String::compareToIgnoreCase);
        incomingCarbonMsg.getHeaders().forEach(entry -> headers.put(entry.getKey(), entry.getValue()));
        msgCtx.setProperty(MessageContext.TRANSPORT_HEADERS, headers);
        // Set the original incoming carbon message as a property
        msgCtx.setProperty(BridgeConstants.HTTP_CARBON_MESSAGE, incomingCarbonMsg);
        msgCtx.setProperty(BridgeConstants.HTTP_CLIENT_REQUEST_CARBON_MESSAGE, incomingCarbonMsg);
        return msgCtx;
    }

    static String getRestUrlPostfix(String uri, String servicePath) {

        String contextServicePath = "/" + servicePath;
        if (uri.startsWith(contextServicePath)) {
            // discard upto servicePath
            uri = uri.substring(uri.indexOf(contextServicePath) +
                    contextServicePath.length());
            // discard [proxy] service name if any
            int pos = uri.indexOf("/", 1);
            if (pos > 0) {
                uri = uri.substring(pos);
            } else {
                pos = uri.indexOf("?");
                if (pos != -1) {
                    uri = uri.substring(pos);
                } else {
                    uri = "";
                }
            }
        } else {
            // remove any absolute prefix if any
            int pos = uri.indexOf("://");
            //compute index of beginning of Query Parameter
            int indexOfQueryStart = uri.indexOf("?");

            //Check if there exist a absolute prefix '://' and it is before query parameters
            //To allow query parameters with URLs. ex: /test?a=http://asddd
            if (pos != -1 && ((indexOfQueryStart == -1 || pos < indexOfQueryStart))) {
                uri = uri.substring(pos + 3);
            }
            pos = uri.indexOf("/");
            if (pos != -1) {
                uri = uri.substring(pos + 1);
            }
            // Remove the service prefix
            if (uri.startsWith(servicePath)) {
                // discard upto servicePath
                uri = uri.substring(uri.indexOf(contextServicePath)
                        + contextServicePath.length());
                // discard [proxy] service name if any
                pos = uri.indexOf("/", 1);
                if (pos > 0) {
                    uri = uri.substring(pos);
                } else {
                    pos = uri.indexOf("?");
                    if (pos != -1) {
                        uri = uri.substring(pos);
                    } else {
                        uri = "";
                    }
                }
            }
        }

        return uri;
    }

    static boolean isRESTRequest(String contentType) {
        return contentType != null &&
                (contentType.contains("application/xml") ||
                        contentType.contains("application/x-www-form-urlencoded") ||
                        contentType.contains("multipart/form-data") ||
                        contentType.contains("application/json") ||
                        contentType.contains("application/jwt"));
    }

    static boolean isRest(String contentType) {
        return contentType != null &&
                !contentType.contains(SOAP11Constants.SOAP_11_CONTENT_TYPE) &&
                !contentType.contains(SOAP12Constants.SOAP_12_CONTENT_TYPE);
    }

    static int populateSOAPVersion(MessageContext msgContext, String soapActionHeader, String contentType) {
        int soapVersion = 0;
        if (contentType != null) {
            if (contentType.contains("application/soap+xml")) {
                soapVersion = 2;
                TransportUtils.processContentTypeForAction(contentType, msgContext);
            } else if (contentType.indexOf("text/xml") > -1) {
                soapVersion = 1;
            } else if (isRESTRequest(contentType)) {
                soapVersion = 1;
                msgContext.setDoingREST(true);
            }

            if (soapVersion == 1) {
                Parameter disableREST = msgContext.getParameter("disableREST");
                if (soapActionHeader == null && disableREST != null && "false".equals(disableREST.getValue())) {
                    msgContext.setDoingREST(true);
                }
            }
        }
        return soapVersion;
    }

}
