/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.project.ws;

import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.server.component.ComponentCleanerService;
import org.sonar.server.user.UserSession;

import static org.sonar.db.permission.OrganizationPermission.ADMINISTER;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;

public class BulkDeleteAction implements ProjectsWsAction {

  private static final String ACTION = "bulk_delete";
  private static final String PARAM_PROJECT_IDS = "projectIds";
  private static final String PARAM_PROJECTS = "projects";

  private final ComponentCleanerService componentCleanerService;
  private final DbClient dbClient;
  private final UserSession userSession;
  private final ProjectsWsSupport support;

  public BulkDeleteAction(ComponentCleanerService componentCleanerService, DbClient dbClient, UserSession userSession,
    ProjectsWsSupport support) {
    this.componentCleanerService = componentCleanerService;
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.support = support;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context
      .createAction(ACTION)
      .setPost(true)
      .setDescription("Delete one or several projects.<br />" +
        "Requires 'Administer System' permission.")
      .setSince("5.2")
      .setHandler(this);

    action
      .createParam(PARAM_PROJECT_IDS)
      .setDescription("List of project IDs to delete")
      .setDeprecatedKey("ids", "6.4")
      .setDeprecatedSince("6.4")
      .setExampleValue("ce4c03d6-430f-40a9-b777-ad877c00aa4d,c526ef20-131b-4486-9357-063fa64b5079");

    action
      .createParam(PARAM_PROJECTS)
      .setDescription("List of project keys to delete")
      .setDeprecatedKey("keys", "6.4")
      .setExampleValue(KEY_PROJECT_EXAMPLE_001);

    support.addOrganizationParam(action);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkLoggedIn();

    List<String> uuids = request.paramAsStrings(PARAM_PROJECT_IDS);
    List<String> keys = request.paramAsStrings(PARAM_PROJECTS);
    String orgKey = request.param(ProjectsWsSupport.PARAM_ORGANIZATION);

    try (DbSession dbSession = dbClient.openSession(false)) {
      Optional<OrganizationDto> org = loadOrganizationByKey(dbSession, orgKey);
      List<ComponentDto> projects = searchProjects(dbSession, uuids, keys);
      projects.stream()
        .filter(p -> !org.isPresent() || org.get().getUuid().equals(p.getOrganizationUuid()))
        .forEach(p -> componentCleanerService.delete(dbSession, p));
    }

    response.noContent();
  }

  private Optional<OrganizationDto> loadOrganizationByKey(DbSession dbSession, @Nullable String orgKey) {
    if (orgKey == null) {
      userSession.checkIsSystemAdministrator();
      return Optional.empty();
    }
    OrganizationDto org = support.getOrganization(dbSession, orgKey);
    userSession.checkPermission(ADMINISTER, org);
    return Optional.of(org);
  }

  private List<ComponentDto> searchProjects(DbSession dbSession, @Nullable List<String> uuids, @Nullable List<String> keys) {
    if (uuids != null) {
      return dbClient.componentDao().selectByUuids(dbSession, uuids);
    }
    if (keys != null) {
      return dbClient.componentDao().selectByKeys(dbSession, keys);
    }

    throw new IllegalArgumentException("ids or keys must be provided");
  }
}
