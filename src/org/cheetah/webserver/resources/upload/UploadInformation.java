/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cheetah.webserver.resources.upload;

/**
 *
 * @author Philippe Schweitzer
 */
public class UploadInformation {

    public String destinationFile;
    public long fileSize;
    public volatile long fileSent;
    public String user;
    public String referer;
    public String errorMessage;
    public boolean finished = false;
    public boolean canceled = false;
    
    public UploadInformation(String destinationFile, long fileSize, long fileSent, String user, String referer){
        this.destinationFile = destinationFile;
        this.fileSize = fileSize;
        this.fileSent = fileSent;
        this.user = user;
        this.referer = referer;
    }
}
