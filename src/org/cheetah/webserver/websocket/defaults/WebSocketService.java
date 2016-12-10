/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.cheetah.webserver.websocket.defaults;

import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import org.cheetah.webserver.CheetahWebserver;
import org.simpleframework.common.buffer.Allocator;
import org.simpleframework.http.core.ContainerTransportProcessor;
import org.simpleframework.http.socket.Frame;
import org.simpleframework.http.socket.FrameChannel;
import org.simpleframework.http.socket.Session;
import org.simpleframework.http.socket.service.Router;
import org.simpleframework.http.socket.service.RouterContainer;
import org.simpleframework.http.socket.service.Service;
import org.slf4j.LoggerFactory;

/**
 *
 * @author phs
 */
public abstract class WebSocketService implements Service{
     
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(WebSocketService.class);
    
    protected CheetahWebserver webserver;
    protected WebSocketDefaultListener listener;
    protected Map<String, FrameChannel> sockets;
    protected Hashtable<String, String> users;
    
    private Router negotiator;
    private RouterContainer container;
    private Allocator allocator;
    private ContainerTransportProcessor processor;
        
    @Override
    public abstract void connect(Session connection);
    public abstract void join(String secWebSocketKey, String user, FrameChannel operation);
    public abstract void leave(String user);
    public abstract void distribute(Frame frame);
    public abstract void distribute(String text);   
        
    public WebSocketService(){    
        this.listener = new WebSocketDefaultListener((WebSocketDefaultService) this);
        this.sockets = new ConcurrentHashMap<>();
        this.users = new Hashtable<>();
    }
    
    protected void setWebserver(CheetahWebserver webserver){
        this.webserver = webserver;
    }
    
    public CheetahWebserver getWebserver() {
        return webserver;
    }
    
    public Hashtable<String,String> getUsers() {
        return users;
    } 

    protected Router getNegotiator() {
        return negotiator;
    }

    protected void setNegotiator(Router negotiator) {
        this.negotiator = negotiator;
    }

    protected RouterContainer getContainer() {
        return container;
    }

    protected void setContainer(RouterContainer container) {
        this.container = container;
    }

    protected Allocator getAllocator() {
        return allocator;
    }

    protected void setAllocator(Allocator allocator) {
        this.allocator = allocator;
    }

    protected ContainerTransportProcessor getProcessor() {
        return processor;
    }

    protected void setProcessor(ContainerTransportProcessor processor) {
        this.processor = processor;
    }

    public WebSocketDefaultListener getListener() {
        return listener;
    }
}
