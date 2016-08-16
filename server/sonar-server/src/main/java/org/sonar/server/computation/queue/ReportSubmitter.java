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

import java.io.InputStream;
import javax.annotation.CheckForNull;
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
import org.sonar.server.component.NewComponent;
import org.sonar.server.permission.PermissionService;
import org.sonar.server.user.UserSession;

import static org.sonar.core.permission.GlobalPermissions.SCAN_EXECUTION;
import static org.sonar.server.user.AbstractUserSession.insufficientPrivilegesException;

@ServerSide
public class ReportSubmitter {

  private final CeQueue queue;
  private final UserSession userSession;
  private final ComponentService componentService;
  private final PermissionService permissionService;
  private final DbClient dbClient;

  public ReportSubmitter(CeQueue queue, UserSession userSession,
    ComponentService componentService, PermissionService permissionService, DbClient dbClient) {
    this.queue = queue;
    this.userSession = userSession;
    this.componentService = componentService;
    this.permissionService = permissionService;
    this.dbClient = dbClient;
  }

  public CeTask submit(String projectKey, @Nullable String projectBranch, @Nullable String projectName, InputStream reportInput) {
    String effectiveProjectKey = ComponentKeys.createKey(projectKey, projectBranch);
    ComponentDto project = componentService.getNullableByKey(effectiveProjectKey);
    if (project == null) {
      project = createProject(projectKey, projectBranch, projectName);
    }

    userSession.checkComponentPermission(SCAN_EXECUTION, projectKey);

    return submitReport(reportInput, project);
  }

  @CheckForNull
  private ComponentDto createProject(String projectKey, @Nullable String projectBranch, @Nullable String projectName) {
    DbSession dbSession = dbClient.openSession(false);
    try {
      boolean wouldCurrentUserHaveScanPermission = permissionService.wouldCurrentUserHavePermissionWithDefaultTemplate(dbSession, SCAN_EXECUTION, projectBranch, projectKey,
        Qualifiers.PROJECT);
      if (!wouldCurrentUserHaveScanPermission) {
        throw insufficientPrivilegesException();
      }

      NewComponent newProject = new NewComponent(projectKey, StringUtils.defaultIfBlank(projectName, projectKey));
      newProject.setBranch(projectBranch);
      newProject.setQualifier(Qualifiers.PROJECT);
      // "provisioning" permission is check in ComponentService
      ComponentDto project = componentService.create(dbSession, newProject);
      permissionService.applyDefaultPermissionTemplate(dbSession, project.getKey());
      return project;
    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  private CeTask submitReport(InputStream reportInput, ComponentDto project) {
    // the report file must be saved before submitting the task
    CeTaskSubmit.Builder submit = queue.prepareSubmit();
    try (DbSession dbSession = dbClient.openSession(false)) {
      dbClient.ceTaskInputDao().insert(dbSession, submit.getUuid(), reportInput);
      dbSession.commit();
    }

    submit.setType(CeTaskTypes.REPORT);
    submit.setComponentUuid(project.uuid());
    submit.setSubmitterLogin(userSession.getLogin());
    return queue.submit(submit.build());
  }
}
