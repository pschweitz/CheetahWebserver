/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cheetah.webserver;

import java.util.Date;

/**
 *
 * @author phs
 */
public class SessionData {

    private String cookie;
    private String username;
    private String ipAddress;
    private String userAgent;
    private Date lastUseTime;
    private Date lastTryTime;
    private boolean isAuthenticated = false;
    private Object sessionObject = null;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Date getLastTryTime() {
        return lastTryTime;
    }

    public void setLastTryTime(Date lastTryTime) {
        this.lastTryTime = lastTryTime;
    }
    private int nbTry = 0; 

    public int getNbTry() {
        return nbTry;
    }

    public void setNbTry(int nbTry) {
        this.nbTry = nbTry;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public boolean isAuthenticated() {
        return isAuthenticated;
    }

    public void setIsAuthenticated(boolean isAuthenticated) {
        this.isAuthenticated = isAuthenticated;
    }

    public SessionData(String cookie, String username, String ipAddress, String userAgent, Object sessionObject) {
        this.cookie = cookie;
        this.username = username;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.lastUseTime = new Date();
        this.sessionObject = sessionObject;
    }
    
    public Date getLastUseTime() {
        return lastUseTime;
    }

    public void setLastUseTime(Date lastUseTime) {
        this.lastUseTime = lastUseTime;
    }

    public String getCookie() {
        //System.out.println("GET SessionData cookie: " + cookie.getName() + ": path: " + cookie.getPath());
        return this.cookie;
    }
    
    public Object getSessionObject(){
        return this.sessionObject;
    }
}
