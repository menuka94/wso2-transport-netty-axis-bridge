package org.wso2.transports.http.bridge;

import org.apache.axis2.Constants;
import org.apache.axis2.context.MessageContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.channels.Pipe;


/**
 * Class MessageUtils containers helper methods that are used to build the payloads.
 */
public class MessageUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageUtils.class);

    public static void buildMessage(MessageContext msgCtx) {

        final Pipe pipe = (Pipe) msgCtx.getProperty(BridgeConstants.BRIDGE_PIPE);

        if (msgCtx.getProperty(Constants.Configuration.CONTENT_TYPE) != null) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Content Type is " + msgCtx.getProperty(Constants.Configuration.CONTENT_TYPE));
            }
        }

        if (pipe != null && Boolean.TRUE.equals(msgCtx.getProperty(BridgeConstants.MESSAGE_BUILDER_INVOKED))) {
//            InputStream in = pipe.getInputStream();
        }
    }
}
