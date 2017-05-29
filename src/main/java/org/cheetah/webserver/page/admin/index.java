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
            body.println("  </head>");
            body.println("<body>");

            body.println("<form action=\"/admin/ChangeProperties\"  method=\"get\">");

            for (PropertyType propertyType : WebServerContext.PropertyType.values()) {

                if (!propertyType.isRequireRestart()) {
                    String propertyName = propertyType.name();
                    String propertyValue = this.webserver.getWebserverContext().getString(propertyName);

                    body.print(propertyName + ": ");

                    if (Integer.class.isAssignableFrom(PropertyType.getClass(propertyName)) || Long.class.isAssignableFrom(PropertyType.getClass(propertyName))) {

                        body.println("<input type=\"number\" value=\"" + propertyValue + "\" name=\"" + propertyName + "\" style=\"width: 50em;\"><br>");
                    } else if (Boolean.class.isAssignableFrom(PropertyType.getClass(propertyName))) {

                        String checked = "";
                        if (propertyValue.equals("true")) {
                            checked = "checked";
                        }

                        body.println("<input type=\"checkbox\" " + checked + " name=\"" + propertyName + "\">");
                        body.println("<input type=\"hidden\" name=\"" + propertyName + "\" value=\"false\" /><br>");

                    } else {

                        String checked = "";
                        if (propertyValue.equals("true")) {
                            checked = "checked";
                        }

                        body.println("<input type=\"text\" value=\"" + propertyValue + "\" name=\"" + propertyName + "\" size=\"50\"><br>");
                    }
                }
            }

            body.println(" <input type=\"submit\" value=\"Apply\">");

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
