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
import org.slf4j.LoggerFactory;
import org.sonar.process.Props;

import java.util.Map;

class Webapp {

  private static final String JRUBY_MAX_RUNTIMES = "jruby.max.runtimes";
  private static final String RAILS_ENV = "rails.env";
  private static final String PROPERTY_CONTEXT = "sonar.web.context";

  static void configure(Tomcat tomcat, Props props) {
    try {
      String webDir = props.of("sonar.path.web");
      LoggerFactory.getLogger(Webapp.class).info("Webapp directory: " + webDir);
      StandardContext context = (StandardContext) tomcat.addWebapp(getContextPath(props), webDir);
      context.setReloadable(false);
      context.setUseHttpOnly(true);
      context.setProcessTlds(false);
      context.setTldValidation(false);
      context.setTldNamespaceAware(false);
      context.setXmlValidation(false);
      context.setXmlNamespaceAware(false);
      context.setUseNaming(false);
      context.setDelegate(true);
      for (Map.Entry<Object, Object> entry : props.encryptedProperties().entrySet()) {
        String key = entry.getKey().toString();
        if (key.startsWith("sonar.")) {
          context.addParameter(key, entry.getValue().toString());
        }
      }
      configureRailsMode(props, context);
      context.setJarScanner(new NullJarScanner());

    } catch (Exception e) {
      throw new IllegalStateException("Fail to configure webapp", e);
    }
  }

  static String getContextPath(Props props) {
    String context = props.of(PROPERTY_CONTEXT, "");
    if ("/".equals(context)) {
      context = "";
    } else if (!"".equals(context) && !context.startsWith("/")) {
      throw new IllegalStateException(String.format("Value of '%s' must start with a forward slash: '%s'", PROPERTY_CONTEXT, context));
    }
    return context;
  }

  static void configureRailsMode(Props props, Context context) {
    if (props.booleanOf("sonar.rails.dev")) {
      context.addParameter(RAILS_ENV, "development");
      context.addParameter(JRUBY_MAX_RUNTIMES, "3");
      LoggerFactory.getLogger(Webapp.class).warn("\n\n\n------ RAILS DEVELOPMENT MODE IS ENABLED ------\n\n\n");
    } else {
      context.addParameter(RAILS_ENV, "production");
      context.addParameter(JRUBY_MAX_RUNTIMES, "1");
    }
  }
}
