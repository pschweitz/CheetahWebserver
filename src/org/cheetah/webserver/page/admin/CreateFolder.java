/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cheetah.webserver.page.admin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.cheetah.webserver.Page;
import org.json.JSONObject;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.Status;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Philippe Schweitzer
 */
public class CreateFolder extends Page {

    private static org.slf4j.Logger logger = LoggerFactory.getLogger(CreateFolder.class);

    @Override
    public void handle(Request request, Response response) {

        String hostPort = webserver.getHostPort(request);
        String referer = request.getValue("Referer");

        String fileRoot = this.webserver.getFileDefaultRoot();
        String folderName = request.getParameter("folderName");

        if (this.webserver.isFileVirtualHostsEnabled()) {

            String host = this.webserver.getHostPort(request);
            if (host.contains(":")) {
                host = host.substring(0, host.indexOf(':'));
            }

            String virtualFileDefaultRoot = this.webserver.getWebserverVirtualHosts().getString(host);

            if (!virtualFileDefaultRoot.equals("")) {
                fileRoot = virtualFileDefaultRoot;
            }
        }

        if (!fileRoot.endsWith("/")) {
            fileRoot = fileRoot + "/";
        }

        logger.debug("Referer : " + referer);
        logger.debug("hostPort: " + hostPort);

        logger.debug("fileRoot: " + fileRoot);

        JSONObject responseJSON = new JSONObject();

        if (!this.webserver.isSessionAuthenticationEnabled() || this.webserver.isSessionAuthenticationEnabled() && !this.webserver.getUsername(request).equals("")) {
            if (this.webserver.isFileFolderBrowsingReadWrite()) {
                try {
                    Files.createDirectory(Paths.get(fileRoot + folderName));
                    responseJSON.put("MessageType", "Success");
                } catch (IOException ex) {
                    responseJSON.put("MessageType", "Error");
                    responseJSON.put("errorMessage", "Error creating folder: " + folderName + ": " + ex.toString());
                    logger.error("Error creating folder: " + folderName + ": " + ex.toString());
                }
            } else {
                responseJSON.put("MessageType", "Error");
                responseJSON.put("errorMessage", "Error creating folder: " + folderName + ": Insufficient privileges");
                logger.error("Error creating folder: " + folderName + ": Insufficient privileges");
            }
        } else {
            responseJSON.put("MessageType", "Error");
            responseJSON.put("errorMessage", "Error creating folder: " + folderName + ": Insufficient privileges");
            logger.error("Error creating folder: " + folderName + ": Insufficient privileges");
        }

        body.println(responseJSON.toString());

        if (responseJSON.getString("MessageType").equals("Success")) {
            
            String folder = folderName.substring(0, folderName.lastIndexOf("/") +1);  
            
            this.webserver.distributeToWebsocketServiceMessage("org.cheetah.webserver.page.ressources.FolderWebSocket", folder);
        }
    }
}
