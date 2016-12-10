package org.cheetah.webserver.websocket.server;

import java.io.Serializable;
import org.cheetah.webserver.websocket.defaults.WebSocketDefaultPage;
import org.simpleframework.transport.TransportProcessor;
import org.slf4j.LoggerFactory;

public abstract class WebSocketServerPage<RequestObject extends Serializable, ResponseObject extends Serializable> extends WebSocketDefaultPage implements TransportProcessor {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(WebSocketServerPage.class);

    public WebSocketServerPage(AbstractWebSocketServerWorker webSocketServerWorker) {
        super();
        service = new WebSocketServerService(webSocketServerWorker);
    }
}
