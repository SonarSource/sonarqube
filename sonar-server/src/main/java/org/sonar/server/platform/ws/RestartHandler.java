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
package org.sonar.server.platform.ws;

import org.slf4j.LoggerFactory;
import org.sonar.api.config.Settings;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.platform.Platform;

public class RestartHandler implements RequestHandler {

  private final Settings settings;
  private final Platform platform;

  public RestartHandler(Settings settings, Platform platform) {
    this.settings = settings;
    this.platform = platform;
  }

  void define(WebService.NewController controller) {
    controller.createAction("restart")
      .setDescription("Restart server. Available only in development mode.")
      .setPost(true)
      .setHandler(this);
  }

  @Override
  public void handle(Request request, Response response) {
    if (settings.getBoolean("sonar.dev")) {
      LoggerFactory.getLogger(getClass()).info("Restart server");
      platform.restartLevel3Container();
    } else {
      throw new BadRequestException("Available in development mode only (sonar.dev=true)");
    }
  }
}
