/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cheetah.webserver.authentication;

import java.net.UnknownHostException;
import java.util.AbstractMap;
import java.util.Base64;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import org.simpleframework.http.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author ph002551
 */
public abstract class AbstractAuthenticator implements IAuthenticator {

    private static final Logger logger = LoggerFactory.getLogger(AbstractAuthenticator.class);

    protected static AbstractAuthenticator instance = null;

    protected Object sessionObject = null;

    public static boolean isInstance(){
        boolean result = false;

        if(instance != null){
            result = true;
        }

        return result;
    }

    public static IAuthenticator getInstance(){

        if(instance == null){
            logger.error("Error getting Authenticator instance: instance is null. Instance must be set in authenticator class constructor (see like in FREE.java class)");
        }

        return instance;
    }

    @Override
    public Object getSessionObject() {
        return this.sessionObject;
    }

    @Override
    public boolean setPassword(String username, String oldPassword, String newPassword) {
        throw new UnsupportedOperationException("Not supported");
    }

    public static byte[] getServerUniqueFootPrint(){
        //byte[] result = new byte[]{5, 4, 5, 9, 5, 4, 2, 9, 3, 1, 8, 2, 8, 6, 4, 1};

        /*
        try {
            java.net.InetAddress addr = java.net.InetAddress.getLocalHost();
            result = addr.getAddress();
            System.out.println(result);
        } catch (UnknownHostException e) {
        }
        */

        oshi.SystemInfo systemInfo = new oshi.SystemInfo();
        oshi.software.os.OperatingSystem operatingSystem = systemInfo.getOperatingSystem();
        oshi.hardware.HardwareAbstractionLayer hardwareAbstractionLayer = systemInfo.getHardware();
        oshi.hardware.CentralProcessor centralProcessor = hardwareAbstractionLayer.getProcessor();
        oshi.hardware.ComputerSystem computerSystem = hardwareAbstractionLayer.getComputerSystem();

        String hostname = systemInfo.getOperatingSystem().getNetworkParams().getHostName();
        String vendor = operatingSystem.getManufacturer();
        String processorSerialNumber = computerSystem.getSerialNumber();
        String processorIdentifier = centralProcessor.getIdentifier();
        int processors = centralProcessor.getLogicalProcessorCount();

        String delimiter = "#";
        String result = hostname +
                delimiter+
                vendor +
                delimiter +
                processorSerialNumber +
                delimiter +
                processorIdentifier +
                delimiter +
                processors;

        return result.getBytes();
    }


    public static Entry<String, String> getCredentials(Request request) {
        return getCredentials(request, "Basic");
    }

    public static Entry<String, String> getCredentials(Request request, String authenticationScheme) {
        Entry result = new AbstractMap.SimpleEntry("", "");

        
        String userName = "";

        StringTokenizer tokenizer = new StringTokenizer(request.toString(), "\r\n");

        while (tokenizer.hasMoreTokens()) {
            String line = tokenizer.nextToken();
            String[] elements = line.split(":");

            //    body.println("<h1> elements[0]T: " + elements[0] + "</h1>");
            if (elements.length > 0) {

                if (elements[0].equalsIgnoreCase("Authorization")) {
                    if (line.length() > "Authorization: ".length()) {
                        userName = elements[1].substring(authenticationScheme.length() + 2);
                    }
                }
            }
        }
        

        if (!userName.equals("")) {

            switch(authenticationScheme) {

                case "Bearer":

                    result = new AbstractMap.SimpleEntry(userName, "");

                    break;


                default:
                    byte[] bytesEncoded = userName.getBytes();

                    byte[] valueDecoded = Base64.getDecoder().decode(bytesEncoded);
                    String[] credentials = new String(valueDecoded).split(":");

                    String username = "";
                    String password = "";
                    if (credentials.length > 0) {
                        username = credentials[0];
                    }
                    if (credentials.length > 1) {
                        password = credentials[1];
                    }
                    result = new AbstractMap.SimpleEntry(username, password);
            }
        }

        return result;
    }
}
