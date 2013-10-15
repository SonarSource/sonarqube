/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.application;

import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;

class Webapp {

  private static final String JRUBY_MAX_RUNTIMES = "jruby.max.runtimes";
  private static final String RAILS_ENV = "rails.env";
  private static final String PROPERTY_CONTEXT = "sonar.web.context";

  static void configure(Tomcat tomcat, Env env, Props props) {
    String ctx = getContext(props);
    try {
      Context context = tomcat.addWebapp(ctx, env.file("web").getAbsolutePath());
      context.setConfigFile(env.file("web/META-INF/context.xml").toURI().toURL());
      configureRailsMode(props, context);
      context.setJarScanner(new NullJarScanner());

    } catch (Exception e) {
      throw new IllegalStateException("Fail to configure webapp", e);
    }
  }

  static String getContext(Props props) {
    String context = props.of(PROPERTY_CONTEXT, "");
    if ("/".equals(context)) {
      context = "";
    } else if (!"".equals(context) && !context.startsWith("/")) {
      throw new IllegalStateException(String.format("Value of '%s' must start with a forward slash: '%s'", PROPERTY_CONTEXT, context));
    }
    return context;
  }

  static void configureRailsMode(Props props, Context context) {
    if (props.booleanOf("sonar.web.dev")) {
      context.addParameter(RAILS_ENV, "development");
      context.addParameter(JRUBY_MAX_RUNTIMES, "3");
    } else {
      context.addParameter(RAILS_ENV, "production");
      context.addParameter(JRUBY_MAX_RUNTIMES, "1");
    }
  }
}
