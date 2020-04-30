/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cheetah.webserver;

import java.io.PrintStream;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.Status;
import org.simpleframework.http.core.Container;

/**
 *
 * @author philou
 */
public abstract class Page implements Container {

    protected PrintStream body;
    protected CheetahWebserver webserver; 
    protected StringBuilder debugString;
    
    public Page(){}
    
    public void setRessources(PrintStream body, CheetahWebserver webserver, StringBuilder debugString) {
        this.body = body;
        this.webserver = webserver;
        this.debugString = debugString;
    }
    
    protected void handleDefaultPage(Status status, Request request, Response response) throws Exception {

        Class lookupPage = null;
        //lookupPage = this.webserver.getClass(this.webserver.getDefaultPageHandler());
        lookupPage = this.webserver.getDefaultPageClass();
        AbstractPageDefault page = (AbstractPageDefault) lookupPage.newInstance();
        page.setRessources(body, webserver, debugString);
        page.setStatus(status);

        page.handle(request, response);
    }
}
