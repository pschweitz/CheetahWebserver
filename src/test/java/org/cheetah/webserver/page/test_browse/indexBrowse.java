package org.cheetah.webserver.page.test_browse;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import org.cheetah.webserver.page.test.*;
import org.cheetah.webserver.page.*;
import org.cheetah.webserver.Page;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;

/**
 *
 * @author Philippe Schweitzer
 */
public class indexBrowse extends Page {

    @Override
    public void handle(Request rqst, Response rspns) {
        body.append("index browse");
    }
}
