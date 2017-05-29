/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cheetah.webserver.page.admin;

import java.io.IOException;
import org.cheetah.nio.file.Files;
import org.cheetah.nio.file.Path;
import org.cheetah.nio.file.Paths;
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
public class RenameFile extends Page {

    private static org.slf4j.Logger logger = LoggerFactory.getLogger(RenameFile.class);

    @Override
    public void handle(Request request, Response response) {

        String hostPort = webserver.getHostPort(request);
        String referer = request.getValue("Referer");

        String fileRoot = this.webserver.getFileDefaultRoot();
        String oldFileName = request.getParameter("oldFileName");
        String fileName = request.getParameter("fileName");

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

        Path sourcePath = Paths.get(fileRoot + oldFileName).toAbsolutePath().normalize();
        Path destinationPath = Paths.get(fileRoot + fileName).toAbsolutePath().normalize();
        Path rootPath = Paths.get(fileRoot).toAbsolutePath().normalize();
        if (!destinationPath.startsWith(rootPath)) {
            responseJSON.put("MessageType", "Error");
            responseJSON.put("errorMessage", "Error renaming file: " + fileName + ": Insufficient privileges");
            logger.error("Error uploading file: " + fileName + ": Insufficient privileges");
            
        } else if (!this.webserver.isSessionAuthenticationEnabled() || this.webserver.isSessionAuthenticationEnabled() && !this.webserver.getUsername(request).equals("")) {
            if (this.webserver.isFileFolderBrowsingReadWrite()) {
                if (Files.isSymbolicLink(sourcePath) && !webserver.isFileFollowSymLinks()) {
                    responseJSON.put("MessageType", "Error");
                    responseJSON.put("errorMessage", "Error renaming file: " + fileName + ": Insufficient privileges");
                    logger.error("Error renaming file: " + fileName + ": Insufficient privileges");
                    response.setStatus(Status.UNAUTHORIZED);
                } else {
                    try {
                        Files.move(sourcePath, destinationPath);
                        responseJSON.put("MessageType", "Success");

                    } catch (IOException ex) {
                        responseJSON.put("MessageType", "Error");
                        responseJSON.put("errorMessage", "Error renaming file: " + fileName + ": " + ex.toString());
                        logger.error("Error renaming file: " + fileName + ": " + ex.toString());
                    }
                }
            } else {
                responseJSON.put("MessageType", "Error");
                responseJSON.put("errorMessage", "Error renaming file: " + fileName + ": Insufficient privileges");
                logger.error("Error renaming file: " + fileName + ": Insufficient privileges");
                response.setStatus(Status.UNAUTHORIZED);
            }
        } else {
            responseJSON.put("MessageType", "Error");
            responseJSON.put("errorMessage", "Error renaming file: " + fileName + ": Insufficient privileges");
            logger.error("Error renaming file: " + fileName + ": Insufficient privileges");
            response.setStatus(Status.UNAUTHORIZED);
        }

        body.println(responseJSON.toString());

        if (responseJSON.getString("MessageType").equals("Success")) {
            
            String folder = fileName.substring(0, fileName.lastIndexOf("/") +1);            
            this.webserver.distributeToWebsocketServiceMessage("org.cheetah.webserver.page.ressources.FolderWebSocket", folder);
        }
    }
}
