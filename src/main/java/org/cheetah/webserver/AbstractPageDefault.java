/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cheetah.webserver;

import org.simpleframework.http.Status;

/**
 *
 * @author philou
 */
public abstract class AbstractPageDefault extends Page implements IDefaultPage {

    protected Status status = Status.OK;
    protected Exception e = null;

    public void setStatus(Status status) {
        this.status = status;
    }
    
    public void setException(Exception e) {
        this.e = e;
    }
}
