/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import javax.annotation.Nullable;
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
import org.sonar.db.project.ProjectDto;
import org.sonar.server.common.newcodeperiod.CaycUtils;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;

import static java.lang.String.format;
import static org.sonar.server.newcodeperiod.ws.NewCodePeriodsWsUtils.createNewCodePeriodHtmlLink;

public class UnsetAction implements NewCodePeriodsWsAction {
  private static final String BRANCH = "branch";
  private static final String PROJECT = "project";
  private static final String INSTANCE = "instance";
  private static final String NON_COMPLIANT_CAYC_ERROR_MESSAGE = "Failed to unset the New Code Definition. Your %s " +
    "New Code Definition is not compatible with the Clean as You Code methodology. Please update your %s New Code Definition";

  private final DbClient dbClient;
  private final UserSession userSession;
  private final ComponentFinder componentFinder;
  private final PlatformEditionProvider editionProvider;
  private final NewCodePeriodDao newCodePeriodDao;
  private final DocumentationLinkGenerator documentationLinkGenerator;

  public UnsetAction(DbClient dbClient, UserSession userSession, ComponentFinder componentFinder,
    PlatformEditionProvider editionProvider, NewCodePeriodDao newCodePeriodDao, DocumentationLinkGenerator documentationLinkGenerator) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.componentFinder = componentFinder;
    this.editionProvider = editionProvider;
    this.newCodePeriodDao = newCodePeriodDao;
    this.documentationLinkGenerator = documentationLinkGenerator;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("unset")
      .setPost(true)
      .setDescription("Unsets the " + createNewCodePeriodHtmlLink(documentationLinkGenerator) +
        " for a branch, project or global. It requires the inherited New Code Definition to be compatible with the Clean as You Code methodology, " +
        "and one of the following permissions: " +
        "<ul>" +
        "<li>'Administer System' to change the global setting</li>" +
        "<li>'Administer' rights for a specified component</li>" +
        "</ul>")
      .setSince("8.0")
      .setHandler(this);

    action.createParam(PROJECT)
      .setDescription("Project key");
    action.createParam(BRANCH)
      .setDescription("Branch key");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    String projectKey = request.getParam(PROJECT).emptyAsNull().or(() -> null);
    String branchKey = request.getParam(BRANCH).emptyAsNull().or(() -> null);

    if (projectKey == null && branchKey != null) {
      throw new IllegalArgumentException("If branch key is specified, project key needs to be specified too");
    }

    try (DbSession dbSession = dbClient.openSession(false)) {
      String projectUuid = null;
      String branchUuid = null;

      // in CE set main branch value instead of project value
      boolean isCommunityEdition = editionProvider.get().filter(t -> t == EditionProvider.Edition.COMMUNITY).isPresent();

      if (projectKey != null) {
        ProjectDto project = getProject(dbSession, projectKey);
        userSession.checkEntityPermission(UserRole.ADMIN, project);
        projectUuid = project.getUuid();

        if (branchKey != null) {
          branchUuid = getBranch(dbSession, project, branchKey).getUuid();
        } else if (isCommunityEdition) {
          branchUuid = getMainBranch(dbSession, project).getUuid();
        }

        checkInheritedNcdCompliant(dbSession, projectUuid, branchUuid);

      } else {
        userSession.checkIsSystemAdministrator();
      }

      newCodePeriodDao.delete(dbSession, projectUuid, branchUuid);
      if (isCommunityEdition && projectUuid != null) {
        // also delete project default in case it was somehow set (downgrade from another edition, for example)
        newCodePeriodDao.delete(dbSession, projectUuid, null);
      }
      dbSession.commit();
    }
  }

  private void checkInheritedNcdCompliant(DbSession dbSession, String projectUuid, @Nullable String branchUuid) {
    if (branchUuid != null) {
      if (dbClient.newCodePeriodDao().selectByBranch(dbSession, projectUuid, branchUuid).isPresent()) {
        checkBranchInheritedNcdCompliant(dbSession, projectUuid);
      }
    } else if (dbClient.newCodePeriodDao().selectByProject(dbSession, projectUuid).isPresent()) {
      checkInstanceNcdCompliant(dbSession);
    }
  }

  private BranchDto getMainBranch(DbSession dbSession, ProjectDto project) {
    return dbClient.branchDao().selectByProject(dbSession, project)
      .stream().filter(BranchDto::isMain)
      .findFirst()
      .orElseThrow(() -> new NotFoundException(format("Main branch in project '%s' not found", project.getKey())));
  }

  private BranchDto getBranch(DbSession dbSession, ProjectDto project, String branchKey) {
    return dbClient.branchDao().selectByBranchKey(dbSession, project.getUuid(), branchKey)
      .orElseThrow(() -> new NotFoundException(format("Branch '%s' in project '%s' not found", branchKey, project.getKey())));
  }

  private ProjectDto getProject(DbSession dbSession, String projectKey) {
    return componentFinder.getProjectByKey(dbSession, projectKey);
  }

  private void checkInstanceNcdCompliant(DbSession dbSession) {
    var instanceNcd = newCodePeriodDao.selectGlobal(dbSession);
    if (instanceNcd.isPresent()) {
      var ncd = instanceNcd.get();
      if (!CaycUtils.isNewCodePeriodCompliant(ncd.getType(), ncd.getValue())) {
        throw new IllegalArgumentException(format(NON_COMPLIANT_CAYC_ERROR_MESSAGE, INSTANCE, INSTANCE));
      }
    }
  }

  private void checkBranchInheritedNcdCompliant(DbSession dbSession, String projectUuid) {
    var projectNcd = newCodePeriodDao.selectByProject(dbSession, projectUuid);
    if (projectNcd.isPresent()) {
      var ncd = projectNcd.get();
      if (!CaycUtils.isNewCodePeriodCompliant(ncd.getType(), ncd.getValue())) {
        throw new IllegalArgumentException(format(NON_COMPLIANT_CAYC_ERROR_MESSAGE, PROJECT, PROJECT));
      }
    } else {
      checkInstanceNcdCompliant(dbSession);
    }
  }

}
