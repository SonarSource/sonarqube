/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import com.google.common.collect.ImmutableMap;
import java.io.File;
import org.slf4j.LoggerFactory;
import org.sonar.process.MinimumViableSystem;
import org.sonar.process.Monitored;
import org.sonar.process.ProcessEntryPoint;
import org.sonar.process.ProcessId;
import org.sonar.process.Props;
import org.sonar.process.sharedmemoryfile.DefaultProcessCommands;

public class WebServer implements Monitored {
  public static final String PROPERTY_SHARED_PATH = "process.sharedDir";

  private final File sharedDir;
  private final EmbeddedTomcat tomcat;

  WebServer(Props props) {
    new MinimumViableSystem()
      .checkWritableTempDir()
      .checkRequiredJavaOptions(ImmutableMap.of("file.encoding", "UTF-8"));
    this.sharedDir = getSharedDir(props);
    this.tomcat = new EmbeddedTomcat(props);
  }

  private static File getSharedDir(Props props) {
    return props.nonNullValueAsFile(PROPERTY_SHARED_PATH);
  }

  @Override
  public void start() {
    tomcat.start();
  }

  @Override
  public Status getStatus() {
    switch (tomcat.getStatus()) {
      case DOWN:
        return Status.DOWN;
      case UP:
        return isOperational() ? Status.OPERATIONAL : Status.UP;
      case FAILED:
      default:
        return Status.FAILED;
    }
  }

  private boolean isOperational() {
    try (DefaultProcessCommands processCommands = DefaultProcessCommands.secondary(sharedDir, ProcessId.WEB_SERVER.getIpcIndex())) {
      return processCommands.isOperational();
    }
  }

  @Override
  public void stop() {
    // hard stop is as graceful as stop for the WebServer
    hardStop();
    LoggerFactory.getLogger(WebServer.class).info("WebServer stopped");
  }

  @Override
  public void hardStop() {
    tomcat.terminate();
  }

  @Override
  public void awaitStop() {
    tomcat.awaitTermination();
  }

  /**
   * Can't be started as is. Needs to be bootstrapped by sonar-application
   */
  public static void main(String[] args) {
    ProcessEntryPoint entryPoint = ProcessEntryPoint.createForArguments(args);
    Props props = entryPoint.getProps();
    new WebServerProcessLogging().configure(props);
    WebServer server = new WebServer(props);
    entryPoint.launch(server);
  }
}
