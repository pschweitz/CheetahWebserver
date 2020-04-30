/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cheetah.webserver.page.admin;

import org.cheetah.webserver.*;
import java.util.concurrent.ConcurrentHashMap;
import org.cheetah.webserver.WebServerContext.PropertyType;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author philou
 */
public class index extends Page {

    private static Logger logger = LoggerFactory.getLogger(index.class);

    private static ConcurrentHashMap<String, String> fileIconsAlias = new ConcurrentHashMap();

    static {
        fileIconsAlias.put("htm", "html");
        fileIconsAlias.put("docx", "doc");
        fileIconsAlias.put("xlsx", "xls");
        fileIconsAlias.put("pptx", "ppt");
        fileIconsAlias.put("jar", "java");
        fileIconsAlias.put("log", "txt");
        fileIconsAlias.put("jpeg", "jpg");
    }

    @Override
    public void handle(Request request, Response response) {

        String username = this.webserver.getUsername(request);

        if (!this.webserver.isSessionAuthenticationEnabled() || (this.webserver.isSessionAuthenticationEnabled() && this.webserver.isAdminUser(username))) {

            String mimeType = MimeType.getMimeType("html");
            response.setValue("Content-Type", mimeType);

            body.println("<!DOCTYPE html>");
            body.println("<html>");
            body.println("  <head>");
            body.println("      <title>Cheetah webserver administration panel</title> ");
            body.println("      <link rel=\"stylesheet\" type=\"text/css\" href=\"/css/Cheetah\">");
            body.println("  </head>");
            body.println("<body>");
            body.println("<div id=\"page\" class=\"page-class\">");

            body.println("<form action=\"/admin/ChangeProperties\"  method=\"get\">");

            body.println("  <table id=\"cheetahTable\">");
            body.println("    <tr>");
            body.println("      <td width=\"70%\">");
            body.println("        <h1>&nbsp;&nbsp;&nbsp;Administration page</h1>");
            body.println("      </td>");
            body.println("      <td width=\"30%\" style=\"text-align: center;\">");
            body.println("        <img src=\"/login/Logo\" height=\"60\"/><BR><BR>");
            body.println("        <a href =\"https://github.com/pschweitz/CheetahWebserver\" target=\"_blank\">" + this.webserver.serverName + "</a>");
            body.println("      </td>");
            body.println("    </tr>");
            body.println("  </table>");
            body.println("  <hr>");

            body.println("        <p><input class=\"button\" type=\"submit\" value=\"Apply changes\"></p>");

            body.println("      <table id=\"folderContentTable\" class=\"table table-striped\">");
            body.println("          <thead>");
            body.println("            <tr>");
            body.println("              <th>Configuration</th>");
            body.println("              <th>Value</th>");
            body.println("            </tr>");
            body.println("          </thead>");
            body.println("          <tbody>");

            for (PropertyType propertyType : WebServerContext.PropertyType.values()) {

                if (!propertyType.isRequireRestart()) {
                    body.println("            <tr>");
                    body.println("              <td width=\"90%\">");
                    String propertyName = propertyType.name();
                    String propertyValue = this.webserver.getWebserverContext().getString(propertyName);

                    //                   body.print(propertyName + ": ");
                    body.print("                " + propertyName);
                    body.println("              </td>");
                    body.println("              <td align=\"center\">");
                    if (Integer.class.isAssignableFrom(PropertyType.getClass(propertyName)) || Long.class.isAssignableFrom(PropertyType.getClass(propertyName))) {

                        body.println("                <input type=\"number\" value=\"" + propertyValue + "\" name=\"" + propertyName + "\" style=\"width: 368px;height:28px\"><br>");
                    } else if (Boolean.class.isAssignableFrom(PropertyType.getClass(propertyName))) {

                        String checked = "";
                        if (propertyValue.equals("true")) {
                            checked = "checked";
                        }

                        body.println("                <input style=\"width: 368px;\" type=\"checkbox\" " + checked + " name=\"" + propertyName + "\">");
                        body.println("                <input type=\"hidden\" name=\"" + propertyName + "\" value=\"false\" /><br>");

                    } else {

                        String checked = "";
                        if (propertyValue.equals("true")) {
                            checked = "checked";
                        }

                        body.println("                <input style=\"width: 368px;\" type=\"text\" value=\"" + propertyValue + "\" name=\"" + propertyName + "\" size=\"50\"><br>");
                    }

                    body.println("              </td>");
                    body.println("            </tr>");
                }
            }

            body.println("          </tbody>");
            body.println("      </table>");

            body.println("</div>");
            body.println("</body>");
            body.println("</html>");
        } else {

            Status status = Status.UNAUTHORIZED;
            try {

                handleDefaultPage(status, request, response);

            } catch (Exception ex) {
                this.debugString.append("Error generating " + status.getDescription() + ": " + ex.toString()).append(System.lineSeparator());
                logger.error("Error generating " + status.getDescription() + ": ", ex);
            }
        }
    }
}
