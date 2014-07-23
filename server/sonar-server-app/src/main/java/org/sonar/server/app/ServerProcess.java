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

public class ServerProcess extends org.sonar.process.Process {

  private final EmbeddedTomcat tomcat;

  public ServerProcess(String[] args) {
    super(args);
    Logging.init();
    Env env = new Env(props);
    this.tomcat = new EmbeddedTomcat(env);
  }

  @Override
  public void onStart() {
    tomcat.start();
  }

  @Override
  public void onTerminate() {
    tomcat.stop();
  }

  @Override
  public boolean isReady() {
    return tomcat.isReady();
  }

  public static void main(String[] args) {
    new ServerProcess(args).start();
    LOGGER.info("ServerProcess is done");
  }
}
