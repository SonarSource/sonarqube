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
package org.sonar.ce.queue.report;

import java.io.InputStream;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.ce.queue.CeQueue;
import org.sonar.ce.queue.CeTask;
import org.sonar.ce.queue.CeTaskSubmit;
import org.sonar.core.component.ComponentKeys;
import org.sonar.db.ce.CeTaskTypes;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.component.ComponentService;
import org.sonar.server.component.NewComponent;
import org.sonar.server.permission.PermissionService;
import org.sonar.server.user.UserSession;

import static org.sonar.core.permission.GlobalPermissions.SCAN_EXECUTION;

@ComputeEngineSide
public class ReportSubmitter {

  private final CeQueue queue;
  private final UserSession userSession;
  private final ReportFiles reportFiles;
  private final ComponentService componentService;
  private final PermissionService permissionService;

  public ReportSubmitter(CeQueue queue, UserSession userSession, ReportFiles reportFiles,
    ComponentService componentService, PermissionService permissionService) {
    this.queue = queue;
    this.userSession = userSession;
    this.reportFiles = reportFiles;
    this.componentService = componentService;
    this.permissionService = permissionService;
  }

  public CeTask submit(String projectKey, @Nullable String projectBranch, @Nullable String projectName, InputStream reportInput) {
    String effectiveProjectKey = ComponentKeys.createKey(projectKey, projectBranch);
    ComponentDto project = componentService.getNullableByKey(effectiveProjectKey);
    if (project == null) {
      // the project does not exist -> require global permission
      userSession.checkPermission(SCAN_EXECUTION);

      // the project does not exist -> requires to provision it
      NewComponent newProject = new NewComponent(projectKey, StringUtils.defaultIfBlank(projectName, projectKey));
      newProject.setBranch(projectBranch);
      newProject.setQualifier(Qualifiers.PROJECT);
      // no need to verify the permission "provisioning" as it's already handled by componentService
      project = componentService.create(newProject);
      permissionService.applyDefaultPermissionTemplate(project.getKey());
    } else {
      // the project exists -> require global or project permission
      userSession.checkComponentPermission(SCAN_EXECUTION, projectKey);
    }

    // the report file must be saved before submitting the task
    CeTaskSubmit.Builder submit = queue.prepareSubmit();
    reportFiles.save(submit.getUuid(), reportInput);

    submit.setType(CeTaskTypes.REPORT);
    submit.setComponentUuid(project.uuid());
    submit.setSubmitterLogin(userSession.getLogin());
    return queue.submit(submit.build());
  }
}
