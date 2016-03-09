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

import com.google.common.collect.ImmutableMap;
import org.sonar.process.MinimumViableSystem;
import org.sonar.process.Monitored;
import org.sonar.process.ProcessEntryPoint;
import org.sonar.process.Props;

public class WebServer implements Monitored {
  private static final String LOG_LEVEL_PROPERTY = "sonar.log.level";

  private final EmbeddedTomcat tomcat;

  WebServer(Props props) {
    new MinimumViableSystem()
      .checkJavaVersion()
      .checkWritableTempDir()
      .checkRequiredJavaOptions(ImmutableMap.of("file.encoding", "UTF-8"));
    this.tomcat = new EmbeddedTomcat(props);
  }

  @Override
  public void start() {
    tomcat.start();
  }

  @Override
  public boolean isUp() {
    return tomcat.isUp();
  }

  @Override
  public void stop() {
    tomcat.terminate();
  }

  @Override
  public void awaitStop() {
    tomcat.awaitTermination();
  }

  /**
   * Can't be started as is. Needs to be bootstrapped by sonar-application
   */
  public static void main(String[] args) throws Exception {
    ProcessEntryPoint entryPoint = ProcessEntryPoint.createForArguments(args);
    Props props = entryPoint.getProps();
    new ServerProcessLogging("web", LOG_LEVEL_PROPERTY).configure(props);
    WebServer server = new WebServer(props);
    entryPoint.launch(server);
  }
}
