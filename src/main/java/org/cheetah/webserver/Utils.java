/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cheetah.webserver;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import org.cheetah.nio.file.Files;
import org.cheetah.nio.file.Path;
import org.simpleframework.http.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author phs
 */
public class Utils extends AbstractWebserverUtils {

    static Logger logger = LoggerFactory.getLogger(Utils.class);

    @Override
    public String loadPage(Request request, Path path, Charset charset) throws IOException, URISyntaxException {
        StringBuffer result = new StringBuffer();

        if (Files.exists(path)) {
            File file = path.toFile();
            BufferedReader br = null;

            FileInputStream fstream = new FileInputStream(file);
            InputStreamReader is = new InputStreamReader(fstream, charset);
            br = new BufferedReader(is);

            StringBuilder trace = new StringBuilder();

            String line = "";
            line = br.readLine();
            while (line != null) {
                result.append(line).append(System.lineSeparator());
                line = br.readLine();
            }

            br.close();
        } else {
            logger.error("Path not found: " + path.toString());
        }

        return result.toString();
    }

    @Override
    public void readTextFile(Request request, PrintStream body, URL url, Charset charset) throws FileNotFoundException, IOException, URISyntaxException {

        File file = new File(url.toURI());
        BufferedReader br = null;

        FileInputStream fstream = new FileInputStream(file);
        InputStreamReader is = new InputStreamReader(fstream, charset);
        br = new BufferedReader(is);

        String line = "";
        line = br.readLine();
        while (line != null) {
            body.println(line);
            line = br.readLine();
        }

        br.close();
    }

    @Override
    public void readBinaryFile(Request request, PrintStream body, URL url) throws FileNotFoundException, IOException, URISyntaxException {

        //logger.debug("URL: " + url);

        File file = new File(url.toURI());

        /*
        if (url.toString().toLowerCase().endsWith("png")
                || url.toString().toLowerCase().endsWith("jpg")
                || url.toString().toLowerCase().endsWith("jpeg")
                || url.toString().toLowerCase().endsWith("bmp")) {
          
            
            
            BufferedImage sourceImage = ImageIO.read(new FileInputStream(file));
            Image thumbnail = sourceImage.getScaledInstance(sourceImage.getWidth()/3, -1, Image.SCALE_SMOOTH);
            BufferedImage bufferedThumbnail = new BufferedImage(thumbnail.getWidth(null),
                    thumbnail.getHeight(null),
                    BufferedImage.TYPE_INT_RGB);
            bufferedThumbnail.getGraphics().drawImage(thumbnail, 0, 0, null);
            ImageIO.write(bufferedThumbnail, "jpeg", body);
            

        } else {
*/
            DataInputStream in = new DataInputStream(new FileInputStream(file));
            byte[] buffer = new byte[1048576];

            int readbytes = in.read(buffer);

            while (readbytes != -1) {
                body.write(buffer, 0, readbytes);
                readbytes = in.read(buffer);
            }

            in.close();
    //    }
    }

    @Override
    public void readTextFileRessource(Request request, PrintStream body, URL url, ClassLoader classLoader, Charset charset) throws FileNotFoundException, IOException, URISyntaxException {

        String fileName = url.toString();

        BufferedReader br = null;
        InputStream in = null;

        if (fileName.startsWith("jar")) {
            fileName = fileName.substring(fileName.lastIndexOf("!") + 1).substring(1);

            in = classLoader.getResourceAsStream(fileName);
        } else {
            fileName = fileName.substring(fileName.lastIndexOf(":") + 1);

            in = new FileInputStream(fileName);
        }

        InputStreamReader is = new InputStreamReader(in, charset);
        br = new BufferedReader(is);

        String line = "";
        line = br.readLine();

        while (line != null) {
            body.println(line);
            line = br.readLine();
        }

        in.close();
    }

    @Override
    public void readBinaryFileRessource(Request request, PrintStream body, URL url, ClassLoader classLoader) throws FileNotFoundException, IOException, URISyntaxException {

        String fileName = url.toString();
        InputStream in = null;

        if (fileName.startsWith("jar")) {
            fileName = fileName.substring(fileName.lastIndexOf("!") + 1).substring(1);

            in = classLoader.getResourceAsStream(fileName);
        } else {
            fileName = fileName.substring(fileName.lastIndexOf(":") + 1);

            in = new FileInputStream(fileName);
        }

        DataInputStream din = new DataInputStream(in);
        byte[] buffer = new byte[1048576];

        int readbytes = din.read(buffer);

        while (readbytes != -1) {
            body.write(buffer, 0, readbytes);
            readbytes = din.read(buffer);
        }

        in.close();
    }
}
