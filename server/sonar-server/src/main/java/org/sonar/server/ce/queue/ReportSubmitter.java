/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.ce.queue;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Scopes;
import org.sonar.api.server.ServerSide;
import org.sonar.api.web.UserRole;
import org.sonar.ce.queue.CeQueue;
import org.sonar.ce.queue.CeTaskSubmit;
import org.sonar.ce.task.CeTask;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.ce.CeTaskTypes;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.permission.OrganizationPermission;
import org.sonar.server.component.ComponentUpdater;
import org.sonar.server.component.NewComponent;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.permission.PermissionTemplateService;
import org.sonar.server.user.UserSession;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.defaultIfBlank;
import static org.sonar.server.component.NewComponent.newComponentBuilder;
import static org.sonar.server.user.AbstractUserSession.insufficientPrivilegesException;

@ServerSide
public class ReportSubmitter {

  private final CeQueue queue;
  private final UserSession userSession;
  private final ComponentUpdater componentUpdater;
  private final PermissionTemplateService permissionTemplateService;
  private final DbClient dbClient;
  private final BranchSupport branchSupport;

  public ReportSubmitter(CeQueue queue, UserSession userSession, ComponentUpdater componentUpdater,
    PermissionTemplateService permissionTemplateService, DbClient dbClient, BranchSupport branchSupport) {
    this.queue = queue;
    this.userSession = userSession;
    this.componentUpdater = componentUpdater;
    this.permissionTemplateService = permissionTemplateService;
    this.dbClient = dbClient;
    this.branchSupport = branchSupport;
  }

  /**
   * @throws NotFoundException if the organization with the specified key does not exist
   * @throws IllegalArgumentException if the organization with the specified key is not the organization of the specified project (when it already exists in DB)
   */
  public CeTask submit(String organizationKey, String projectKey, @Nullable String projectName,
    Map<String, String> characteristics, InputStream reportInput) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      OrganizationDto organizationDto = getOrganizationDtoOrFail(dbSession, organizationKey);
      BranchSupport.ComponentKey componentKey = branchSupport.createComponentKey(projectKey, characteristics);
      Optional<ComponentDto> existingComponent = dbClient.componentDao().selectByKey(dbSession, componentKey.getDbKey());
      validateProject(dbSession, existingComponent, projectKey);
      ensureOrganizationIsConsistent(existingComponent, organizationDto);
      ComponentDto component = existingComponent.orElseGet(() -> createComponent(dbSession, organizationDto, componentKey, projectName));
      checkScanPermission(component);
      return submitReport(dbSession, reportInput, component, characteristics);
    }
  }

  private void checkScanPermission(ComponentDto project) {
    // this is a specific and inconsistent behavior. For legacy reasons, "technical users"
    // defined on an organization should be able to analyze a project even if
    // they don't have the direct permission on the project.
    // That means that dropping the permission on the project does not have any effects
    // if user has still the permission on the organization
    if (!userSession.hasComponentPermission(UserRole.SCAN, project) &&
      !userSession.hasPermission(OrganizationPermission.SCAN, project.getOrganizationUuid())) {
      throw insufficientPrivilegesException();
    }
  }

  private OrganizationDto getOrganizationDtoOrFail(DbSession dbSession, String organizationKey) {
    return dbClient.organizationDao().selectByKey(dbSession, organizationKey)
      .orElseThrow(() -> new NotFoundException(format("Organization with key '%s' does not exist", organizationKey)));
  }

  private void validateProject(DbSession dbSession, Optional<ComponentDto> project, String rawProjectKey) {
    List<String> errors = new ArrayList<>();
    if (!project.isPresent()) {
      return;
    }

    ComponentDto component = project.get();
    if (!Qualifiers.PROJECT.equals(component.qualifier()) || !Scopes.PROJECT.equals(component.scope())) {
      errors.add(format("Component '%s' is not a project", rawProjectKey));
    }
    if (!component.projectUuid().equals(component.uuid())) {
      // Project key is already used as a module of another project
      ComponentDto anotherBaseProject = dbClient.componentDao().selectOrFailByUuid(dbSession, component.projectUuid());
      errors.add(format("The project '%s' is already defined in SonarQube but as a module of project '%s'. "
        + "If you really want to stop directly analysing project '%s', please first delete it from SonarQube and then relaunch the analysis of project '%s'.",
        rawProjectKey, anotherBaseProject.getKey(), anotherBaseProject.getKey(), rawProjectKey));
    }
    if (!errors.isEmpty()) {
      throw BadRequestException.create(errors);
    }
  }

  private static void ensureOrganizationIsConsistent(Optional<ComponentDto> project, OrganizationDto organizationDto) {
    if (project.isPresent()) {
      checkArgument(project.get().getOrganizationUuid().equals(organizationDto.getUuid()),
        "Organization of component with key '%s' does not match specified organization '%s'",
        project.get().getDbKey(), organizationDto.getKey());
    }
  }

  private ComponentDto createComponent(DbSession dbSession, OrganizationDto organization, BranchSupport.ComponentKey componentKey, @Nullable String projectName) {
    if (componentKey.isMainBranch()) {
      ComponentDto project = createProject(dbSession, organization, componentKey, projectName);
      componentUpdater.commitAndIndex(dbSession, project);
      return project;
    }

    Optional<ComponentDto> existingMainComponent = dbClient.componentDao().selectByKey(dbSession, componentKey.getKey());
    ComponentDto mainComponentDto = existingMainComponent
      .orElseGet(() -> createProject(dbSession, organization, componentKey.getMainBranchComponentKey(), projectName));
    BranchDto mainComponentBranchDto = dbClient.branchDao().selectByUuid(dbSession, mainComponentDto.uuid())
      .orElseThrow(() -> new IllegalStateException("Branch of main component does not exist"));
    ComponentDto branchComponent = branchSupport.createBranchComponent(dbSession, componentKey, organization, mainComponentDto, mainComponentBranchDto);
    if (existingMainComponent.isPresent()) {
      dbSession.commit();
    } else {
      componentUpdater.commitAndIndex(dbSession, mainComponentDto);
    }
    return branchComponent;
  }

  private ComponentDto createProject(DbSession dbSession, OrganizationDto organization, BranchSupport.ComponentKey componentKey,
    @Nullable String projectName) {
    userSession.checkPermission(OrganizationPermission.PROVISION_PROJECTS, organization);
    Integer userId = userSession.getUserId();

    boolean wouldCurrentUserHaveScanPermission = permissionTemplateService.wouldUserHaveScanPermissionWithDefaultTemplate(
      dbSession, organization.getUuid(), userId, componentKey.getDbKey());
    if (!wouldCurrentUserHaveScanPermission) {
      throw insufficientPrivilegesException();
    }

    boolean newProjectPrivate = dbClient.organizationDao().getNewProjectPrivate(dbSession, organization);

    NewComponent newProject = newComponentBuilder()
      .setOrganizationUuid(organization.getUuid())
      .setKey(componentKey.getKey())
      .setName(defaultIfBlank(projectName, componentKey.getKey()))
      .setQualifier(Qualifiers.PROJECT)
      .setPrivate(newProjectPrivate)
      .build();
    return componentUpdater.createWithoutCommit(dbSession, newProject, userId);
  }

  private CeTask submitReport(DbSession dbSession, InputStream reportInput, ComponentDto project, Map<String, String> characteristics) {
    CeTaskSubmit.Builder submit = queue.prepareSubmit();

    // the report file must be saved before submitting the task
    dbClient.ceTaskInputDao().insert(dbSession, submit.getUuid(), reportInput);
    dbSession.commit();

    submit.setType(CeTaskTypes.REPORT);
    submit.setComponent(CeTaskSubmit.Component.fromDto(project));
    submit.setSubmitterUuid(userSession.getUuid());
    submit.setCharacteristics(characteristics);
    return queue.submit(submit.build());
  }

}
