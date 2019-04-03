package org.wso2.transports.http.bridge.util;

import org.apache.axiom.om.OMOutputFormat;
import org.apache.axis2.Constants;
import org.apache.axis2.context.MessageContext;
import org.wso2.transports.http.bridge.BridgeConstants;

public class TransportUtils {
    public static OMOutputFormat getOMOutputFormat(MessageContext msgContext) {

        OMOutputFormat format = null;
        if (msgContext.getProperty(BridgeConstants.MESSAGE_OUTPUT_FORMAT) != null) {
            format = (OMOutputFormat) msgContext.getProperty(BridgeConstants.MESSAGE_OUTPUT_FORMAT);
        } else {
            format = new OMOutputFormat();
        }

        msgContext.setDoingMTOM(org.apache.axis2.transport.TransportUtils.doWriteMTOM(msgContext));
        msgContext.setDoingSwA(org.apache.axis2.transport.TransportUtils.doWriteSwA(msgContext));
        msgContext.setDoingREST(org.apache.axis2.transport.TransportUtils.isDoingREST(msgContext));

        /**
         *  BridgeConstants.INVOKED_REST set to true here if isDoingREST is true -
         *  this enables us to check whether the original request to the endpoint was a
         * REST request inside DefferedMessageBuilder (which we need to convert
         * text/xml content type into application/xml if the request was not a SOAP
         * request.
         */
        if (msgContext.isDoingREST()) {
            msgContext.setProperty(BridgeConstants.INVOKED_REST, true);
        }
        format.setSOAP11(msgContext.isSOAP11());
        format.setDoOptimize(msgContext.isDoingMTOM());
        format.setDoingSWA(msgContext.isDoingSwA());

        format.setCharSetEncoding(org.apache.axis2.transport.TransportUtils.getCharSetEncoding(msgContext));
        Object mimeBoundaryProperty = msgContext.getProperty(Constants.Configuration.MIME_BOUNDARY);
        if (mimeBoundaryProperty != null) {
            format.setMimeBoundary((String) mimeBoundaryProperty);
        }

        return format;
    }
}
