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

import org.slf4j.LoggerFactory;
import org.sonar.process.ConfigurationUtils;
import org.sonar.process.MonitoredProcess;
import org.sonar.process.Props;

public class WebServer extends MonitoredProcess {

  private final EmbeddedTomcat tomcat;

  WebServer(Props props) throws Exception {
    super(props);
    this.tomcat = new EmbeddedTomcat(props);
  }

  @Override
  protected void doStart() {
    try {
      tomcat.start();
    } catch (Exception e) {
      LoggerFactory.getLogger(getClass()).error("TC error", e);
    } finally {
      terminate();
    }
  }

  @Override
  protected void doTerminate() {
    tomcat.terminate();
  }

  @Override
  protected boolean doIsReady() {
    return tomcat.isReady();
  }

  public static void main(String[] args) throws Exception {
    Props props = ConfigurationUtils.loadPropsFromCommandLineArgs(args);
    Logging.init(props);
    new WebServer(props).start();
  }
}
