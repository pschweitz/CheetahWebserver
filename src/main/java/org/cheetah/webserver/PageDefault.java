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
public class PageDefault extends AbstractPageDefault {

    @Override
    public void handle(Request request, Response response) {

        response.setStatus(status);

        String mimeType = MimeType.getMimeType("html");
        response.setValue("Content-Type", mimeType);

        body.println("<link rel=\"stylesheet\" type=\"text/css\" href=\"/css/Cheetah\">");

        body.println("<div id=\"page\" class=\"page-class\">");
        body.println("  <table id=\"cheetahTable\">");
        body.println("    <tr>");
        body.println("      <td width=\"70%\">");
        if (status.getCode() > 400) {
            body.println("        <h1>&nbsp;&nbsp;&nbsp;Error " + status.getCode() + " - " + status.getDescription() + "</h1>");
        } else {
            body.println("        <h1>&nbsp;&nbsp;&nbsp;" + status.getCode() + " - " + status.getDescription() + "</h1>");
        }
        body.println("      </td>");
        body.println("      <td width=\"30%\" style=\"text-align: center;\">");
        body.println("        <img src=\"/login/Logo\" height=\"60\"/><BR><BR>");
        body.println("        <a href =\"https://github.com/pschweitz/CheetahWebserver\" target=\"_blank\">" + this.webserver.serverName + "</a>");
        body.println("      </td>");
        body.println("    </tr>");
        body.println("  </table>");
        body.println("  <hr>");

        if (request.getTarget().length() > 80) {
            body.println("<h2>&nbsp;&nbsp;&nbsp;" + request.getTarget().substring(0, 80) + " ...</h2>");
        } else {
            body.println("<h2>&nbsp;&nbsp;&nbsp;" + request.getTarget() + "</h2>");
        }
        
        if (this.e != null) {
            body.println("<div style=\"margin-left:20px\">");
            body.println("<p>");
            body.println(e.toString() + "<BR>");
            for (StackTraceElement element : e.getStackTrace()) {
                body.println( element.toString() + "<BR>");
            }
            body.println("</p>");
            body.println("</div>");
        }

        body.println("</div>");
    }
}
