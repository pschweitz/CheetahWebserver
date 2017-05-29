/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cheetah.webserver.page.admin;

import java.util.Map;
import java.util.Map.Entry;
import org.cheetah.webserver.Page;
import java.util.Set;
import org.cheetah.webserver.WebServerContext;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author philou
 */
public class ChangeProperties extends Page {

    private static Logger logger = LoggerFactory.getLogger(ChangeProperties.class);

    @Override
    public void handle(Request request, Response response) {

        String username = this.webserver.getUsername(request);

        if (!this.webserver.isSessionAuthenticationEnabled() || this.webserver.isAdminUser(username)) {

            for (Entry<String, String> entry : (Set<Map.Entry<String, String>>) this.webserver.getWebserverContext().entrySet()) {

                String key = entry.getKey();
                String value = request.getParameter(entry.getKey());

                if (value != null) {

                    if (Boolean.class.isAssignableFrom(WebServerContext.PropertyType.getClass(key))) {

                        if (value.equals("on")) {
                            value = "true";
                        } else {
                            value = "false";
                        }
                    } else if (value.equals("null")) {
                        value = "";
                    }

                    logger.debug(key + ": " + value);

                    if (this.webserver.getWebserverContext().containsValue(key)) {
                        this.webserver.getWebserverContext().replace(key, value);
                    } else {
                        this.webserver.getWebserverContext().put(key, value);
                    }
                }
            }

            this.webserver.getWebserverContext().storeProperties();
            this.webserver.removeAllSessionDataExceptAdmin();
            this.webserver.populateAuthorizationFolder();

            body.println("<!DOCTYPE html>");
            body.println("<html>");
            body.println("    <head>");
            body.println("        <script type=\"text/javascript\">");
            body.println("                window.location = document.referrer;");
            body.println("        </script>");
            body.println("    </head>");
            body.println("    <body>");
            body.println("    </body>");
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
