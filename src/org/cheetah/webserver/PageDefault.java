/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cheetah.webserver;

import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.Status;

/**
 *
 * @author philou
 */
public class PageDefault extends AbstractPageDefault {


    @Override
    public void handle(Request request, Response response) {

        response.setStatus(status);

        String mimeType = MimeType.getMimeType("html");
        response.setValue("Content-Type", mimeType);

        body.println("<h1>" + status.getCode() + " - " + status.getDescription() + "</h1>");
        body.println("<h1>" + request.getTarget() + "</h1>");

        if (this.e != null) {
            body.println("<p>");
                body.println(e.toString() + "<BR>");
            for (StackTraceElement element : e.getStackTrace()) {
                body.println(element.toString() + "<BR>");
            }
            body.println("</p>");
        }
    }
}
