/*
 * SonarQube :: Server
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
package org.sonar.server.platform.ws;

import java.io.File;
import org.sonar.api.config.Settings;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.web.UserRole;
import org.sonar.process.DefaultProcessCommands;
import org.sonar.process.ProcessCommands;
import org.sonar.server.platform.Platform;
import org.sonar.server.user.UserSession;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Implementation of the {@code restart} action for the System WebService.
 */
public class RestartAction implements SystemWsAction {

  private static final Logger LOGGER = Loggers.get(RestartAction.class);
  private static final String PROPERTY_SHARED_PATH = "process.sharedDir";
  private static final String PROPERTY_PROCESS_INDEX = "process.index";

  private final UserSession userSession;
  private final Settings settings;
  private final Platform platform;

  public RestartAction(UserSession userSession, Settings settings, Platform platform) {
    this.userSession = userSession;
    this.settings = settings;
    this.platform = platform;
  }

  @Override
  public void define(WebService.NewController controller) {
    controller.createAction("restart")
      .setDescription("Restart server. " +
          "In development mode (sonar.web.dev=true), performs a partial and quick restart of only the web server where " +
          "Ruby on Rails extensions are not reloaded. " +
          "In Production mode, require system administration permission and fully restart web server and Elastic Search processes.")
      .setSince("4.3")
      .setPost(true)
      .setHandler(this);
  }

  @Override
  public void handle(Request request, Response response) {
    if (settings.getBoolean("sonar.web.dev")) {
      LOGGER.info("Fast restarting WebServer...");
      platform.restart();
      LOGGER.info("WebServer restarted");
    } else {
      LOGGER.info("Requesting SonarQube restart");
      userSession.checkPermission(UserRole.ADMIN);

      File shareDir = nonNullValueAsFile(PROPERTY_SHARED_PATH);
      int processNumber = nonNullAsInt(PROPERTY_PROCESS_INDEX);
      try (ProcessCommands commands = new DefaultProcessCommands(shareDir, processNumber, false)) {
        commands.askForRestart();
      } catch (Exception e) {
        LOGGER.warn("Failed to close ProcessCommands", e);
      }
    }
    response.noContent();
  }

  private int nonNullAsInt(String key) {
    String s = settings.getString(key);
    checkArgument(s != null, "Property %s is not set", key);
    return Integer.parseInt(s);
  }

  public File nonNullValueAsFile(String key) {
    String s = settings.getString(key);
    checkArgument(s != null, "Property %s is not set", key);
    return new File(s);
  }

}
