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
package org.sonar.batch.task;

import com.github.kevinsawicki.http.HttpRequest;
import java.net.MalformedURLException;
import java.net.URL;
import org.sonar.api.CoreProperties;
import org.sonar.api.platform.Server;
import org.sonar.api.task.Task;
import org.sonar.api.task.TaskDefinition;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.batch.bootstrap.ServerClient;

import static java.lang.String.format;

public class ViewsTask implements Task {

  private static final Logger LOG = Loggers.get(ViewsTask.class);

  public static final String KEY = "views";

  public static final TaskDefinition DEFINITION = TaskDefinition.builder()
    .key(KEY)
    .description("Trigger Views update")
    .taskClass(ViewsTask.class)
    .build();

  private final ServerClient serverClient;
  private final Server server;

  public ViewsTask(ServerClient serverClient, Server server) {
    this.serverClient = serverClient;
    this.server = server;
  }

  @Override
  public void execute() {
    LOG.info("Trigger Views update");
    URL url;
    try {
      url = new URL(serverClient.getURL() + "/api/views/run");
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException("Invalid URL", e);
    }
    HttpRequest request = HttpRequest.post(url);
    request.trustAllCerts();
    request.trustAllHosts();
    request.header("User-Agent", format("SonarQube %s", server.getVersion()));
    request.basic(serverClient.getLogin(), serverClient.getPassword());
    if (!request.ok()) {
      int responseCode = request.code();
      if (responseCode == 401) {
        throw new IllegalStateException(format(serverClient.getMessageWhenNotAuthorized(), CoreProperties.LOGIN, CoreProperties.PASSWORD));
      }
      if (responseCode == 409) {
        throw new IllegalStateException("A full refresh of Views is already queued or running");
      }
      if (responseCode == 403) {
        // SONAR-4397 Details are in response content
        throw new IllegalStateException(request.body());
      }
      throw new IllegalStateException(format("Fail to execute request [code=%s, url=%s]: %s", responseCode, url, request.body()));
    }
  }

}
