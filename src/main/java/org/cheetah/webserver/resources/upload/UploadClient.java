/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cheetah.webserver.resources.upload;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Philippe Schweitzer
 */
public class UploadClient {

    private Path file;
    private URL url;
    private String destination = "";
    private String username = "";
    private String password = "";

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(UploadClient.class);

    public static void main(String[] args) {

        try {
            Path file = Paths.get("/var/log/syslog").toAbsolutePath();
            URL url = new URL("http://172.20.10.2:8080/admin/Upload");

            UploadClient uploadClient = new UploadClient(file, url, "New Folder3", "admin", "");

            uploadClient.send();

        } catch (MalformedURLException ex) {
            Logger.getLogger(UploadClient.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public UploadClient(Path file, URL url, String destination) {
        this(file, url, destination, "", "");
    }

    public UploadClient(Path file, URL url, String destination, String username, String password) {
        this.file = file;
        this.url = url;
        this.destination = destination;
        this.username = username;
        this.password = password;

    }

    public void send() {
        File binaryFile = null;
        String charset = "utf-8";
        String param = "value";
        String boundary = Long.toHexString(System.currentTimeMillis()); // Just generate some unique random value.
        String CRLF = "\r\n"; // Line separator required by multipart/form-data.

        try {
            binaryFile = file.toFile();

            URLConnection connection = url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            connection.addRequestProperty("NoProgressBar", "true");
            connection.addRequestProperty("Destination", destination);

            if (!username.equals("")) {
                String userpass = username + ":" + password;
                String basicAuth = "Basic " + new String(Base64.getEncoder().encode(userpass.getBytes()));
                connection.setRequestProperty("Authorization", basicAuth);
            }

            OutputStream output = connection.getOutputStream();
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(output, charset), true);

            // Send normal param.
            writer.append("--" + boundary).append(CRLF);
            writer.append("Content-Disposition: form-data; name=\"param\"").append(CRLF);
            writer.append("Content-Type: text/plain; charset=" + charset).append(CRLF);
            writer.append(CRLF).append(param).append(CRLF).flush();

            // Send binary file.
            writer.append("--" + boundary).append(CRLF);
            writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"" + binaryFile.getName() + "\"").append(CRLF);
            writer.append("Content-Type: " + URLConnection.guessContentTypeFromName(binaryFile.getName())).append(CRLF);
            writer.append("Content-Transfer-Encoding: binary").append(CRLF);
            writer.append(CRLF).flush();

            FileInputStream in = new FileInputStream(binaryFile);

            DataInputStream din = new DataInputStream(in);
            byte[] buffer = new byte[1048576];

            int readbytes = din.read(buffer);

            while (readbytes != -1) {
                output.write(buffer, 0, readbytes);
                readbytes = din.read(buffer);
            }

            output.flush(); // Important before continuing with writer!
            writer.append(CRLF).flush(); // CRLF is important! It indicates end of boundary.

            // End of multipart/form-data.
            writer.append("--" + boundary + "--").append(CRLF).flush();

// Request is lazily fired whenever you need to obtain information about response.
            int responseCode = ((HttpURLConnection) connection).getResponseCode();
            System.out.println(responseCode); // Should be 200

        } catch (IOException ex) {
            logger.error("Error sending file: " + file + ": " + ex.toString());
        }
    }
}
