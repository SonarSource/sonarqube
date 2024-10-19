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
package org.sonar.server.tester;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.web.UserRole;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.portfolio.PortfolioDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.user.AbstractUserSession;

import static com.google.common.base.Preconditions.checkArgument;

public abstract class AbstractMockUserSession<T extends AbstractMockUserSession> extends AbstractUserSession {

  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractMockUserSession.class);
  private static final Set<String> PUBLIC_PERMISSIONS = ImmutableSet.of(UserRole.USER, UserRole.CODEVIEWER); // FIXME to check with Simon

  private final Class<T> clazz;
  private final HashMultimap<String, String> projectUuidByPermission = HashMultimap.create();
  private final Set<GlobalPermission> permissions = new HashSet<>();
  private final Map<String, String> projectUuidByComponentUuid = new HashMap<>();
  private final Map<String, String> projectUuidByBranchUuid = new HashMap<>();
  private final Map<String, Set<String>> applicationProjects = new HashMap<>();
  private final Map<String, Set<String>> portfolioProjects = new HashMap<>();
  private final Set<String> projectPermissions = new HashSet<>();
  private boolean systemAdministrator = false;
  private boolean resetPassword = false;

  protected AbstractMockUserSession(Class<T> clazz) {
    this.clazz = clazz;
  }

  public T addPermission(GlobalPermission permission) {
    permissions.add(permission);
    return clazz.cast(this);
  }

  @Override
  protected boolean hasPermissionImpl(GlobalPermission permission) {
    return permissions.contains(permission);
  }

  /**
   * Use this method to register public root component and non root components the UserSession must be aware of.
   * (ie. this method can be used to emulate the content of the DB)
   */
  public T registerComponents(ComponentDto... components) {
    Arrays.stream(components)
      .forEach(component -> {
        if (component.branchUuid().equals(component.uuid()) && !component.isPrivate()) {
          this.projectUuidByPermission.put(UserRole.USER, component.uuid());
          this.projectUuidByPermission.put(UserRole.CODEVIEWER, component.uuid());
          this.projectPermissions.add(UserRole.USER);
          this.projectPermissions.add(UserRole.CODEVIEWER);
        }
        this.projectUuidByComponentUuid.put(component.uuid(), component.branchUuid());
      });
    return clazz.cast(this);
  }

  public T registerProjects(ProjectDto... projects) {
    Arrays.stream(projects)
      .forEach(project -> {
        if (!project.isPrivate()) {
          this.projectUuidByPermission.put(UserRole.USER, project.getUuid());
          this.projectUuidByPermission.put(UserRole.CODEVIEWER, project.getUuid());
          this.projectPermissions.add(UserRole.USER);
          this.projectPermissions.add(UserRole.CODEVIEWER);
        }
        this.projectUuidByComponentUuid.put(project.getUuid(), project.getUuid());
      });
    return clazz.cast(this);
  }

  public T registerApplication(ProjectDto application, ProjectDto... appProjects) {
    registerProjects(application);
    registerProjects(appProjects);

    var appProjectsUuid = Arrays.stream(appProjects)
      .map(ProjectDto::getUuid)
      .collect(Collectors.toSet());
    this.applicationProjects.put(application.getUuid(), appProjectsUuid);

    return clazz.cast(this);
  }

  public T registerPortfolios(PortfolioDto... portfolios) {
    Arrays.stream(portfolios)
      .forEach(portfolio -> {
        if (!portfolio.isPrivate()) {
          this.projectUuidByPermission.put(UserRole.USER, portfolio.getUuid());
          this.projectUuidByPermission.put(UserRole.CODEVIEWER, portfolio.getUuid());
          this.projectPermissions.add(UserRole.USER);
          this.projectPermissions.add(UserRole.CODEVIEWER);
        }
        this.projectUuidByComponentUuid.put(portfolio.getUuid(), portfolio.getUuid());
      });
    return clazz.cast(this);
  }

  public T registerBranches(BranchDto ...branchDtos){
    Arrays.stream(branchDtos)
      .forEach(branch -> projectUuidByBranchUuid.put(branch.getUuid(), branch.getProjectUuid()));
    return clazz.cast(this);
  }

  /**
   * Branches need to be registered in order to save the mapping between branch and project.
   */
  public T addProjectBranchMapping(String projectUuid, ComponentDto... componentDtos) {
    Arrays.stream(componentDtos)
      .forEach(componentDto -> projectUuidByBranchUuid.put(componentDto.uuid(), projectUuid));
    return clazz.cast(this);
  }

  public T addProjectPermission(String permission, ComponentDto... components) {
    Arrays.stream(components).forEach(component -> {
      checkArgument(
        component.isPrivate() || !PUBLIC_PERMISSIONS.contains(permission),
        "public component %s can't be granted public permission %s", component.uuid(), permission);
    });
    registerComponents(components);
    this.projectPermissions.add(permission);
    Arrays.stream(components)
      .forEach(component -> this.projectUuidByPermission.put(permission, component.branchUuid()));
    return clazz.cast(this);
  }

  public T addProjectPermission(String permission, ProjectDto... projects) {
    Arrays.stream(projects).forEach(component -> {
      checkArgument(
        component.isPrivate() || !PUBLIC_PERMISSIONS.contains(permission),
        "public component %s can't be granted public permission %s", component.getUuid(), permission);
    });
    registerProjects(projects);
    this.projectPermissions.add(permission);
    Arrays.stream(projects)
      .forEach(project -> this.projectUuidByPermission.put(permission, project.getUuid()));
    return clazz.cast(this);
  }

  public T addPortfolioPermission(String permission, PortfolioDto... portfolios) {
    Arrays.stream(portfolios).forEach(component -> {
      checkArgument(
        component.isPrivate() || !PUBLIC_PERMISSIONS.contains(permission),
        "public component %s can't be granted public permission %s", component.getUuid(), permission);
    });
    registerPortfolios(portfolios);
    this.projectPermissions.add(permission);
    Arrays.stream(portfolios)
      .forEach(portfolio -> this.projectUuidByPermission.put(permission, portfolio.getUuid()));
    return clazz.cast(this);
  }

  @Override
  protected Optional<String> componentUuidToEntityUuid(String componentUuid) {
    return Optional.ofNullable(Optional.ofNullable(projectUuidByBranchUuid.get(componentUuid))
      .orElse(projectUuidByComponentUuid.get(componentUuid)));
  }

  @Override
  public boolean hasComponentPermission(String permission, ComponentDto component) {
    return componentUuidToEntityUuid(component.uuid())
      .or(() -> componentUuidToEntityUuid(component.branchUuid()))
      .map(projectUuid -> hasEntityUuidPermission(permission, projectUuid)).orElseGet(() -> {
        LOGGER.warn("No project uuid for branchUuid : {}", component.branchUuid());
        return false;
      });
  }

  @Override
  protected boolean hasEntityUuidPermission(String permission, String entityUuid) {
    return projectPermissions.contains(permission) && projectUuidByPermission.get(permission).contains(entityUuid);
  }

  @Override
  protected boolean hasChildProjectsPermission(String permission, String applicationUuid) {
    return applicationProjects.containsKey(applicationUuid) && applicationProjects.get(applicationUuid)
      .stream()
      .allMatch(projectUuid -> projectPermissions.contains(permission) && projectUuidByPermission.get(permission).contains(projectUuid));
  }

  @Override
  protected boolean hasPortfolioChildProjectsPermission(String permission, String portfolioUuid) {
    return portfolioProjects.containsKey(portfolioUuid) && portfolioProjects.get(portfolioUuid)
      .stream()
      .allMatch(projectUuid -> projectPermissions.contains(permission) && projectUuidByPermission.get(permission).contains(projectUuid));
  }

  public T setSystemAdministrator(boolean b) {
    this.systemAdministrator = b;
    return clazz.cast(this);
  }

  @Override
  public boolean isSystemAdministrator() {
    return systemAdministrator;
  }

  public T setResetPassword(boolean b) {
    this.resetPassword = b;
    return clazz.cast(this);
  }

  @Override
  public boolean shouldResetPassword() {
    return resetPassword;
  }

  public abstract void flagAsBrowserSession();
}
