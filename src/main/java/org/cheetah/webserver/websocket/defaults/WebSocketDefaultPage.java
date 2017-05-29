package org.cheetah.webserver.websocket.defaults;

import org.simpleframework.transport.TransportProcessor;
import org.slf4j.LoggerFactory;

public abstract class WebSocketDefaultPage extends WebSocketPage implements TransportProcessor {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(WebSocketDefaultPage.class);

    public WebSocketDefaultPage() {
        super();
        service = new WebSocketDefaultService();
    }
}
