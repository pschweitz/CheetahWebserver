package org.cheetah.webserver.resources.upload;

import org.cheetah.webserver.websocket.defaults.*;
import org.simpleframework.transport.TransportProcessor;
import org.slf4j.LoggerFactory;

public abstract class WebSocketUploadPage extends WebSocketPage implements TransportProcessor {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(WebSocketUploadPage.class);

    public WebSocketUploadPage() {
        super();
        service = new WebSocketUploadService();
    }
}
