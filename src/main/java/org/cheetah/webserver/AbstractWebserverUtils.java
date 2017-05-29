/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cheetah.webserver;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import org.cheetah.nio.file.Path;
import org.simpleframework.http.Request;

/**
 *
 * @author phs
 */
public  abstract class AbstractWebserverUtils {

    
    public abstract String loadPage(Request request, Path path, Charset charset) throws IOException, URISyntaxException;

    public abstract void readTextFile(Request request, PrintStream body, URL url, Charset charset) throws FileNotFoundException, IOException, URISyntaxException;

    public abstract void readBinaryFile(Request request, PrintStream body, URL url) throws FileNotFoundException, IOException, URISyntaxException;

    public abstract void readTextFileRessource(Request request, PrintStream body, URL url, ClassLoader classLoader, Charset charset) throws FileNotFoundException, IOException, URISyntaxException;

    public abstract void readBinaryFileRessource(Request request, PrintStream body, URL url, ClassLoader classLoader) throws FileNotFoundException, IOException, URISyntaxException;
}
