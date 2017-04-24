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
import org.sonar.db.organization.OrganizationDto;
import org.sonar.server.component.ComponentUpdater;
import org.sonar.server.component.NewComponent;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.db.permission.OrganizationPermission;
import org.sonar.server.permission.PermissionTemplateService;
import org.sonar.server.user.UserSession;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static org.sonar.core.permission.GlobalPermissions.SCAN_EXECUTION;
import static org.sonar.server.component.NewComponent.newComponentBuilder;
import static org.sonar.server.user.AbstractUserSession.insufficientPrivilegesException;

@ServerSide
public class ReportSubmitter {

  private final CeQueue queue;
  private final UserSession userSession;
  private final ComponentUpdater componentUpdater;
  private final PermissionTemplateService permissionTemplateService;
  private final DbClient dbClient;

  public ReportSubmitter(CeQueue queue, UserSession userSession, ComponentUpdater componentUpdater,
    PermissionTemplateService permissionTemplateService, DbClient dbClient) {
    this.queue = queue;
    this.userSession = userSession;
    this.componentUpdater = componentUpdater;
    this.permissionTemplateService = permissionTemplateService;
    this.dbClient = dbClient;
  }

  /**
   * @throws NotFoundException if the organization with the specified key does not exist
   * @throws IllegalArgumentException if the organization with the specified key is not the organization of the specified project (when it already exists in DB)
   */
  public CeTask submit(String organizationKey, String projectKey, @Nullable String projectBranch, @Nullable String projectName, InputStream reportInput) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      String effectiveProjectKey = ComponentKeys.createKey(projectKey, projectBranch);
      OrganizationDto organizationDto = getOrganizationDtoOrFail(dbSession, organizationKey);
      Optional<ComponentDto> opt = dbClient.componentDao().selectByKey(dbSession, effectiveProjectKey);
      ensureOrganizationIsConsistent(opt, organizationDto);
      ComponentDto project = opt.or(() -> createProject(dbSession, organizationDto, projectKey, projectBranch, projectName));
      checkScanPermission(project);
      return submitReport(dbSession, reportInput, project);
    }
  }

  private void checkScanPermission(ComponentDto project) {
    // this is a specific and inconsistent behavior. For legacy reasons, "technical users"
    // defined on an organization should be able to analyze a project even if
    // they don't have the direct permission on the project.
    // That means that dropping the permission on the project does not have any effects
    // if user has still the permission on the organization
    if (!userSession.hasComponentPermission(SCAN_EXECUTION, project) &&
      !userSession.hasPermission(OrganizationPermission.SCAN, project.getOrganizationUuid())) {
      throw insufficientPrivilegesException();
    }
  }

  private OrganizationDto getOrganizationDtoOrFail(DbSession dbSession, String organizationKey) {
    return dbClient.organizationDao().selectByKey(dbSession, organizationKey)
      .orElseThrow(() -> new NotFoundException(format("Organization with key '%s' does not exist", organizationKey)));
  }

  private static void ensureOrganizationIsConsistent(Optional<ComponentDto> project, OrganizationDto organizationDto) {
    if (project.isPresent()) {
      checkArgument(project.get().getOrganizationUuid().equals(organizationDto.getUuid()),
        "Organization of component with key '%s' does not match specified organization '%s'",
        project.get().key(), organizationDto.getKey());
    }
  }

  private ComponentDto createProject(DbSession dbSession, OrganizationDto organization, String projectKey, @Nullable String projectBranch, @Nullable String projectName) {
    userSession.checkPermission(OrganizationPermission.PROVISION_PROJECTS, organization);
    Integer userId = userSession.getUserId();

    boolean wouldCurrentUserHaveScanPermission = permissionTemplateService.wouldUserHaveScanPermissionWithDefaultTemplate(
      dbSession, organization.getUuid(), userId, projectBranch, projectKey, Qualifiers.PROJECT);
    if (!wouldCurrentUserHaveScanPermission) {
      throw insufficientPrivilegesException();
    }

    boolean newProjectPrivate = dbClient.organizationDao().getNewProjectPrivate(dbSession, organization);

    NewComponent newProject = newComponentBuilder()
      .setOrganizationUuid(organization.getUuid())
      .setKey(projectKey)
      .setName(StringUtils.defaultIfBlank(projectName, projectKey))
      .setBranch(projectBranch)
      .setQualifier(Qualifiers.PROJECT)
      .setPrivate(newProjectPrivate)
      .build();
    return componentUpdater.create(dbSession, newProject, userId);
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
