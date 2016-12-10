/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cheetah.webserver;

import org.simpleframework.http.Request;
import org.simpleframework.http.Response;

/**
 *
 * @author philou
 */
public class PageFolder extends Page {

    @Override
    public void handle(Request request, Response response) {
        
        String mimeType = MimeType.getMimeType("html");
        mimeType += "; charset=utf-8";
        response.setValue("Content-Type", mimeType);
        /*
        try {
            body.write("#".getBytes(Charset.forName("UTF-8")));
        } catch (IOException ex) {
            Logger.getLogger(PageDefaultFolder.class.getName()).log(Level.SEVERE, null, ex);
        }
                */
        //    body.write(new byte[]{35},0,1);
            body.println("<h1>Folder</h1>");
            body.println("<h1>" + request.getTarget() + "</h1>");
       
    }
}
