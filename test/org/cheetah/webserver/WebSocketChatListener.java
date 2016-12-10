package org.cheetah.webserver;

import org.cheetah.webserver.websocket.defaults.WebSocketDefaultListener;
import org.cheetah.webserver.websocket.defaults.WebSocketDefaultService;
import org.simpleframework.http.socket.*;

import org.slf4j.LoggerFactory;

public class WebSocketChatListener extends WebSocketDefaultListener {

//   private final CertificateUserExtractor extractor;
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(WebSocketChatListener.class);

    public WebSocketChatListener(WebSocketDefaultService webSocketService) {
        super(webSocketService);
    }

    @Override
    public void onFrame(Session session, Frame frame) {
        String user = getUserName(session);

        FrameType type = frame.getType();
        Frame replay = null;

        if (type == FrameType.PONG || type == FrameType.CLOSE) {
            return;
        }

        logger.debug("CHAT onFrame type: " + type + " from user: " + user);

        if (type == FrameType.TEXT) {
            try {

                String text = frame.getText();

                text = user + "> " + text;
                replay = new DataFrame(type, text);

            } catch (Exception e) {
                logger.error("Error creating text replay frame", e);
            }

            if (replay != null) {
                webSocketService.distribute(replay);
                webSocketService.getWebserver().distributeToWebsocketServiceFrame("org.cheetah.webserver.page.websocket.Chat2", session, frame);
            } else {
                logger.error("Replay is null");
            }

            printRequest(session);
        }
    }
    
    @Override
    public void onError(Session session, Exception cause) {
        String user = getUserName(session);

        if (!cause.toString().equals("java.io.IOException: Connection reset by peer")) {
            logger.error("CHAT onError from user: " + user, cause);
        } else {
            logger.warn("CHAT onError disconnected user: " + user);
        }
    }

    @Override
    public void onClose(Session session, Reason reason) {
        String secWebSocketKey = session.getRequest().getValue("Sec-WebSocket-Key");
        String user = webSocketService.getUsers().get(secWebSocketKey);

        logger.debug("CHAT onClose from user: " + user + " Reason: " + reason.getText());

        webSocketService.leave(secWebSocketKey);
    }
}
