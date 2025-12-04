/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.app;

import com.google.common.annotations.VisibleForTesting;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;
import org.apache.commons.io.FileUtils;
import org.sonar.api.utils.MessageException;
import org.sonar.process.Props;

import static java.lang.String.format;
import static org.sonar.process.ProcessProperties.Property.PATH_DATA;
import static org.sonar.process.ProcessProperties.Property.PATH_HOME;
import static org.sonar.process.ProcessProperties.Property.WEB_CONTEXT;

/**
 * Configures Tomcat contexts:
 * <ul>
 *   <li>/deploy delivers the plugins required by analyzers. It maps directory ${sonar.path.data}/web/deploy.</li>
 *   <li>/ is the regular webapp</li>
 * </ul>
 */
public class TomcatContexts {
  private static final String WEB_DEPLOY_PATH_RELATIVE_TO_DATA_DIR = "web/deploy";

  private final Fs fs;

  public TomcatContexts() {
    this.fs = new Fs();
  }

  @VisibleForTesting
  TomcatContexts(Fs fs) {
    this.fs = fs;
  }

  public StandardContext configure(Tomcat tomcat, Props props) {
    String contextPath = getContextPath(props);

    // Add ROOT context only if we have a non-empty context path
    // This catches requests outside the web context
    if (!contextPath.isEmpty()) {
      addRootContext(tomcat, contextPath, props);
    }

    addStaticDir(tomcat, contextPath + "/deploy", new File(props.nonNullValueAsFile(PATH_DATA.getKey()), WEB_DEPLOY_PATH_RELATIVE_TO_DATA_DIR));

    StandardContext webapp = addContext(tomcat, contextPath, webappDir(props));
    for (Map.Entry<Object, Object> entry : props.rawProperties().entrySet()) {
      String key = entry.getKey().toString();
      webapp.addParameter(key, entry.getValue().toString());
    }
    return webapp;
  }

  static String getContextPath(Props props) {
    String context = props.value(WEB_CONTEXT.getKey(), "");
    if ("/".equals(context)) {
      context = "";
    } else if (!"".equals(context) && context != null && !context.startsWith("/")) {
      throw MessageException.of(format("Value of '%s' must start with a forward slash: '%s'", WEB_CONTEXT.getKey(), context));
    }
    return context;
  }

  @VisibleForTesting
  StandardContext addStaticDir(Tomcat tomcat, String contextPath, File dir) {
    try {
      fs.createOrCleanupDir(dir);
      configureDeployErrorPages(dir, contextPath);
    } catch (IOException e) {
      throw new IllegalStateException(format("Fail to create or clean-up directory %s", dir.getAbsolutePath()), e);
    }

    return addContext(tomcat, contextPath, dir);
  }

  private static StandardContext addContext(Tomcat tomcat, String contextPath, File dir) {
    try {
      StandardContext context = (StandardContext) tomcat.addWebapp(contextPath, dir.getAbsolutePath());
      context.setClearReferencesHttpClientKeepAliveThread(false);
      context.setClearReferencesStopThreads(false);
      context.setClearReferencesStopTimerThreads(false);
      context.setClearReferencesStopTimerThreads(false);
      context.setAntiResourceLocking(false);
      context.setReloadable(false);
      context.setUseHttpOnly(true);
      context.setTldValidation(false);
      context.setXmlValidation(false);
      context.setXmlNamespaceAware(false);
      context.setUseNaming(false);
      context.setDelegate(true);
      context.setJarScanner(new NullJarScanner());
      context.setAllowCasualMultipartParsing(true);
      context.setCookies(false);
      // disable JSP and WebSocket support
      context.setContainerSciFilter("org.apache.tomcat.websocket.server.WsSci|org.apache.jasper.servlet.JasperInitializer");
      return context;
    } catch (Exception e) {
      throw new IllegalStateException("Fail to configure webapp from " + dir, e);
    }
  }

  private static File webappDir(Props props) {
    return new File(props.value(PATH_HOME.getKey()), "web");
  }

  private static void configureDeployErrorPages(File dir, String contextPath) throws IOException {
    // Create a simple web.xml with error page configuration that redirects to main webapp
    File webInfDir = new File(dir, "WEB-INF");
    FileUtils.forceMkdir(webInfDir);

    // Create a redirect HTML page that immediately redirects to the main context
    File errorHtmlFile = new File(dir, "error.html");
    String mainContextUrl = contextPath.replace("/deploy", "/not-found-deploy");

    String errorHtml = """
      <!DOCTYPE html>
      <html>
      <head>
        <meta charset="UTF-8">
        <meta http-equiv="refresh" content="0; url=%s">
        <script>window.location.href = '%s';</script>
      </head>
      <body></body>
      </html>""".formatted(mainContextUrl, mainContextUrl);

    FileUtils.writeStringToFile(errorHtmlFile, errorHtml, StandardCharsets.UTF_8);

    // Create web.xml with error page mapping
    File webXmlFile = new File(webInfDir, "web.xml");
    String webXml = """
      <?xml version="1.0" encoding="UTF-8"?>
      <web-app xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
               xmlns="http://java.sun.com/xml/ns/javaee"
               xsi:schemaLocation="http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_3_0.xsd"
               version="3.0"
               metadata-complete="true">
        <display-name>SonarQube Deploy</display-name>
        <error-page>
          <error-code>404</error-code>
          <location>/error.html</location>
        </error-page>
        <error-page>
          <error-code>403</error-code>
          <location>/error.html</location>
        </error-page>
        <error-page>
          <error-code>500</error-code>
          <location>/error.html</location>
        </error-page>
      </web-app>""";

    FileUtils.writeStringToFile(webXmlFile, webXml, StandardCharsets.UTF_8);
  }

  private void addRootContext(Tomcat tomcat, String webContext, Props props) {
    try {
      File rootDir = new File(props.value(PATH_DATA.getKey()), "web/root");
      fs.createOrCleanupDir(rootDir);

      File webInfDir = new File(rootDir, "WEB-INF");
      FileUtils.forceMkdir(webInfDir);

      File webXmlDest = new File(webInfDir, "web.xml");
      String webXml = """
        <?xml version="1.0" encoding="UTF-8"?>
        <web-app xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                 xmlns="http://java.sun.com/xml/ns/javaee"
                 xsi:schemaLocation="http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_3_0.xsd"
                 version="3.0"
                 metadata-complete="true">
          <display-name>SonarQube Root</display-name>
          <servlet>
            <servlet-name>root</servlet-name>
            <servlet-class>org.sonar.server.app.RootContextServlet</servlet-class>
            <init-param>
              <param-name>webContext</param-name>
              <param-value>%s</param-value>
            </init-param>
          </servlet>
          <servlet-mapping>
            <servlet-name>root</servlet-name>
            <url-pattern>/*</url-pattern>
          </servlet-mapping>
        </web-app>""".formatted(webContext);
      FileUtils.writeStringToFile(webXmlDest, webXml, StandardCharsets.UTF_8);

      addContext(tomcat, "", rootDir);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to configure ROOT context", e);
    }
  }

  static class Fs {
    void createOrCleanupDir(File dir) throws IOException {
      FileUtils.forceMkdir(dir);
      org.sonar.core.util.FileUtils.cleanDirectory(dir);
    }
  }
}
