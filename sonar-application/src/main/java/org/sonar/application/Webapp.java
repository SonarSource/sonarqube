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

  static void configure(Tomcat tomcat, Env env, Props props) {
    String ctx = props.of("sonar.web.context", "/");
    try {
      Context context = tomcat.addWebapp(ctx, env.file("web").getAbsolutePath());
      context.setConfigFile(env.file("web/META-INF/context.xml").toURI().toURL());
      context.setJarScanner(new NullJarScanner());

    } catch (Exception e) {
      throw new IllegalStateException("Fail to configure webapp", e);
    }
  }
}
