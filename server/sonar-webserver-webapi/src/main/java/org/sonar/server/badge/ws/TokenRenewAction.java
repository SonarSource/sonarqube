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
package org.sonar.server.badge.ws;

import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.db.permission.ProjectPermission;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.user.TokenType;
import org.sonar.server.user.UserSession;
import org.sonar.server.usertoken.TokenGenerator;

import static java.lang.String.format;
import static org.sonar.server.badge.ws.ProjectBadgesWs.PROJECT_OR_APP_NOT_FOUND;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;

public class TokenRenewAction implements ProjectBadgesWsAction {

  private static final String PROJECT_KEY_PARAM = "project";
  private final DbClient dbClient;
  private final TokenGenerator tokenGenerator;
  private final UserSession userSession;

  public TokenRenewAction(DbClient dbClient, TokenGenerator tokenGenerator, UserSession userSession) {
    this.dbClient = dbClient;
    this.tokenGenerator = tokenGenerator;
    this.userSession = userSession;
  }

  @Override
  public void define(WebService.NewController controller) {
    NewAction action = controller.createAction("renew_token")
      .setHandler(this)
      .setSince("9.2")
      .setPost(true)
      .setChangelog(new Change("10.1", format("Application key can be used for %s parameter.", PROJECT_KEY_PARAM)))
      .setDescription("Creates new token replacing any existing token for project or application badge access for private projects and " +
        "applications.<br/>" +
        "This token can be used to authenticate with api/project_badges/quality_gate and api/project_badges/measure endpoints.<br/>" +
        "Requires 'Administer' permission on the specified project or application.");
    action.createParam(PROJECT_KEY_PARAM)
      .setDescription("Project or application key")
      .setRequired(true)
      .setExampleValue(KEY_PROJECT_EXAMPLE_001);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    doHandle(request);
    response.noContent();
  }

  private void doHandle(Request request) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      String projectKey = request.mandatoryParam(PROJECT_KEY_PARAM);

      ProjectDto projectDto = dbClient.projectDao().selectProjectOrAppByKey(dbSession, projectKey)
        .orElseThrow(() -> new IllegalArgumentException(PROJECT_OR_APP_NOT_FOUND));
      userSession.checkEntityPermission(ProjectPermission.ADMIN, projectDto);
      String newGeneratedToken = tokenGenerator.generate(TokenType.PROJECT_BADGE_TOKEN);
      dbClient.projectBadgeTokenDao().upsert(dbSession, newGeneratedToken, projectDto, userSession.getUuid(), userSession.getLogin());
      dbSession.commit();
    }
  }

}
