/*
 * SonarQube
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
package org.sonar.server.qualityprofile.ws;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.apache.commons.lang.builder.CompareToBuilder;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.api.server.ws.WebService.NewController;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.server.ws.WebService.SelectionMode;
import org.sonar.api.utils.Paging;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.api.web.UserRole;
import org.sonar.core.util.NonNullInputFunction;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.ProjectQprofileAssociationDto;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;

import static org.sonar.api.utils.Paging.forPageIndex;

public class ProjectsAction implements QProfileWsAction {

  private static final String PARAM_KEY = "key";
  private static final String PARAM_QUERY = "query";
  private static final String PARAM_PAGE_SIZE = "pageSize";
  private static final String PARAM_PAGE = "page";

  private final DbClient dbClient;
  private final UserSession userSession;

  public ProjectsAction(DbClient dbClient, UserSession userSession) {
    this.dbClient = dbClient;
    this.userSession = userSession;
  }

  @Override
  public void define(NewController controller) {
    NewAction projects = controller.createAction("projects")
      .setSince("5.2")
      .setHandler(this)
      .setDescription("List projects with their association status regarding a quality profile.")
      .setResponseExample(getClass().getResource("example-projects.json"));
    projects.createParam(PARAM_KEY)
      .setDescription("A quality profile key.")
      .setRequired(true)
      .setExampleValue("sonar-way-java-12345");
    projects.addSelectionModeParam();
    projects.createParam(PARAM_QUERY)
      .setDescription("If specified, return only projects whose name match the query.");
    projects.createParam(PARAM_PAGE_SIZE)
      .setDescription("Size for the paging to apply.").setDefaultValue(100);
    projects.createParam(PARAM_PAGE)
      .setDescription("Index of the page to display.").setDefaultValue(1);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    String profileKey = request.mandatoryParam(PARAM_KEY);

    DbSession session = dbClient.openSession(false);

    try {
      checkProfileExists(profileKey, session);
      String selected = request.param(Param.SELECTED);
      String query = request.param(PARAM_QUERY);
      int pageSize = request.mandatoryParamAsInt(PARAM_PAGE_SIZE);
      int page = request.mandatoryParamAsInt(PARAM_PAGE);

      List<ProjectQprofileAssociationDto> projects = loadProjects(profileKey, session, selected, query);
      Collections.sort(projects, new Comparator<ProjectQprofileAssociationDto>() {
        @Override
        public int compare(ProjectQprofileAssociationDto o1, ProjectQprofileAssociationDto o2) {
          return new CompareToBuilder()
            // First, sort by name
            .append(o1.getProjectName(), o2.getProjectName())
            // Then by UUID to disambiguate
            .append(o1.getProjectUuid(), o2.getProjectUuid())
            .toComparison();
        }
      });

      Collection<Long> projectIds = Collections2.transform(projects, new NonNullInputFunction<ProjectQprofileAssociationDto, Long>() {
        @Override
        protected Long doApply(ProjectQprofileAssociationDto input) {
          return input.getProjectId();
        }
      });

      final Collection<Long> authorizedProjectIds = dbClient.authorizationDao().keepAuthorizedProjectIds(session, projectIds, userSession.getUserId(), UserRole.USER);
      Iterable<ProjectQprofileAssociationDto> authorizedProjects = Iterables.filter(projects, new Predicate<ProjectQprofileAssociationDto>() {
        @Override
        public boolean apply(ProjectQprofileAssociationDto input) {
          return authorizedProjectIds.contains(input.getProjectId());
        }
      });

      Paging paging = forPageIndex(page).withPageSize(pageSize).andTotal(authorizedProjectIds.size());

      List<ProjectQprofileAssociationDto> pagedAuthorizedProjects = Lists.newArrayList(authorizedProjects);
      if (pagedAuthorizedProjects.size() <= paging.offset()) {
        pagedAuthorizedProjects = Lists.newArrayList();
      } else if (pagedAuthorizedProjects.size() > paging.pageSize()) {
        int endIndex = Math.min(paging.offset() + pageSize, pagedAuthorizedProjects.size());
        pagedAuthorizedProjects = pagedAuthorizedProjects.subList(paging.offset(), endIndex);
      }

      writeProjects(response.newJsonWriter(), pagedAuthorizedProjects, paging);
    } finally {
      session.close();
    }
  }

  private void checkProfileExists(String profileKey, DbSession session) {
    if (dbClient.qualityProfileDao().selectByKey(session, profileKey) == null) {
      throw new NotFoundException(String.format("Could not find a quality profile with key '%s'", profileKey));
    }
  }

  private List<ProjectQprofileAssociationDto> loadProjects(String profileKey, DbSession session, String selected, String query) {
    List<ProjectQprofileAssociationDto> projects = Lists.newArrayList();
    SelectionMode selectionMode = SelectionMode.fromParam(selected);
    if (SelectionMode.SELECTED == selectionMode) {
      projects.addAll(dbClient.qualityProfileDao().selectSelectedProjects(profileKey, query, session));
    } else if (SelectionMode.DESELECTED == selectionMode) {
      projects.addAll(dbClient.qualityProfileDao().selectDeselectedProjects(profileKey, query, session));
    } else {
      projects.addAll(dbClient.qualityProfileDao().selectProjectAssociations(profileKey, query, session));
    }
    return projects;
  }

  private void writeProjects(JsonWriter json, List<ProjectQprofileAssociationDto> projects, Paging paging) {
    json.beginObject();
    json.name("results").beginArray();
    for (ProjectQprofileAssociationDto project : projects) {
      json.beginObject()
        .prop("uuid", project.getProjectUuid())
        .prop("name", project.getProjectName())
        .prop("selected", project.isAssociated())
        .endObject();
    }
    json.endArray();
    json.prop("more", paging.hasNextPage());
    json.endObject().close();
  }
}
