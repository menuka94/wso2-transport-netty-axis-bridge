package org.wso2.transports.http.bridge.util;

import java.util.List;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.AddressingConstants;
import org.apache.axis2.addressing.AddressingHelper;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.WSDL2Constants;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.engine.Handler;
import org.apache.axis2.engine.Phase;
import org.apache.axis2.transport.RequestResponseTransport;


public class RelayUtils {

    static boolean earlyBuild = false;
    private static boolean noAddressingHandler = false;
    private static volatile Handler addressingInHandler = null;

    static void processAddressing(MessageContext messageContext,
                                  DeferredMessageBuilder messageBuilder) throws AxisFault {
        if (noAddressingHandler) {
            return;
        } else if (addressingInHandler == null) {
            synchronized (messageBuilder) {
                if (addressingInHandler == null) {
                    AxisConfiguration axisConfig = messageContext.getConfigurationContext()
                            .getAxisConfiguration();
                    List<Phase> phases = axisConfig.getInFlowPhases();
                    boolean handlerFound = false;
                    for (Phase phase : phases) {
                        if ("Addressing".equals(phase.getName())) {
                            List<Handler> handlers = phase.getHandlers();
                            for (Handler handler : handlers) {
                                if ("AddressingInHandler".equals(handler.getName())) {
                                    addressingInHandler = handler;
                                    handlerFound = true;
                                    break;
                                }
                            }
                            break;
                        }
                    }

                    if (!handlerFound) {
                        noAddressingHandler = true;
                        return;
                    }
                }
            }
        }

        messageContext.setProperty(AddressingConstants.DISABLE_ADDRESSING_FOR_IN_MESSAGES, "false");

        Object disableAddressingForOutGoing = null;
        if (messageContext.getProperty(AddressingConstants.DISABLE_ADDRESSING_FOR_OUT_MESSAGES) != null) {
            disableAddressingForOutGoing = messageContext
                    .getProperty(AddressingConstants.DISABLE_ADDRESSING_FOR_OUT_MESSAGES);
        }
        addressingInHandler.invoke(messageContext);

        if (disableAddressingForOutGoing != null) {
            messageContext.setProperty(AddressingConstants.DISABLE_ADDRESSING_FOR_OUT_MESSAGES,
                    disableAddressingForOutGoing);
        }

        if (messageContext.getAxisOperation() == null) {
            return;
        }

        String mepString = messageContext.getAxisOperation().getMessageExchangePattern();

        if (isOneWay(mepString)) {
            Object requestResponseTransport = messageContext
                    .getProperty(RequestResponseTransport.TRANSPORT_CONTROL);
            if (requestResponseTransport != null) {

                Boolean disableAck = getDisableAck(messageContext);
                if (disableAck == null || disableAck.booleanValue() == false) {
                    ((RequestResponseTransport) requestResponseTransport)
                            .acknowledgeMessage(messageContext);
                }
            }
        } else if (AddressingHelper.isReplyRedirected(messageContext)
                && AddressingHelper.isFaultRedirected(messageContext)) {
            if (mepString.equals(WSDL2Constants.MEP_URI_IN_OUT)
                    || mepString.equals(WSDL2Constants.MEP_URI_IN_OUT)
                    || mepString.equals(WSDL2Constants.MEP_URI_IN_OUT)) {
                // OR, if 2 way operation but the response is intended to not
                // use the response channel of a 2-way transport
                // then we don't need to keep the transport waiting.

                Object requestResponseTransport = messageContext
                        .getProperty(RequestResponseTransport.TRANSPORT_CONTROL);
                if (requestResponseTransport != null) {

                    // We should send an early ack to the transport whenever
                    // possible, but some modules need
                    // to use the back channel, so we need to check if they have
                    // disabled this code.
                    Boolean disableAck = getDisableAck(messageContext);

                    if (disableAck == null || disableAck.booleanValue() == false) {
                        ((RequestResponseTransport) requestResponseTransport)
                                .acknowledgeMessage(messageContext);
                    }

                }
            }
        }
    }

    private static boolean isOneWay(String mepString) {
        return (mepString.equals(WSDL2Constants.MEP_URI_IN_ONLY)
                || mepString.equals(WSDL2Constants.MEP_URI_IN_ONLY) || mepString
                .equals(WSDL2Constants.MEP_URI_IN_ONLY));
    }

    private static Boolean getDisableAck(MessageContext msgContext) throws AxisFault {
        // We should send an early ack to the transport whenever possible, but
        // some modules need
        // to use the back channel, so we need to check if they have disabled
        // this code.
        Boolean disableAck = (Boolean) msgContext
                .getProperty(Constants.Configuration.DISABLE_RESPONSE_ACK);
        if (disableAck == null) {
            disableAck = (Boolean) (msgContext.getAxisService() != null ? msgContext
                    .getAxisService().getParameterValue(
                            Constants.Configuration.DISABLE_RESPONSE_ACK) : null);
        }

        return disableAck;
    }
}
