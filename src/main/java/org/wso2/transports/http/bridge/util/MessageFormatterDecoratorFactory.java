package org.wso2.transports.http.bridge.util;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.transport.MessageFormatter;
import org.apache.axis2.util.MessageProcessorSelector;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Factory for getting Decorator to Message Formatter . This decorator is needed when extending
 * message formatter  by adding decoration functionality
 */
public class MessageFormatterDecoratorFactory {
    private static final Log log = LogFactory.getLog(MessageFormatterDecoratorFactory.class);

    private static final String GZIP_CODEC = "gzip";

    public static MessageFormatter createMessageFormatterDecorator(MessageContext msgContext) {

        if (msgContext == null) {
            throw new IllegalArgumentException("Message Context cannot be null");
        }

        try {
            // Get message formatter based on the content type
            MessageFormatter formatter = MessageProcessorSelector.getMessageFormatter(msgContext);

            return formatter;

        } catch (AxisFault axisFault) {
            String msg = "Cannot find a suitable MessageFormatter : " + axisFault.getMessage();
            log.error(msg, axisFault);
        }

        return null;

    }
}
