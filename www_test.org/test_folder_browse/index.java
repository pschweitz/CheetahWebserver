package test_folder_index1;

import org.cheetah.webserver.Page;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;

public class index extends Page {

    public void handle(Request rqst, Response rspns) {
        this.body.append("index plugin");
    }
}
