CheetahWebserver is a webserver created by Philippe Schweitzer 

https://ch.linkedin.com/pub/philippe-schweitzer/87/ab/2bb

Download available from this page: https://github.com/pschweitz/CheetahWebserver

The webserver aims to be an alternative to Tomcat for HTML dynamic pages development in Java.
It requires less than 256MB RAM to run, while Tomcat requires 2GB.

It created it to be a best choice in order to provide dynamic REST FULL web services, 
as well as for providing the minimal technology stack in order to build web-based administration interfaces for J2SE Java programs.

The webserver relies on a backend library called "simple framework" (https://github.com/ngallagher/simpleframework), 
and I would like to thank the author for his excellent work !

The webserver can either be run as a standalone Java (J2SE) program, with "java -jar" command, or integrated in a larger program at source code level.
The webserver is highly configurable thanks to its configuration file "webserver.properties" in the "etc/" folder.

By integration at the source code level, there is even more configurations available.
Dynamic pages must be put inside a specific package for being visible by the clients browsers.


It is possible to build applications like for a Tomacat webserver. These applications must be put inside the "plugin/" folder, inside a "zip" or "jar" archive. 
Then after a restart of the server, static and dynamic pages from applications become available for web browser clients.

The webserver also supports virtual hosts, and location of physical files are configured inside the "virtualhosts.properties" file.
Be careful, static contents of applications are available for all virtual host at the same path, 
while dynamic pages must be put inside a package having the same name than the - virtual - host name.
 
It is also possible to configure the webserver to run with SSL, thanks to the "ssl.properties". 
The way of achieving SSL communication is equivalent to a Tomcat webserver.

The webserver implements websockets, and it is possible to write your own service.

Finally the webserver also implements an authentication delegation mechanism. 


The webserver is still under development, but however, its features coverage already enables it to be used in real-case scenarios.

For example, just take a look at the "inslideshow" (http://www.in-slideshow.com) application to see it working within smart phones applications (both iOS and Android).

There are only 2 configuration items that are not fully finished at the moment: "WebserverMode" and "WebserverOutputCharset".

For the rest, I invite you to inspire yourself from the test cases for more details, as the documentation is also still under construction.
-> Otherwise, just contact me.
 

It is released under Apache v2 License

/*
 * Copyright 2018 Philippe Schweitzer.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
