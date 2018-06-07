/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cheetah.webserver;

import java.net.URL;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author philou
 */
public class PageDefaultFolder extends Page {

    private static final Logger logger = LoggerFactory.getLogger(PageDefaultFolder.class);

    private static final ConcurrentHashMap<String, String> fileIconsAlias = new ConcurrentHashMap();

    private final int imgScale = 24;

    static {
        fileIconsAlias.put("htm", "html");
        fileIconsAlias.put("docx", "doc");
        fileIconsAlias.put("xlsx", "xls");
        fileIconsAlias.put("pptx", "ppt");
        fileIconsAlias.put("jar", "java");
        fileIconsAlias.put("log", "txt");
        fileIconsAlias.put("jpeg", "jpg");
        fileIconsAlias.put("tar", "zip");
        fileIconsAlias.put("gz", "zip");
        fileIconsAlias.put("bz", "zip");
        fileIconsAlias.put("rar", "zip");
    }

    @Override
    public void handle(Request request, Response response) {

        String mimeType = MimeType.getMimeType("html");
        response.setValue("Content-Type", mimeType);

        org.simpleframework.http.Path path = request.getPath();

        String directory = path.getDirectory();
        String name = path.getName();

        String page;

        if (name != null) {
            page = directory + name;
        } else {
            page = directory;
        }

        boolean pluginJqwidjets = false;

        for (String pluginName : CheetahWebserver.pluginList) {

            if (pluginName.contains("jqwidgets")) {
                pluginJqwidjets = true;
            }
        }

        if (!page.endsWith("/")) {
            page = page + "/";
        }

        String contentOnly = request.getParameter("ContentOnly");

        logger.debug("ContentOnly: " + contentOnly);

        if (contentOnly == null || !contentOnly.equals("true")) {

            body.println("<!DOCTYPE html>");
            body.println("<html>");
            body.println("  <head>");
            body.println("      <title>Cheetah webserver file browser</title> ");

            body.println("      <script type=\"text/javascript\">");
            body.println("          function updateContent() {");
            body.println("              folderContent = document.getElementById(\"folderContent\");");

            if (pluginJqwidjets) {
                body.println("              $(\"#folderContent\").load(document.URL + '?ContentOnly=true');");
            } else {

                body.println("              var xmlHttp = new XMLHttpRequest();");
                body.println("              xmlHttp.onreadystatechange = function() {");
                body.println("                  if (xmlHttp.readyState == 4 && xmlHttp.status == 200){");
                body.println("                      folderContent.innerHTML = xmlHttp.responseText;");
                body.println("                  }");
                body.println("              };");
                body.println("              xmlHttp.open(\"GET\", document.URL + '?ContentOnly=true', true); // true for asynchronous");
                body.println("              xmlHttp.send(null);");
            }
            body.println("          }");
            body.println("      </script>");

            if (pluginJqwidjets) {
                body.println("      <link type=\"text/css\" rel=\"Stylesheet\" href=\"/jqwidgets/styles/jqx.base.css\" />");
                body.println("      <script type=\"text/javascript\" src=\"/scripts/jquery-1.11.1.min.js\"></script>");
                body.println("      <script type=\"text/javascript\" src=\"/jqwidgets/jqxcore.js\"></script>");
                body.println("      <script type=\"text/javascript\" src=\"/jqwidgets/jqxbuttons.js\"></script>");
                body.println("      <script type=\"text/javascript\" src=\"/jqwidgets/jqxfileupload.js\"></script>");
                body.println("      <script type=\"text/javascript\" src=\"/jqwidgets/jqxprogressbar.js\"></script>");
                body.println("      <script type=\"text/javascript\">");
                body.println("          $(document).ready(function () {");
                body.println("              $('#jqxFileUpload').jqxFileUpload({ width: 300, uploadUrl: '/admin/Upload?NoProgressBar=false&Destination=" + page + "', fileInputName: 'file' });");
        //        body.println("              $('#jqxProgressBar').jqxProgressBar({animationDuration: 0, showText: true, renderText: renderText, template: 'info', width: 250, height: 30, value: 0});");
        //        body.println("              $('#jqxProgressBar').hide();");
                if (this.webserver.isWebsocketEnabled()) {

                    body.println("              var renderText = function(text, value) {");
                    //                  if (value < 55) {
                    //                      return "<span style='color: #333;'>" + text + "</span>";
                    //                  }
                    body.println("                  return \"<span style='color: #fff;'>\" + text + \"</span>\";");
                    body.println("              };");
                    body.println("              $('#jqxFileUpload').on('uploadStart', function (event) {");
                    body.println("                  console.log(event);");

                    body.println("                  json = event.args.response;");

                    body.println("                  console.log(event.args)");
                    body.println("                  console.log(json)");                    
        //            body.println("                  $('#jqxProgressBar').show();");                    
        //            body.println("                  openUploadWebsocket();");
                    body.println("              });");
                }

                body.println("              $('#jqxFileUpload').on('uploadEnd', function (event) {");
                
        //        body.println("                  $('#jqxProgressBar').hide();");
        //        body.println("                  closeUploadWebsocket();");
                body.println("                  console.log(event);");

                if (!this.webserver.isWebsocketEnabled()) {
                    body.println("                  folderContent = document.getElementById(\"folderContent\");");
                    body.println("                  $('#folderContent').load(document.URL + '?ContentOnly=true');");
                }
                body.println("                  json = event.args.response;");

                body.println("                  console.log(event.args);");
                body.println("                  console.log(json)");
                body.println("                  if (json === \"\") {");
                body.println("                      json = \"{}\";");
                body.println("                  } else {");
                body.println("                  var n = json.search(/>{/i);");
                body.println("                  json = event.args.response.substring(n+1);");
                body.println("                  json = json.replace(\"</pre>\", \"\");");
                body.println("                  }");
                body.println("                  data = JSON.parse(json);");
                body.println("                  if (data.MessageType === \"Error\") {");
                body.println("                      alert(data.errorMessage);");
                body.println("                  }");
                body.println("              });");
                body.println("          });");

                body.println("      </script>");

                if (this.webserver.isWebsocketEnabled()) {
                    body.println("      <script type=\"text/javascript\" src=\"/javascript/WebSocketUpload\"></script>");
                }
            }

            if (this.webserver.isWebsocketEnabled()) {
                body.println("      <script type=\"text/javascript\" src=\"/javascript/WebSocketURL\"></script>");
                body.println("      <script type=\"text/javascript\">");
                body.println("      websocketHost = getWebsocketRootURL();");
                body.println("      websocket = new WebSocket(websocketHost + \"/ressources/FolderWebSocket\")");
                body.println("      websocket.onopen = function () {");
                body.println("      }");
                body.println("      websocket.onmessage = function (evt) {");
                body.println("          if('" + page + "' === evt.data.toString()){");
                body.println("              updateContent();");
                body.println("          }");
                body.println("      };");
                body.println("      websocket.onerror = function (evt) {");
                body.println("      };");
                body.println("      websocket.onclose = function (evt) {");
                body.println("      };");
                body.println("      </script>");
            }

            String parent = page;

            if (parent.length() > 1) {

                if (parent.endsWith("/")) {
                    parent = parent.substring(0, parent.length() - 1);
                }
                parent = parent.substring(0, parent.lastIndexOf('/') + 1);
            }

            body.println("      <script type=\"text/javascript\">");
            body.println("          function parentFolder() {");

            if (this.webserver.isWebsocketEnabled()) {
                body.println("              websocket.close();");
            }
            body.println("              window.location = '" + parent + "';");
            body.println("          }");
            body.println("          function newFolder() {");
            body.println("              var folderName = prompt(\"Please enter new folder name\",\"New Folder\");");
            body.println("              if (folderName != null) {");
            body.println("                  var xmlHttp = new XMLHttpRequest();");
            body.println("                  xmlHttp.onreadystatechange = function() {");
            body.println("                      if (xmlHttp.readyState == 4 && xmlHttp.status == 200){");
            body.println("                      json = xmlHttp.responseText");
            body.println("                      var n = json.search(/>{/i);");
            body.println("                      json = json.substring(n+1)");
            body.println("                      json = json.replace(\"</pre>\", \"\")");
            body.println("                      data = JSON.parse(json);");
            body.println("                      if (data.MessageType === \"Error\") {");
            body.println("                          alert(data.errorMessage)");
            body.println("                      }");
            if (!this.webserver.isWebsocketEnabled()) {
                body.println("                          updateContent();");
            }
            body.println("                      }");
            body.println("                  };");
            body.println("                  xmlHttp.open(\"GET\", \"/admin/CreateFolder?folderName=" + page + "\" + folderName, true); // true for asynchronous");
            body.println("                  xmlHttp.send(null);");
            body.println("              }");
            body.println("          }");
            body.println("          function renameFile(fileName) {");
            body.println("              var oldFileName = fileName;");
            body.println("              var fileName = prompt(\"Please enter new name\",fileName);");
            body.println("              if (fileName != null && fileName !== oldFileName) {");
            body.println("                  var xmlHttp = new XMLHttpRequest();");
            body.println("                  xmlHttp.onreadystatechange = function() {");
            body.println("                      if (xmlHttp.readyState == 4 && xmlHttp.status == 200){");
            body.println("                      json = xmlHttp.responseText");
            body.println("                      var n = json.search(/>{/i);");
            body.println("                      json = json.substring(n+1)");
            body.println("                      json = json.replace(\"</pre>\", \"\")");
            body.println("                      data = JSON.parse(json);");
            body.println("                      if (data.MessageType === \"Error\") {");
            body.println("                          alert(data.errorMessage)");
            body.println("                      }");
            if (!this.webserver.isWebsocketEnabled()) {
                body.println("                          updateContent();");
            }
            body.println("                      }");
            body.println("                  };");
            body.println("                  xmlHttp.open(\"GET\", \"/admin/RenameFile?oldFileName=\" + oldFileName + \"&fileName=\" + fileName, true); // true for asynchronous");
            body.println("                  xmlHttp.send(null);");
            body.println("              }");
            body.println("          }");
            body.println("          function deleteFile(fileName) {");
            body.println("              var xmlHttp = new XMLHttpRequest();");
            body.println("              xmlHttp.onreadystatechange = function() {");
            body.println("                  if (xmlHttp.readyState == 4 && xmlHttp.status == 200){");
            body.println("                      json = xmlHttp.responseText");
            body.println("                      var n = json.search(/>{/i);");
            body.println("                      json = json.substring(n+1)");
            body.println("                      json = json.replace(\"</pre>\", \"\")");
            body.println("                      data = JSON.parse(json);");
            body.println("                      if (data.MessageType === \"Error\") {");
            body.println("                          alert(data.errorMessage)");
            body.println("                      }");
            if (!this.webserver.isWebsocketEnabled()) {
                body.println("                      updateContent();");
            }
            body.println("                  }");
            body.println("              };");
            body.println("              var r = confirm('Would you like to delete \"' + fileName + '\" element ?');");
            body.println("              if (r == true) {");
            body.println("                  xmlHttp.open(\"GET\", \"/admin/DeleteFile?fileName=\" + fileName, true); // true for asynchronous");
            body.println("                  xmlHttp.send(null);");
            body.println("              }");
            body.println("          }");
            body.println("      </script>");

            body.println("  </head>");
            body.println("<body>");
            body.println("  <h1>Folder: " + page + "</h1>");

            String user = this.webserver.getUsername(request);
            if (!user.equals("")) {
                body.println("  <h3>logged as: " + user + "</h3><a href='/admin/Logoff'>Disconnect</a>");
            } else {
                body.println("  <a href='/admin/Login'>Sign-in</a>");
            }

            if (!this.webserver.isSessionAuthenticationEnabled() || this.webserver.isSessionAuthenticationEnabled() && this.isUserUploadGranted(request)) {
                if (this.webserver.isFileUploadEnabled()) {
                    if (pluginJqwidjets) {
                        body.println("  <div id=\"jqxFileUpload\"></div>");
        //                body.println("  <div id=\"jqxProgressBar\"></div>");
                        body.println("  <div id=\"eventsPanel\"></div>");

                    } else {
                        body.println("  <form method=\"post\" action=\"/admin/Upload?Destination=" + page + "\" enctype=\"multipart/form-data\">");
                        body.println("  <p>");
                        body.println("  <input type=\"file\" name=\"file\" size=\"30\">");

                        String websocket = "";
                        if (this.webserver.isWebsocketEnabled()) {
                            websocket = "onClick=\"websocket.close();\"";
                        }

                        body.println("  <input type=\"submit\" name=\"upload\" value=\"Uploader\" " + websocket + ">");
                        body.println("  <input type=\"hidden\" name=\"MAX_FILE_SIZE\" value=\"" + this.webserver.getFileUploadLimit() + "\" />");
                        body.println("  </p>");
                        body.println("  </form>");
                    }
                }
            }

            URL url;
            if (!page.equals("/")) {
                body.print("  <a href='javascript:;' onClick=\"parentFolder();\">");

                url = this.webserver.getClassLoader().getResource("ressources/file-icons/32px");

                if (url != null) {
                    body.print("<img src=\"/ressources/file-icons/32px/parent.png\" height=\"" + imgScale + "\" width=\"" + imgScale + "\">");
                }
                body.println("Parent Folder </a>  ");
            }

            if (!this.webserver.isSessionAuthenticationEnabled() || this.webserver.isSessionAuthenticationEnabled() && !this.webserver.getUsername(request).equals("")) {
                if (this.webserver.isFileFolderBrowsingReadWrite()) {
                    body.print("  <a href='javascript:;' onClick=\"newFolder();\">");

                    url = this.webserver.getClassLoader().getResource("ressources/file-icons/32px");

                    if (url != null) {
                        body.print("<img src=\"/ressources/file-icons/32px/folder-new.png\" height=\"" + imgScale + "\" width=\"" + imgScale + "\">");
                    }
                    body.print("New Folder </a>  ");
                }
            }
            body.println("  <div id=\"folderContent\">");

        }

        for (FileInformation file : RessourceFinder.listRessources(webserver, request)) {
            displayFile(request, page, file);
        }

        if (contentOnly == null || !contentOnly.equals("true")) {
            body.println("  </div>");
            body.println("</body>");
            body.println("</html>");
        }

    }

    private void displayFile(Request request, String page, FileInformation file) {

        String fileName = file.getName();
        String extention = "_blank";

        if (fileName.contains(".")) {
            extention = fileName.substring(fileName.lastIndexOf(".") + 1);
        }

        if (fileIconsAlias.containsKey(extention)) {
            extention = fileIconsAlias.get(extention);
        }

        body.print("  <p>");

        body.print("<a href =\"" + page + fileName + "\">");

        URL url = this.webserver.getClassLoader().getResource("ressources/file-icons/32px");

        if (url != null) {

            if (file.isIsFolder()) {
                extention = "folder";
            }

            url = this.webserver.getClassLoader().getResource("ressources/file-icons/32px/" + extention.toLowerCase() + ".png");

            if (url == null) {
                extention = "_blank";
            }

            body.print("<img src=\"/ressources/file-icons/32px/" + extention.toLowerCase() + ".png\" alt=\"img\" height=\"" + imgScale + "\" width=\"" + imgScale + "\">");
        }

        body.print(fileName + "</a>  ");

        if (!file.isIsDynamic()) {
            body.print(new Date(file.getLastModified()) + " " + formatFileSize(file.getSize()));

            if (!this.webserver.isSessionAuthenticationEnabled() || this.webserver.isSessionAuthenticationEnabled() && !this.webserver.getUsername(request).equals("")) {
                if (this.webserver.isFileFolderBrowsingReadWrite()) {

                    if (!file.isIsPlugin()) {

                        url = this.webserver.getClassLoader().getResource("ressources/file-icons/32px/file-edit.png");
                        if (url != null) {
                            body.print("&nbsp;<a href='javascript:;' onClick=\"renameFile('" + page + fileName + "');\"><img src=\"/ressources/file-icons/32px/file-edit.png\" alt=\"rename\" height=\"" + imgScale + "\" width=\"" + imgScale + "\"></a>  ");
                            body.print("&nbsp;<a href='javascript:;' onClick=\"deleteFile('" + page + fileName + "');\"><img src=\"/ressources/file-icons/32px/recycle-bin.png\" alt=\"delete\" height=\"" + imgScale + "\" width=\"" + imgScale + "\"></a>  ");
                        } else {
                            body.print("&nbsp;<a href='javascript:;' onClick=\"renameFile('" + page + fileName + "');\">rename</a>  ");
                            body.print("&nbsp;<a href='javascript:;' onClick=\"deleteFile('" + page + fileName + "');\">delete</a>  ");
                        }
                    } else {
                        body.print("&nbsp;(plugin)  ");
                    }
                }
            }
        } else {

            body.print("&nbsp; (dynamic content)");
        }

        body.println("</p>");

    }

    private boolean isUserUploadGranted(Request request) {
        boolean result = false;

        String username = this.webserver.getUsername(request);

        if (!username.equals("")) {
            if (this.webserver.isFileUploadAdminOnly()) {
                result = this.webserver.isAdminUser(username);
            } else {
                result = true;
            }
        }

        return result;
    }

    private String formatFileSize(long fileSize) {

        String result = "";

        if (fileSize > Math.pow(2, 40)) {

            result = String.format("%.1f", fileSize / new Float(Math.pow(2, 40))) + " TB";

        } else if (fileSize > Math.pow(2, 30)) {

            result = String.format("%.1f", fileSize / new Float(Math.pow(2, 30))) + " GB";

        } else if (fileSize > Math.pow(2, 20)) {

            result = String.format("%.1f", fileSize / new Float(Math.pow(2, 20))) + " MB";

        } else if (fileSize > Math.pow(2, 10)) {

            result = String.format("%.1f", fileSize / new Float(Math.pow(2, 10))) + " KB";

        } else {
            result = fileSize + "  B";
        }

        return result;
    }
}
