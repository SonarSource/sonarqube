/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.app;

import org.apache.catalina.Context;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.utils.log.Loggers;
import org.sonar.process.ProcessProperties;
import org.sonar.process.Props;

import java.io.File;
import java.util.Map;

/**
 * Configures webapp into Tomcat
 */
class Webapp {

  private static final String JRUBY_MAX_RUNTIMES = "jruby.max.runtimes";
  private static final String RAILS_ENV = "rails.env";
  private static final String PROPERTY_CONTEXT = "sonar.web.context";

  private Webapp() {
  }

  static StandardContext configure(Tomcat tomcat, Props props) {
    try {
      StandardContext context = (StandardContext) tomcat.addWebapp(getContextPath(props), webappPath(props));
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
      configureRails(props, context);

      for (Map.Entry<Object, Object> entry : props.rawProperties().entrySet()) {
        String key = entry.getKey().toString();
        context.addParameter(key, entry.getValue().toString());
      }
      return context;

    } catch (Exception e) {
      throw new IllegalStateException("Fail to configure webapp", e);
    }
  }

  static String getContextPath(Props props) {
    String context = props.value(PROPERTY_CONTEXT, "");
    if ("/".equals(context)) {
      context = "";
    } else if (!"".equals(context) && !context.startsWith("/")) {
      throw new IllegalStateException(String.format("Value of '%s' must start with a forward slash: '%s'", PROPERTY_CONTEXT, context));
    }
    return context;
  }

  static void configureRails(Props props, Context context) {
    // sonar.dev is kept for backward-compatibility
    if (props.valueAsBoolean("sonar.dev", false)) {
      props.set("sonar.web.dev", "true");
    }
    if (props.valueAsBoolean("sonar.web.dev", false)) {
      context.addParameter(RAILS_ENV, "development");
      context.addParameter(JRUBY_MAX_RUNTIMES, "3");
      Loggers.get(Webapp.class).warn("WEB DEVELOPMENT MODE IS ENABLED - DO NOT USE FOR PRODUCTION USAGE");
    } else {
      context.addParameter(RAILS_ENV, "production");
      context.addParameter(JRUBY_MAX_RUNTIMES, "1");
    }
  }

  static String webappPath(Props props) {
    String webDir = props.value("sonar.web.dev.sources");
    if (StringUtils.isEmpty(webDir)) {
      webDir = new File(props.value(ProcessProperties.PATH_HOME), "web").getAbsolutePath();
    }
    Loggers.get(Webapp.class).info(String.format("Webapp directory: %s", webDir));
    return webDir;
  }
}
