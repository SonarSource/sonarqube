/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.server.qualityprofile.ws;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.api.server.ws.WebService.NewController;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.server.ws.WebService.SelectionMode;
import org.sonar.api.utils.Paging;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.api.web.UserRole;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.ProjectQprofileAssociationDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;

import static java.util.Comparator.comparing;
import static org.sonar.api.utils.Paging.forPageIndex;
import static org.sonar.core.util.Uuids.UUID_EXAMPLE_01;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_KEY;

public class ProjectsAction implements QProfileWsAction {

  private static final int MAX_PAGE_SIZE = 500;

  private final DbClient dbClient;
  private final UserSession userSession;
  private final QProfileWsSupport qProfileWsSupport;

  public ProjectsAction(DbClient dbClient, UserSession userSession, QProfileWsSupport qProfileWsSupport) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.qProfileWsSupport = qProfileWsSupport;
  }

  @Override
  public void define(NewController controller) {
    NewAction action = controller.createAction("projects")
      .setSince("5.2")
      .setHandler(this)
      .setDescription("List projects with their association status regarding a quality profile <br/>" +
        "See api/qualityprofiles/search in order to get the Quality Profile Key")
      .setResponseExample(getClass().getResource("projects-example.json"));

    action.setChangelog(
      new Change("8.8", "deprecated 'id' response field has been removed"),
      new Change("8.8", "deprecated 'uuid' response field has been removed"),
      new Change("7.2", "'more' response field is deprecated"),
      new Change("6.5", "'id' response field is deprecated"),
      new Change("6.0", "'uuid' response field is deprecated and replaced by 'id'"),
      new Change("6.0", "'key' response field has been added to return the project key"));

    action.createParam(PARAM_KEY)
      .setDescription("Quality profile key")
      .setRequired(true)
      .setExampleValue(UUID_EXAMPLE_01);
    action.addSelectionModeParam();

    action.createSearchQuery("sonar", "projects");

    action.createPageParam();

    action.createPageSize(100, MAX_PAGE_SIZE);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    String profileKey = request.mandatoryParam(PARAM_KEY);

    try (DbSession session = dbClient.openSession(false)) {
      String selected = request.param(Param.SELECTED);
      String query = request.param(Param.TEXT_QUERY);
      int page = request.mandatoryParamAsInt(Param.PAGE);
      int pageSize = request.mandatoryParamAsInt(Param.PAGE_SIZE);

      List<ProjectQprofileAssociationDto> projects = loadAllProjects(profileKey, session, selected, query).stream()
        .sorted(comparing(ProjectQprofileAssociationDto::getProjectName)
          .thenComparing(ProjectQprofileAssociationDto::getProjectUuid))
        .collect(MoreCollectors.toList());

      Collection<String> projectUuids = projects.stream()
        .map(ProjectQprofileAssociationDto::getProjectUuid)
        .collect(MoreCollectors.toSet());

      Set<String> authorizedProjectUuids = dbClient.authorizationDao().keepAuthorizedProjectUuids(session, projectUuids, userSession.getUuid(), UserRole.USER);
      Paging paging = forPageIndex(page).withPageSize(pageSize).andTotal(authorizedProjectUuids.size());

      List<ProjectQprofileAssociationDto> authorizedProjects = projects.stream()
        .filter(input -> authorizedProjectUuids.contains(input.getProjectUuid()))
        .skip(paging.offset())
        .limit(paging.pageSize())
        .collect(MoreCollectors.toList());

      writeProjects(response, authorizedProjects, paging);
    }
  }

  private List<ProjectQprofileAssociationDto> loadAllProjects(String profileKey, DbSession session, String selected, String query) {
    QProfileDto profile = dbClient.qualityProfileDao().selectByUuid(session, profileKey);
    OrganizationDto organization = qProfileWsSupport.getOrganization(session, profile);
    if (profile == null) {
      throw new NotFoundException("Quality profile not found: " + profileKey);
    }
    List<ProjectQprofileAssociationDto> projects;
    SelectionMode selectionMode = SelectionMode.fromParam(selected);

    if (SelectionMode.SELECTED == selectionMode) {
      projects = dbClient.qualityProfileDao().selectSelectedProjects(session, organization, profile, query);
    } else if (SelectionMode.DESELECTED == selectionMode) {
      projects = dbClient.qualityProfileDao().selectDeselectedProjects(session, organization, profile, query);
    } else {
      projects = dbClient.qualityProfileDao().selectProjectAssociations(session, organization, profile, query);
    }

    return projects;
  }

  private static void writeProjects(Response response, List<ProjectQprofileAssociationDto> projects, Paging paging) {
    JsonWriter json = response.newJsonWriter();

    json.beginObject();
    json.name("results").beginArray();
    for (ProjectQprofileAssociationDto project : projects) {
      json.beginObject()
        .prop("key", project.getProjectKey())
        .prop("name", project.getProjectName())
        .prop("selected", project.isAssociated())
        .endObject();
    }
    json.endArray();
    json.name("paging").beginObject()
      .prop("pageIndex", paging.pageIndex())
      .prop("pageSize", paging.pageSize())
      .prop("total", paging.total())
      .endObject();
    // more is deprecated since 7.2
    json.prop("more", paging.hasNextPage());
    json.endObject();
    json.close();
  }
}
