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
package org.sonar.server.newcodeperiod.ws;

import java.util.EnumSet;
import java.util.Set;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.core.documentation.DocumentationLinkGenerator;
import org.sonar.core.platform.EditionProvider;
import org.sonar.core.platform.PlatformEditionProvider;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.newcodeperiod.NewCodePeriodDao;
import org.sonar.db.newcodeperiod.NewCodePeriodDto;
import org.sonar.db.newcodeperiod.NewCodePeriodType;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.newcodeperiod.CaycUtils;
import org.sonar.server.user.UserSession;

import static java.lang.String.format;
import static org.sonar.db.newcodeperiod.NewCodePeriodType.NUMBER_OF_DAYS;
import static org.sonar.db.newcodeperiod.NewCodePeriodType.PREVIOUS_VERSION;
import static org.sonar.db.newcodeperiod.NewCodePeriodType.REFERENCE_BRANCH;
import static org.sonar.db.newcodeperiod.NewCodePeriodType.SPECIFIC_ANALYSIS;
import static org.sonar.server.newcodeperiod.NewCodePeriodUtils.NEW_CODE_PERIOD_TYPE_DESCRIPTION;
import static org.sonar.server.newcodeperiod.NewCodePeriodUtils.NEW_CODE_PERIOD_VALUE_DESCRIPTION;
import static org.sonar.server.newcodeperiod.NewCodePeriodUtils.getNewCodeDefinitionValue;
import static org.sonar.server.newcodeperiod.NewCodePeriodUtils.validateType;
import static org.sonar.server.ws.WsUtils.createHtmlExternalLink;

public class SetAction implements NewCodePeriodsWsAction {
  private static final String PARAM_BRANCH = "branch";
  private static final String PARAM_PROJECT = "project";
  private static final String PARAM_TYPE = "type";
  private static final String PARAM_VALUE = "value";
  private static final String BEGIN_LIST = "<ul>";
  private static final String END_LIST = "</ul>";
  private static final String BEGIN_ITEM_LIST = "<li>";
  private static final String END_ITEM_LIST = "</li>";

  private static final Set<NewCodePeriodType> OVERALL_TYPES = EnumSet.of(PREVIOUS_VERSION, NUMBER_OF_DAYS);
  private static final Set<NewCodePeriodType> PROJECT_TYPES = EnumSet.of(PREVIOUS_VERSION, NUMBER_OF_DAYS, REFERENCE_BRANCH);
  private static final Set<NewCodePeriodType> BRANCH_TYPES = EnumSet.of(PREVIOUS_VERSION, NUMBER_OF_DAYS, SPECIFIC_ANALYSIS, REFERENCE_BRANCH);

  private final DbClient dbClient;
  private final UserSession userSession;
  private final ComponentFinder componentFinder;
  private final PlatformEditionProvider editionProvider;
  private final NewCodePeriodDao newCodePeriodDao;
  private final String newCodeDefinitionDocumentationUrl;

  public SetAction(DbClient dbClient, UserSession userSession, ComponentFinder componentFinder, PlatformEditionProvider editionProvider,
    NewCodePeriodDao newCodePeriodDao, DocumentationLinkGenerator documentationLinkGenerator) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.componentFinder = componentFinder;
    this.editionProvider = editionProvider;
    this.newCodePeriodDao = newCodePeriodDao;
    this.newCodeDefinitionDocumentationUrl = documentationLinkGenerator.getDocumentationLink("/project-administration/defining-new-code/");
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("set")
      .setPost(true)
      .setDescription("Updates the " + createHtmlExternalLink(newCodeDefinitionDocumentationUrl, "new code definition") +
        " on different levels:<br>" +
        BEGIN_LIST +
        BEGIN_ITEM_LIST + "Not providing a project key and a branch key will update the default value at global level. " +
        "Existing projects or branches having a specific new code definition will not be impacted" + END_ITEM_LIST +
        BEGIN_ITEM_LIST + "Project key must be provided to update the value for a project" + END_ITEM_LIST +
        BEGIN_ITEM_LIST + "Both project and branch keys must be provided to update the value for a branch" + END_ITEM_LIST +
        BEGIN_ITEM_LIST + "New setting must be compliant with the Clean as You Code methodology" + END_ITEM_LIST +
        END_LIST +
        "Requires one of the following permissions: " +
        BEGIN_LIST +
        BEGIN_ITEM_LIST + "'Administer System' to change the global setting" + END_ITEM_LIST +
        BEGIN_ITEM_LIST + "'Administer' rights on the specified project to change the project setting" + END_ITEM_LIST +
        END_LIST)
      .setSince("8.0")
      .setHandler(this);

    action.createParam(PARAM_PROJECT)
      .setDescription("Project key");
    action.createParam(PARAM_BRANCH)
      .setDescription("Branch key");
    action.createParam(PARAM_TYPE)
      .setRequired(true)
      .setDescription(NEW_CODE_PERIOD_VALUE_DESCRIPTION);
    action.createParam(PARAM_VALUE)
      .setDescription(NEW_CODE_PERIOD_TYPE_DESCRIPTION);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    String projectKey = request.getParam(PARAM_PROJECT).emptyAsNull().or(() -> null);
    String branchKey = request.getParam(PARAM_BRANCH).emptyAsNull().or(() -> null);

    if (projectKey == null && branchKey != null) {
      throw new IllegalArgumentException("If branch key is specified, project key needs to be specified too");
    }

    try (DbSession dbSession = dbClient.openSession(false)) {
      String typeStr = request.mandatoryParam(PARAM_TYPE);
      String valueStr = request.getParam(PARAM_VALUE).emptyAsNull().or(() -> null);
      boolean isCommunityEdition = editionProvider.get().filter(t -> t == EditionProvider.Edition.COMMUNITY).isPresent();

      NewCodePeriodType type = validateType(typeStr, projectKey == null, branchKey != null || isCommunityEdition);

      NewCodePeriodDto dto = new NewCodePeriodDto();
      dto.setType(type);

      ProjectDto project = null;
      BranchDto branch = null;

      if (projectKey != null) {
        project = getProject(dbSession, projectKey);
        userSession.checkProjectPermission(UserRole.ADMIN, project);

        if (branchKey != null) {
          branch = getBranch(dbSession, project, branchKey);
          dto.setBranchUuid(branch.getUuid());
        } else if (isCommunityEdition) {
          // in CE set main branch value instead of project value
          branch = getMainBranch(dbSession, project);
          dto.setBranchUuid(branch.getUuid());
        }

        dto.setProjectUuid(project.getUuid());
      } else {
        userSession.checkIsSystemAdministrator();
      }

      getNewCodeDefinitionValue(dbSession, dbClient, type, project, branch, valueStr).ifPresent(dto::setValue);

      if (!CaycUtils.isNewCodePeriodCompliant(dto.getType(), dto.getValue())) {
        throw new IllegalArgumentException("Failed to set the New Code Definition. The given value is not compatible with the Clean as You Code methodology. "
          + "Please refer to the documentation for compliant options.");
      }

      newCodePeriodDao.upsert(dbSession, dto);
      dbSession.commit();
    }
  }

  private BranchDto getBranch(DbSession dbSession, ProjectDto project, String branchKey) {
    return dbClient.branchDao().selectByBranchKey(dbSession, project.getUuid(), branchKey)
      .orElseThrow(() -> new NotFoundException(format("Branch '%s' in project '%s' not found", branchKey, project.getKey())));
  }

  private ProjectDto getProject(DbSession dbSession, String projectKey) {
    return componentFinder.getProjectByKey(dbSession, projectKey);
  }

  private BranchDto getMainBranch(DbSession dbSession, ProjectDto project) {
    return dbClient.branchDao().selectByProject(dbSession, project)
      .stream().filter(BranchDto::isMain)
      .findFirst()
      .orElseThrow(() -> new NotFoundException(format("Main branch in project '%s' is not found", project.getKey())));
  }
}
