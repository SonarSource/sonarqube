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
package org.sonar.server.computation.queue;

import com.google.common.base.Optional;
import java.io.InputStream;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.server.ServerSide;
import org.sonar.ce.queue.CeQueue;
import org.sonar.ce.queue.CeTask;
import org.sonar.ce.queue.CeTaskSubmit;
import org.sonar.core.component.ComponentKeys;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.ce.CeTaskTypes;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.component.ComponentService;
import org.sonar.server.favorite.FavoriteUpdater;
import org.sonar.server.component.NewComponent;
import org.sonar.server.permission.PermissionTemplateService;
import org.sonar.server.user.UserSession;

import static org.sonar.core.permission.GlobalPermissions.SCAN_EXECUTION;
import static org.sonar.server.user.AbstractUserSession.insufficientPrivilegesException;

@ServerSide
public class ReportSubmitter {

  private final CeQueue queue;
  private final UserSession userSession;
  private final ComponentService componentService;
  private final PermissionTemplateService permissionTemplateService;
  private final DbClient dbClient;
  private final FavoriteUpdater favoriteUpdater;

  public ReportSubmitter(CeQueue queue, UserSession userSession,
    ComponentService componentService, PermissionTemplateService permissionTemplateService, DbClient dbClient, FavoriteUpdater favoriteUpdater) {
    this.queue = queue;
    this.userSession = userSession;
    this.componentService = componentService;
    this.permissionTemplateService = permissionTemplateService;
    this.dbClient = dbClient;
    this.favoriteUpdater = favoriteUpdater;
  }

  public CeTask submit(String projectKey, @Nullable String projectBranch, @Nullable String projectName, InputStream reportInput) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      String effectiveProjectKey = ComponentKeys.createKey(projectKey, projectBranch);
      Optional<ComponentDto> opt = dbClient.componentDao().selectByKey(dbSession, effectiveProjectKey);
      ComponentDto project = opt.or(() -> createProject(dbSession, projectKey, projectBranch, projectName));
      userSession.checkComponentUuidPermission(SCAN_EXECUTION, project.uuid());
      return submitReport(dbSession, reportInput, project);
    }
  }

  private ComponentDto createProject(DbSession dbSession, String projectKey, @Nullable String projectBranch, @Nullable String projectName) {
    Integer userId = userSession.getUserId();
    Long projectCreatorUserId = userId == null ? null : userId.longValue();

    boolean wouldCurrentUserHaveScanPermission = permissionTemplateService.wouldUserHavePermissionWithDefaultTemplate(
      dbSession, projectCreatorUserId, SCAN_EXECUTION, projectBranch, projectKey, Qualifiers.PROJECT);
    if (!wouldCurrentUserHaveScanPermission) {
      throw insufficientPrivilegesException();
    }

    NewComponent newProject = new NewComponent(projectKey, StringUtils.defaultIfBlank(projectName, projectKey));
    newProject.setBranch(projectBranch);
    newProject.setQualifier(Qualifiers.PROJECT);
    // "provisioning" permission is check in ComponentService
    ComponentDto project = componentService.create(dbSession, newProject);
    if (permissionTemplateService.hasDefaultTemplateWithPermissionOnProjectCreator(dbSession, project)) {
      favoriteUpdater.add(dbSession, project);
      dbSession.commit();
    }

    permissionTemplateService.applyDefault(dbSession, project, projectCreatorUserId);

    return project;
  }

  private CeTask submitReport(DbSession dbSession, InputStream reportInput, ComponentDto project) {
    // the report file must be saved before submitting the task
    CeTaskSubmit.Builder submit = queue.prepareSubmit();
    dbClient.ceTaskInputDao().insert(dbSession, submit.getUuid(), reportInput);
    dbSession.commit();

    submit.setType(CeTaskTypes.REPORT);
    submit.setComponentUuid(project.uuid());
    submit.setSubmitterLogin(userSession.getLogin());
    return queue.submit(submit.build());
  }
}
