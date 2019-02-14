package org.wso2.transports.http.bridge.sender;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.builder.BuilderUtil;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.WSDL2Constants;
import org.apache.axis2.engine.AxisEngine;
import org.apache.axis2.wsdl.WSDLConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.transport.http.netty.contract.HttpConnectorListener;
import org.wso2.transport.http.netty.message.HttpCarbonMessage;
import org.wso2.transports.http.bridge.BridgeConstants;

import java.util.Map;
import java.util.TreeMap;


/**
 * {@code ResponseProcessor} handles the response of the
 */
public class ResponseProcessor implements HttpConnectorListener {

    private static final Logger LOG = LoggerFactory.getLogger(ResponseProcessor.class);

    private MessageContext requestMsgCtx;
    private MessageContext responseMsgCtx;

    public ResponseProcessor(MessageContext requestMsgContext) {
        this.requestMsgCtx = requestMsgContext;
        try {
            responseMsgCtx = requestMsgCtx.getOperationContext().
                    getMessageContext(WSDL2Constants.MESSAGE_LABEL_IN);
        } catch (AxisFault af) {
            LOG.error("Error getting IN message context from the operation context", af);
            return;
        }

        responseMsgCtx.setServerSide(true);
        responseMsgCtx.setDoingREST(requestMsgCtx.isDoingREST());
        responseMsgCtx.setProperty(MessageContext.TRANSPORT_IN, requestMsgCtx
                .getProperty(MessageContext.TRANSPORT_IN));
        responseMsgCtx.setTransportIn(requestMsgCtx.getTransportIn());
        responseMsgCtx.setTransportOut(requestMsgCtx.getTransportOut());
        responseMsgCtx.setAxisMessage(requestMsgCtx.getOperationContext().getAxisOperation().
                getMessage(WSDLConstants.MESSAGE_LABEL_IN_VALUE));
        responseMsgCtx.setOperationContext(requestMsgCtx.getOperationContext());
        responseMsgCtx.setConfigurationContext(requestMsgCtx.getConfigurationContext());
        responseMsgCtx.setTo(null);
    }

    @Override
    public void onMessage(HttpCarbonMessage httpResponse) {
        LOG.info("Response received...!");

        // http transport header names are case insensitive
        Map<String, String> headers = new TreeMap<String, String>((o1, o2) -> o1.compareToIgnoreCase(o2));
        httpResponse.getHeaders().forEach(entry -> headers.put(entry.getKey(), entry.getValue()));
        responseMsgCtx.setProperty(MessageContext.TRANSPORT_HEADERS, headers);
        // Set the original incoming carbon message as a property
        responseMsgCtx.setProperty(BridgeConstants.HTTP_CARBON_MESSAGE, httpResponse);
        responseMsgCtx.setProperty(BridgeConstants.HTTP_CLIENT_REQUEST_CARBON_MESSAGE,
                requestMsgCtx.getProperty(BridgeConstants.HTTP_CLIENT_REQUEST_CARBON_MESSAGE));
        String contentType = httpResponse.getHeader(BridgeConstants.CONTENT_TYPE_HEADER);
        responseMsgCtx.setProperty(Constants.Configuration.CONTENT_TYPE, contentType);

        String charSetEnc = BuilderUtil.getCharSetEncoding(contentType);
        if (charSetEnc == null) {
            charSetEnc = MessageContext.DEFAULT_CHAR_SET_ENCODING;
        }
        if (contentType != null) {
            responseMsgCtx.setProperty(
                    Constants.Configuration.CHARACTER_SET_ENCODING,
                    contentType.indexOf("charset") >= 1 ?
                            charSetEnc : MessageContext.DEFAULT_CHAR_SET_ENCODING);
        }

        SOAPFactory fac = OMAbstractFactory.getSOAP11Factory();
        SOAPEnvelope envelope = fac.getDefaultEnvelope();
        try {
            responseMsgCtx.setEnvelope(envelope);
        } catch (AxisFault axisFault) {
            LOG.error("Error setting SOAP envelope", axisFault);
        }
        responseMsgCtx.setServerSide(true);

        int statusCode = (int) httpResponse.getProperty(BridgeConstants.HTTP_STATUS_CODE);
        responseMsgCtx.setProperty(BridgeConstants.HTTP_STATUS_CODE_PROP, statusCode);
        responseMsgCtx.setProperty(BridgeConstants.HTTP_STATUS_CODE_DESCRIPTION_PROP,
                httpResponse.getProperty(BridgeConstants.HTTP_REASON_PHRASE));

        // process response received
        try {
            AxisEngine.receive(responseMsgCtx);
        } catch (AxisFault af) {
            LOG.error("Fault processing response message through Axis2", af);
        }
    }

    @Override
    public void onError(Throwable throwable) {
        LOG.error("Error while processing the response", throwable);
    }

}
