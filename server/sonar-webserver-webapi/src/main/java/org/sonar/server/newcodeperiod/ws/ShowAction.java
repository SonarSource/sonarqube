/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.newcodeperiod.ws;

import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.core.documentation.DocumentationLinkGenerator;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.newcodeperiod.NewCodePeriodDao;
import org.sonar.db.newcodeperiod.NewCodePeriodDto;
import org.sonar.db.newcodeperiod.NewCodePeriodType;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.NewCodePeriods;

import static java.lang.String.format;
import static org.sonar.db.permission.GlobalPermission.SCAN;
import static org.sonar.server.newcodeperiod.ws.NewCodePeriodsWsUtils.createNewCodePeriodHtmlLink;
import static org.sonar.server.user.AbstractUserSession.insufficientPrivilegesException;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.NewCodePeriods.ShowWSResponse;

public class ShowAction implements NewCodePeriodsWsAction {
  private static final String PARAM_BRANCH = "branch";
  private static final String PARAM_PROJECT = "project";

  private final DbClient dbClient;
  private final UserSession userSession;
  private final ComponentFinder componentFinder;
  private final NewCodePeriodDao newCodePeriodDao;
  private final DocumentationLinkGenerator documentationLinkGenerator;

  public ShowAction(DbClient dbClient, UserSession userSession, ComponentFinder componentFinder, NewCodePeriodDao newCodePeriodDao,
    DocumentationLinkGenerator documentationLinkGenerator) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.componentFinder = componentFinder;
    this.newCodePeriodDao = newCodePeriodDao;
    this.documentationLinkGenerator = documentationLinkGenerator;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("show")
      .setDescription("Shows the " + createNewCodePeriodHtmlLink(documentationLinkGenerator) + ".<br> " +
        "If the component requested doesn't exist or if no new code definition is set for it, a value is inherited from the project or from the global setting." +
        "Requires one of the following permissions if a component is specified: " +
        "<ul>" +
        "<li>'Administer' rights on the specified component</li>" +
        "<li>'Execute analysis' rights on the specified component</li>" +
        "</ul>")
      .setSince("8.0")
      .setResponseExample(getClass().getResource("show-example.json"))
      .setHandler(this);

    action.createParam(PARAM_PROJECT)
      .setDescription("Project key");
    action.createParam(PARAM_BRANCH)
      .setDescription("Branch key");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    String projectKey = request.getParam(PARAM_PROJECT).emptyAsNull().or(() -> null);
    String branchKey = request.getParam(PARAM_BRANCH).emptyAsNull().or(() -> null);

    if (projectKey == null && branchKey != null) {
      throw new IllegalArgumentException("If branch key is specified, project key needs to be specified too");
    }

    try (DbSession dbSession = dbClient.openSession(false)) {
      ProjectDto project = null;
      BranchDto branch = null;
      boolean inherited = false;

      if (projectKey != null) {
        try {
          project = getProject(dbSession, projectKey);
          checkPermission(project);
          if (branchKey != null) {
            try {
              branch = getBranch(dbSession, project, branchKey);
            } catch (NotFoundException e) {
              inherited = true;
            }
          }
        } catch (NotFoundException e) {
          inherited = true;
        }
      }

      ShowWSResponse.Builder builder = get(dbSession, project, branch, inherited);

      if (project != null) {
        builder.setProjectKey(project.getKey());
      }
      if (branch != null) {
        builder.setBranchKey(branch.getKey());
      }
      writeProtobuf(builder.build(), request, response);
    }
  }

  private void checkPermission(ProjectDto project) {
    if (userSession.hasEntityPermission(UserRole.SCAN, project) ||
      userSession.hasEntityPermission(UserRole.ADMIN, project) ||
      userSession.hasPermission(SCAN)) {
      return;
    }
    throw insufficientPrivilegesException();
  }

  private ShowWSResponse.Builder get(DbSession dbSession, @Nullable ProjectDto project, @Nullable BranchDto branch, boolean inherited) {
    if (project == null) {
      Optional<NewCodePeriodDto> dto = newCodePeriodDao.selectGlobal(dbSession);
      return dto.map(d -> build(d, inherited))
        .orElseGet(() -> buildDefault(inherited));
    }
    if (branch == null) {
      Optional<NewCodePeriodDto> dto = newCodePeriodDao.selectByProject(dbSession, project.getUuid());
      return dto.map(d -> build(d, inherited))
        .orElseGet(() -> get(dbSession, null, null, true));
    }

    Optional<NewCodePeriodDto> dto = newCodePeriodDao.selectByBranch(dbSession, project.getUuid(), branch.getUuid());
    return dto.map(d -> build(d, inherited))
      .orElseGet(() -> get(dbSession, project, null, true));
  }

  private static ShowWSResponse.Builder build(NewCodePeriodDto dto, boolean inherited) {
    ShowWSResponse.Builder builder = ShowWSResponse.newBuilder()
      .setType(convertType(dto.getType()))
      .setUpdatedAt(dto.getUpdatedAt())
      .setInherited(inherited);

    if (dto.getValue() != null) {
      builder.setValue(dto.getValue());
    }
    if (dto.getPreviousNonCompliantValue() != null) {
      builder.setPreviousNonCompliantValue(dto.getPreviousNonCompliantValue());
    }
    return builder;
  }

  private static ShowWSResponse.Builder buildDefault(boolean inherited) {
    return ShowWSResponse.newBuilder()
      .setType(convertType(NewCodePeriodType.PREVIOUS_VERSION))
      .setInherited(inherited);
  }

  private static NewCodePeriods.NewCodePeriodType convertType(NewCodePeriodType type) {
    switch (type) {
      case NUMBER_OF_DAYS:
        return NewCodePeriods.NewCodePeriodType.NUMBER_OF_DAYS;
      case PREVIOUS_VERSION:
        return NewCodePeriods.NewCodePeriodType.PREVIOUS_VERSION;
      case SPECIFIC_ANALYSIS:
        return NewCodePeriods.NewCodePeriodType.SPECIFIC_ANALYSIS;
      case REFERENCE_BRANCH:
        return NewCodePeriods.NewCodePeriodType.REFERENCE_BRANCH;
      default:
        throw new IllegalStateException("Unexpected type: " + type);
    }
  }

  private BranchDto getBranch(DbSession dbSession, ProjectDto project, String branchKey) {
    return dbClient.branchDao().selectByBranchKey(dbSession, project.getUuid(), branchKey)
      .orElseThrow(() -> new NotFoundException(format("Branch '%s' in project '%s' not found", branchKey, project.getKey())));
  }

  private ProjectDto getProject(DbSession dbSession, String projectKey) {
    return componentFinder.getProjectByKey(dbSession, projectKey);
  }

}
