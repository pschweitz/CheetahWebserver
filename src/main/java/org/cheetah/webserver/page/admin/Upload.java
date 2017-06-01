/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cheetah.webserver.page.admin;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import org.cheetah.webserver.resources.upload.UploadInformation;
import org.cheetah.webserver.resources.upload.WebSocketUploadPage;
import org.json.JSONObject;
import org.simpleframework.http.ContentType;
import org.simpleframework.http.Part;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.Status;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Philippe Schweitzer
 */
public class Upload extends WebSocketUploadPage {

    private static org.slf4j.Logger logger = LoggerFactory.getLogger(Upload.class);
    public static ConcurrentHashMap<String, UploadInformation> uploadInformationMap = new ConcurrentHashMap();

    @Override
    public void handle(Request request, Response response) {

        String hostPort = webserver.getHostPort(request);
        String referer = request.getValue("Referer");
        String destination = request.getParameter("Destination");
        String errorMessage = "";

        String noProgressBar = request.getParameter("NoProgressBar");

        if (noProgressBar == null) {
            noProgressBar = request.getValue("NoProgressBar");

            if (noProgressBar == null) {
                noProgressBar = "null";
            }
        }
        logger.debug("NoProgressBar: " + noProgressBar);

        if (destination == null || destination.equals("null") || destination.equals("")) {
            destination = request.getValue("Destination");
        }

        response.setStatus(Status.OK);
        /*
        if (referer == null) {
            // bad request...
        }
         */
        String fileRoot = this.webserver.getFileDefaultRoot();
        String destinationFile = "";

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

        if (!destination.startsWith("/")) {
            destination = "/" + destination;
        }

        destination = destination.replaceAll("\\%20", " ");
        destination = fileRoot + destination;

        if (!destination.endsWith("/")) {
            destination = destination + "/";
        }

        logger.debug("Referer : " + referer);
        logger.debug("hostPort: " + hostPort);

        logger.debug("destination: " + destination);

        JSONObject responseJSON = new JSONObject();

        String contentType = request.getContentType().toString();

        String boundary = contentType.substring(contentType.indexOf("boundary=") + "boundary=".length());

        long headerLength = 0L;

        for (Part part : request.getParts()) {

            /*
            System.out.println("boundary    :" + boundary);
            System.out.println("File getFileName    :" + part.getFileName());
            System.out.println("File getContentType :" + part.getContentType());
            System.out.println("File getName        :" + part.getName());
            System.out.println("Content-Disposition :" + part.getHeader("Content-Disposition"));
             */
            headerLength += boundary.length() + 3;
            headerLength += part.getHeader("Content-Disposition").length() + 1 + "Content-Disposition: ".length();

            ContentType partContentType = part.getContentType();
            if (partContentType != null) {
                String partContentTypeString = part.getContentType().toString();
                headerLength += partContentTypeString.length() + 1 + "Content-Type: ".length();
            }

            InputStream is = null;
            long totalReadbytes = 0L;
            try {
                if (!part.getName().equals("file")) {

                    is = part.getInputStream();

                    if (is != null) {

                        byte[] buffer = new byte[8192];
                        totalReadbytes = 0;
                        int readbytes = is.read(buffer);

                        while (readbytes != -1) {
                            totalReadbytes += readbytes;
                            readbytes = is.read(buffer);
                        }

                        headerLength += totalReadbytes + 2;
                    } else {
                        headerLength += part.getContent().length() + 2;
                    }
                }

            } catch (IOException ex) {
                logger.error("Error calculating part length: " + part.getFileName() + ": " + ex.toString());
            } catch (Exception ex) {
                logger.error("Error calculating part length: " + part.getFileName() + ": " + ex.toString());
            } finally {
                try {
                    is.close();
                } catch (Exception ex) {
                }
            }

            headerLength += 4;

        }

        headerLength += boundary.length() + 9;

        for (Part part : request.getParts()) {

            if (part.getName().equals("file")) {

                String user = this.webserver.getUsername(request);

                if (user.equals("")) {
                    user = this.webserver.getSessionID(request);
                }

                String agent = request.getValue("User-Agent");

                destinationFile = destination + part.getFileName();

                Path destinationPath = Paths.get(destinationFile).toAbsolutePath().normalize();
                Path rootPath = Paths.get(fileRoot).toAbsolutePath().normalize();

                logger.debug("destinationPath: " + destinationPath);
                logger.debug("rootPath       : " + rootPath);

                if (!destinationPath.startsWith(rootPath)) {
                    responseJSON.put("MessageType", "Error");
                    responseJSON.put("errorMessage", "Error uploading file: " + part.getFileName() + ": Insufficient privileges");
                    logger.error("Error uploading file: " + part.getFileName() + ": Insufficient privileges");
                } else if (!this.webserver.isSessionAuthenticationEnabled() || this.webserver.isSessionAuthenticationEnabled() && !this.webserver.getUsername(request).equals("")) {

                    if (!this.webserver.isSessionAuthenticationEnabled() || (this.webserver.isSessionAuthenticationEnabled() && !this.webserver.isFileUploadAdminOnly()) || (this.webserver.isSessionAuthenticationEnabled() && this.webserver.isFileUploadAdminOnly() && this.webserver.isAdminUser(user))) {

                        if (agent != null && (agent.toLowerCase().contains("ios") || agent.toLowerCase().contains("iphone") || agent.toLowerCase().contains("ipad"))) {

                            String pattern = "yyyyMMddhhmmssS";
                            SimpleDateFormat format = new SimpleDateFormat(pattern);

                            if (destinationFile.contains(".")) {
                                destinationFile = destinationFile.substring(0, destinationFile.lastIndexOf('.')) + format.format(new Date()) + destinationFile.substring(destinationFile.lastIndexOf('.'), destinationFile.length());

                            } else {
                                destinationFile = destinationFile + format.format(new Date());
                            }

                        }

                        long fileSize = request.getContentLength() - headerLength;
                        long uploadLimit = this.webserver.getFileUploadLimit();

                        if (fileSize > uploadLimit) {
                            try {
                                //part.getInputStream().skip(Long.MAX_VALUE);
                                //part.getInputStream().read();
                                part.getInputStream().close();
                            } catch (Exception ex) {
                            }
                            response.setStatus(Status.UNAUTHORIZED);
                            responseJSON.put("MessageType", "Error");
                            responseJSON.put("errorMessage", "Error uploading file: " + part.getFileName() + ": File size exeeds upload limit: " + uploadLimit + " (B)");
                            logger.error("Error uploading file: " + part.getFileName() + ": File size exeeds upload limit: " + uploadLimit + " (B)");

                            errorMessage = "Error uploading file: " + part.getFileName() + ": File size exeeds upload limit: " + uploadLimit + " (B)";

                        } else {

                            UploadThread uploadThread = new UploadThread(destinationFile, fileSize, part, user, referer);
                            uploadThread.start();

                            if (noProgressBar.equals("true")) {
                                try {
                                    uploadThread.join();
                                } catch (InterruptedException ex) {
                                }

                                if (uploadInformationMap.containsKey(destinationFile)) {

                                    errorMessage = uploadInformationMap.get(destinationFile).errorMessage;

                                    if (errorMessage == null) {
                                        errorMessage = "";
                                    }

                                    if (!errorMessage.equals("")) {
                                        responseJSON.put("MessageType", "Error");
                                        responseJSON.put("errorMessage", errorMessage);
                                        response.setStatus(Status.NOT_FOUND);
                                    } else {
                                        responseJSON.put("MessageType", "Success");
                                    }

                                    uploadInformationMap.remove(destinationFile);
                                }

                            }
                        }
                    } else {
                        responseJSON.put("MessageType", "Error");
                        responseJSON.put("errorMessage", "Error uploading file: " + part.getFileName() + ": Insufficient privileges");
                        logger.error("Error uploading file: " + part.getFileName() + ": Insufficient privileges");
                    }
                } else {
                    responseJSON.put("MessageType", "Error");
                    responseJSON.put("errorMessage", "Error uploading file: " + part.getFileName() + ": Insufficient privileges");
                    logger.error("Error uploading file: " + part.getFileName() + ": Insufficient privileges");
                }

                break;
            }
        }

        if (responseJSON.has("MessageType")) {

            if (responseJSON.getString("MessageType").equalsIgnoreCase("error")) {
                response.setValue("Content-Type", "application/json");
                body.print(responseJSON.toString());
                return;
            } else {
                response.setValue("Content-Type", "text/html");
            }
        } else {
            response.setValue("Content-Type", "text/html");
        }

        if (!noProgressBar.equals("true")) {

            try {
                URL url = this.webserver.getClassLoader().getResource("org/cheetah/webserver/resources/upload/adminUpload.htm");

                logger.debug("url: " + url.toString());

                if (!this.webserver.isWebsocketEnabled() || !errorMessage.equals("")) {
                    body.println("<!DOCTYPE html>");
                    body.println("<html>");
                    body.println("    <head>");
                    body.println("        <script type=\"text/javascript\">");
                    body.println("              function goback() {");
                    body.println("                window.location = document.referrer;");
                    body.println("              }");
                    if (errorMessage.equals("")) {
                        body.println("              window.location = document.referrer;");
                    }
                    body.println("        </script>");
                    body.println("    </head>");
                    body.println("    <body>");
                    if (!errorMessage.equals("")) {
                        body.println("        <div id=\"status\">" + errorMessage + "</div>");
                    } else {
                        body.println("        <div id=\"status\"> Upload successfull </div>");
                    }
                    body.println("        <button onclick=\"goback()\">Close</button>");
                    body.println("    </body>");
                    body.println("</html>");
                } else {
                    this.webserver.getDefaultUtilsClass().readTextFileRessource(request, body, url, this.webserver.getClassLoader(), Charset.forName("utf-8"));
                }

            } catch (IOException ex) {
                response.setStatus(Status.NOT_FOUND);
                logger.error("Error uploading file: " + ex.toString());
            } catch (URISyntaxException ex) {
                response.setStatus(Status.NOT_FOUND);
                logger.error("Error uploading file: " + ex.toString());
            } catch (Exception ex) {
                response.setStatus(Status.NOT_FOUND);
                logger.error("Error uploading file: " + ex.toString());
            }
        }

        this.webserver.distributeToWebsocketServiceMessage("org.cheetah.webserver.page.ressources.FolderWebSocket", destination.substring(fileRoot.length()));
    }

    private class UploadThread extends Thread {

        private String destinationFile;
        private long fileSize;
        private Part part;
        private String user;
        public String referer;

        private UploadInformation uploadInformation;

        public UploadThread(String destinationFile, long fileSize, Part part, String user, String referer) {
            this.destinationFile = destinationFile;
            this.fileSize = fileSize;
            this.part = part;
            this.user = user;
            this.referer = referer;

            uploadInformation = new UploadInformation(destinationFile, fileSize, 0L, user, referer);

            if (!uploadInformationMap.containsKey(destinationFile)) {
                uploadInformationMap.put(destinationFile, uploadInformation);
            }
        }

        @Override
        public void run() {
            long totalReadbytes = 0L;
            uploadInformation.fileSent = totalReadbytes;
            InputStream is = null;
            FileOutputStream fos = null;
            try {

                is = part.getInputStream();
                fos = new FileOutputStream(destinationFile);

                byte[] buffer = new byte[1048576];
                totalReadbytes = 0;
                int readbytes = is.read(buffer);

                while (readbytes != -1 && !uploadInformation.canceled) {
                    totalReadbytes += readbytes;
                    uploadInformation.fileSent = totalReadbytes;
                    fos.write(buffer, 0, readbytes);
                    readbytes = is.read(buffer);

                    // Thread.sleep(500);
                    //logger.debug("totalReadbytes: " + totalReadbytes);
                }

            } catch (FileNotFoundException ex) {
                uploadInformation.errorMessage = "Error uploading file: " + part.getFileName() + ": " + ex.toString();
                logger.error("Error uploading file: " + part.getFileName() + ": " + ex.toString());
            } catch (IOException ex) {
                uploadInformation.errorMessage = "Error uploading file: " + part.getFileName() + ": " + ex.toString();
                logger.error("Error uploading file: " + part.getFileName() + ": " + ex.toString());
            } catch (Exception ex) {
                uploadInformation.errorMessage = "Error uploading file: " + part.getFileName() + ": " + ex.toString();
                logger.error("Error uploading file: " + part.getFileName() + ": " + ex.toString());
            } finally {
                try {
                    is.close();
                } catch (Exception ex) {
                }
                try {
                    fos.close();
                } catch (Exception ex) {
                }
            }
            //uploadInformation.errorMessage = "test23";
            uploadInformation.finished = true;
        }
    }
}
