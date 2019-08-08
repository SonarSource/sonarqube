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
package org.sonar.server.platform.ws;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.IOUtils.write;

public class PingAction implements SystemWsAction {
  @Override
  public void define(WebService.NewController controller) {
    controller.createAction("ping")
      .setDescription("Answers \"pong\" as plain-text")
      .setSince("6.3")
      .setResponseExample(getClass().getResource("ping-example.txt"))
      .setHandler(this);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    response.stream().setMediaType("text/plain");
    write("pong", response.stream().output(), UTF_8);
  }
}
