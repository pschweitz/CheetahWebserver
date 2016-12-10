/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cheetah.webserver.websocket.defaults;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.cheetah.webserver.Page;
import org.simpleframework.common.buffer.Allocator;
import org.simpleframework.common.buffer.ArrayAllocator;
import org.simpleframework.http.core.ContainerTransportProcessor;
import org.simpleframework.http.socket.service.DirectRouter;
import org.simpleframework.http.socket.service.Router;
import org.simpleframework.http.socket.service.RouterContainer;
import org.simpleframework.transport.Transport;
import org.slf4j.LoggerFactory;

/**
 *
 * @author philou
 */
public abstract class WebSocketPage extends Page {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(WebSocketPage.class);

    protected Router negotiator;
    protected RouterContainer container;
    protected Allocator allocator;
    protected ContainerTransportProcessor processor;

    protected WebSocketService service;

    public void initService() {

        ConcurrentHashMap<String, WebSocketService> websocketServiceList = this.webserver.getWebsocketServiceList();
        try {
            String serviceClassName = this.getClass().getName();

            if (websocketServiceList.containsKey(serviceClassName)) {

                service = websocketServiceList.get(serviceClassName);

                this.negotiator = service.getNegotiator();
                this.container = service.getContainer();
                this.allocator = service.getAllocator();
                this.processor = service.getProcessor();
            } else {

                websocketServiceList.put(serviceClassName, service);

                int workerWebsocketThreads = this.webserver.getThreadWorkerWebsocket();

                this.negotiator = new DirectRouter(service);
                this.container = new RouterContainer(this.webserver, this.negotiator, workerWebsocketThreads);
                this.allocator = new ArrayAllocator();
                this.processor = new ContainerTransportProcessor(container, allocator, workerWebsocketThreads);

                service.setNegotiator(negotiator);
                service.setContainer(container);
                service.setAllocator(allocator);
                service.setProcessor(processor);
            }

            service.setWebserver(this.webserver);

        } catch (IOException ex) {

            logger.error("Error during service initialization", ex);
        }
    }

    public void process(Transport transport) throws IOException {
        Map map = transport.getAttributes();
        map.put(Transport.class, transport);
        processor.process(transport);
    }

    public void stop() throws IOException {
        processor.stop();
    }

    public RouterContainer getContainer() {
        return this.container;
    }

}
