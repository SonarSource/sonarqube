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
package org.sonar.server.pushapi.sonarlint;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.http.HttpServletResponse;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.pushapi.ServerPushAction;
import org.sonar.server.user.UserSession;
import org.sonar.server.ws.ServletRequest;
import org.sonar.server.ws.ServletResponse;

public class SonarLintPushAction extends ServerPushAction {

  private static final String PROJECT_PARAM_KEY = "projectKeys";
  private static final String LANGUAGE_PARAM_KEY = "languages";
  private final SonarLintClientsRegistry clientsRegistry;
  private final SonarLintClientPermissionsValidator permissionsValidator;
  private final UserSession userSession;
  private final DbClient dbClient;
  private final SonarLintPushEventExecutorService sonarLintPushEventExecutorService;

  public SonarLintPushAction(SonarLintClientsRegistry sonarLintClientRegistry, UserSession userSession, DbClient dbClient,
    SonarLintClientPermissionsValidator permissionsValidator, SonarLintPushEventExecutorService sonarLintPushEventExecutorService) {
    this.clientsRegistry = sonarLintClientRegistry;
    this.userSession = userSession;
    this.dbClient = dbClient;
    this.permissionsValidator = permissionsValidator;
    this.sonarLintPushEventExecutorService = sonarLintPushEventExecutorService;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller
      .createAction("sonarlint_events")
      .setInternal(true)
      .setDescription("Endpoint for listening to server side events. Currently it notifies listener about change to activation of a rule")
      .setSince("9.4")
      .setContentType(Response.ContentType.NO_CONTENT)
      .setHandler(this);

    action
      .createParam(PROJECT_PARAM_KEY)
      .setDescription("Comma-separated list of projects keys for which events will be delivered")
      .setRequired(true)
      .setExampleValue("example-project-key,example-project-key2");

    action
      .createParam(LANGUAGE_PARAM_KEY)
      .setDescription("Comma-separated list of languages for which events will be delivered")
      .setRequired(true)
      .setExampleValue("java,cobol");
  }

  @Override
  public void handle(Request request, Response response) throws IOException {
    userSession.checkLoggedIn();

    ServletRequest servletRequest = (ServletRequest) request;
    ServletResponse servletResponse = (ServletResponse) response;

    var params = new SonarLintPushActionParamsValidator(request);
    params.validateParams();

    List<ProjectDto> projectDtos = permissionsValidator.validateUserCanReceivePushEventForProjects(userSession, params.projectKeys);

    if (!isServerSideEventsRequest(servletRequest)) {
      servletResponse.stream().setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
      return;
    }

    setHeadersForResponse(servletResponse);

    AsyncContext asyncContext = servletRequest.startAsync();
    asyncContext.setTimeout(0);

    Set<String> projectUuids = projectDtos.stream().map(ProjectDto::getUuid).collect(Collectors.toSet());

    SonarLintClient sonarLintClient = new SonarLintClient(sonarLintPushEventExecutorService, asyncContext, projectUuids, params.getLanguages(), userSession.getUuid());

    clientsRegistry.registerClient(sonarLintClient);
  }

  class SonarLintPushActionParamsValidator {

    private final Request request;
    private final Set<String> projectKeys;
    private final Set<String> languages;

    SonarLintPushActionParamsValidator(Request request) {
      this.request = request;
      this.projectKeys = parseParam(PROJECT_PARAM_KEY);
      this.languages = parseParam(LANGUAGE_PARAM_KEY);
    }

    Set<String> getLanguages() {
      return languages;
    }

    private Set<String> parseParam(String paramKey) {
      String paramProjectKeys = request.getParam(paramKey).getValue();
      if (paramProjectKeys == null) {
        throw new IllegalArgumentException("Param " + paramKey + " was not provided.");
      }
      return Set.of(paramProjectKeys.trim().split(","));
    }

    private void validateParams() {
      List<ProjectDto> projectDtos;
      try (DbSession dbSession = dbClient.openSession(false)) {
        projectDtos = dbClient.projectDao().selectProjectsByKeys(dbSession, projectKeys);
      }
      if (projectDtos.size() < projectKeys.size() || projectDtos.isEmpty()) {
        throw new IllegalArgumentException("Param " + PROJECT_PARAM_KEY + " is invalid.");
      }
    }

  }

}
