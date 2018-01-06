/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.platform.ws;

import com.google.common.io.Resources;
import org.apache.commons.io.IOUtils;
import org.sonar.api.platform.Server;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonarqube.ws.MediaTypes;

import static java.nio.charset.StandardCharsets.UTF_8;

public class ServerWs implements WebService, RequestHandler {

  private final Server server;

  public ServerWs(Server server) {
    this.server = server;
  }

  @Override
  public void define(Context context) {
    NewController controller = context.createController("api/server");

    controller.createAction("version")
      .setDescription("Version of SonarQube in plain text")
      .setSince("2.10")
      .setResponseExample(Resources.getResource(this.getClass(), "example-server-version.txt"))
      .setHandler(this);

    controller.done();
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    response.stream().setMediaType(MediaTypes.TXT);
    IOUtils.write(server.getVersion(), response.stream().output(), UTF_8);
  }
}
