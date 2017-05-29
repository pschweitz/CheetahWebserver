package org.cheetah.webserver;

import org.cheetah.webserver.websocket.defaults.WebSocketDefaultPage;
import org.cheetah.webserver.page.websocket.*;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.simpleframework.common.buffer.Allocator;
import org.simpleframework.common.buffer.ArrayAllocator;

import org.simpleframework.http.core.ContainerTransportProcessor;
import org.simpleframework.http.socket.service.DirectRouter;
import org.simpleframework.http.socket.service.Router;
import org.simpleframework.http.socket.service.RouterContainer;
import org.simpleframework.transport.Transport;
import org.simpleframework.transport.TransportProcessor;
import org.slf4j.LoggerFactory;

public abstract class WebSocketChatPage extends WebSocketDefaultPage implements TransportProcessor {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(WebSocketChatPage.class);
 
    public WebSocketChatPage() {
        super();
        
                service = new WebSocketChatService();
             
    }

    @Override
    public void process(Transport transport) throws IOException {
        Map map = transport.getAttributes();
        map.put(Transport.class, transport);
        processor.process(transport);
    }

    @Override
    public void stop() throws IOException {
        processor.stop();
    }

    @Override
    public RouterContainer getContainer() {
        return this.container;
    }  
}
