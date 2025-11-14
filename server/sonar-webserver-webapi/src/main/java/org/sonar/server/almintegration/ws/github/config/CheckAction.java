/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.almintegration.ws.github.config;

import com.google.gson.GsonBuilder;
import java.nio.charset.StandardCharsets;
import jakarta.servlet.http.HttpServletResponse;
import org.sonar.alm.client.github.config.ConfigCheckResult;
import org.sonar.alm.client.github.config.GithubProvisioningConfigValidator;
import org.sonar.api.server.ServerSide;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.almintegration.ws.github.GithubProvisioningAction;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.MediaTypes;

@ServerSide
public class CheckAction implements GithubProvisioningAction {

  private final UserSession userSession;
  private final GithubProvisioningConfigValidator githubProvisioningConfigValidator;

  public CheckAction(UserSession userSession, GithubProvisioningConfigValidator githubProvisioningConfigValidator) {
    this.userSession = userSession;
    this.githubProvisioningConfigValidator = githubProvisioningConfigValidator;
  }

  @Override
  public void define(WebService.NewController controller) {
    controller
      .createAction("check")
      .setPost(true)
      .setDescription("Validate Github provisioning configuration.")
      .setHandler(this)
      .setInternal(true)
      .setResponseExample(getClass().getResource("check-example.json"))
      .setSince("10.1");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkIsSystemAdministrator();

    ConfigCheckResult result = githubProvisioningConfigValidator.checkConfig();

    response.stream().setStatus(HttpServletResponse.SC_OK);
    response.stream().setMediaType(MediaTypes.JSON);
    response.stream().output().write(new GsonBuilder().create().toJson(result).getBytes(StandardCharsets.UTF_8));
    response.stream().output().flush();
  }
}
