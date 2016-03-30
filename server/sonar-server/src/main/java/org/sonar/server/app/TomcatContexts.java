/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import java.util.Map;
import javax.servlet.ServletException;
import org.apache.catalina.Context;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;
import org.apache.commons.io.FileUtils;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.log.Loggers;
import org.sonar.process.ProcessProperties;
import org.sonar.process.Props;

import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.isEmpty;

/**
 * Configures Tomcat contexts:
 * <ul>
 *   <li>/deploy delivers the plugins required by analyzers. It maps directory ${sonar.path.data}/web/deploy.</li>
 *   <li>/ is the regular webapp</li>
 * </ul>
 */
public class TomcatContexts {

  private static final String JRUBY_MAX_RUNTIMES = "jruby.max.runtimes";
  private static final String RAILS_ENV = "rails.env";
  public static final String PROPERTY_CONTEXT = "sonar.web.context";
  public static final String WEB_DEPLOY_PATH_RELATIVE_TO_DATA_DIR = "web/deploy";

  private final Fs fs;

  public TomcatContexts() {
    this.fs = new Fs();
  }

  @VisibleForTesting
  TomcatContexts(Fs fs) {
    this.fs = fs;
  }

  public StandardContext configure(Tomcat tomcat, Props props) {
    addStaticDir(tomcat, getContextPath(props) + "/deploy", new File(props.nonNullValueAsFile(ProcessProperties.PATH_DATA), WEB_DEPLOY_PATH_RELATIVE_TO_DATA_DIR));

    StandardContext webapp = addContext(tomcat, getContextPath(props), webappDir(props));
    configureRails(props, webapp);
    for (Map.Entry<Object, Object> entry : props.rawProperties().entrySet()) {
      String key = entry.getKey().toString();
      webapp.addParameter(key, entry.getValue().toString());
    }
    return webapp;
  }

  static String getContextPath(Props props) {
    String context = props.value(PROPERTY_CONTEXT, "");
    if ("/".equals(context)) {
      context = "";
    } else if (!"".equals(context) && context != null && !context.startsWith("/")) {
      throw MessageException.of(format("Value of '%s' must start with a forward slash: '%s'", PROPERTY_CONTEXT, context));
    }
    return context;
  }

  @VisibleForTesting
  StandardContext addStaticDir(Tomcat tomcat, String contextPath, File dir) {
    try {
      fs.createOrCleanupDir(dir);
    } catch (IOException e) {
      throw new IllegalStateException(format("Fail to create or clean-up directory %s", dir.getAbsolutePath()), e);
    }

    return addContext(tomcat, contextPath, dir);
  }

  private static StandardContext addContext(Tomcat tomcat, String contextPath, File dir) {
    try {
      StandardContext context = (StandardContext) tomcat.addWebapp(contextPath, dir.getAbsolutePath());
      context.setClearReferencesHttpClientKeepAliveThread(false);
      context.setClearReferencesStatic(false);
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
      return context;
    } catch (ServletException e) {
      throw new IllegalStateException("Fail to configure webapp from " + dir, e);
    }
  }

  static void configureRails(Props props, Context context) {
    // sonar.dev is kept for backward-compatibility
    if (props.valueAsBoolean("sonar.dev", false)) {
      props.set("sonar.web.dev", "true");
    }
    if (props.valueAsBoolean("sonar.web.dev", false)) {
      context.addParameter(RAILS_ENV, "development");
      context.addParameter(JRUBY_MAX_RUNTIMES, "3");
      Loggers.get(TomcatContexts.class).warn("WEB DEVELOPMENT MODE IS ENABLED - DO NOT USE FOR PRODUCTION USAGE");
    } else {
      context.addParameter(RAILS_ENV, "production");
      context.addParameter(JRUBY_MAX_RUNTIMES, "1");
    }
  }

  static File webappDir(Props props) {
    String devDir = props.value("sonar.web.dev.sources");
    File dir;
    if (isEmpty(devDir)) {
      dir = new File(props.value(ProcessProperties.PATH_HOME), "web");
    } else {
      dir = new File(devDir);
    }
    Loggers.get(TomcatContexts.class).info("Webapp directory: {}", dir);
    return dir;
  }

  static class Fs {
    void createOrCleanupDir(File dir) throws IOException {
      FileUtils.forceMkdir(dir);
      org.sonar.core.util.FileUtils.cleanDirectory(dir);
    }
  }
}
