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
package org.sonar.server.user;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.db.component.ComponentQualifiers;
import org.sonar.db.component.ComponentScopes;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTreeQuery;
import org.sonar.db.component.ComponentTreeQuery.Strategy;
import org.sonar.db.entity.EntityDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.organization.OrganizationMemberDto;
import org.sonar.db.permission.OrganizationPermission;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;

import static java.util.Collections.singleton;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toSet;
import static org.sonar.db.component.ComponentQualifiers.SUBVIEW;
import static org.sonar.db.component.ComponentQualifiers.VIEW;
import static org.sonar.api.web.UserRole.PUBLIC_PERMISSIONS;

/**
 * Implementation of {@link UserSession} used in web server
 */
public class ServerUserSession extends AbstractUserSession {

  private static final Set<String> QUALIFIERS = Set.of(VIEW, SUBVIEW);

  @CheckForNull
  private final UserDto userDto;
  private final boolean isAuthenticatedBrowserSession;
  private final DbClient dbClient;
  private final Map<String, String> entityUuidByComponentUuid = new HashMap<>();
  private final Map<String, Set<String>> permissionsByEntityUuid = new HashMap<>();
  private final Map<String, Set<OrganizationPermission>> permissionsByOrganizationUuid = new HashMap<>();

  private Collection<GroupDto> groups;
  private final Set<String> organizationMembership = new HashSet<>();

  public ServerUserSession(DbClient dbClient, @Nullable UserDto userDto, boolean isAuthenticatedBrowserSession) {
    this.dbClient = dbClient;
    this.userDto = userDto;
    this.isAuthenticatedBrowserSession = isAuthenticatedBrowserSession;
  }

  private Collection<GroupDto> loadGroups() {
    if (this.userDto == null) {
      return Collections.emptyList();
    }
    try (DbSession dbSession = dbClient.openSession(false)) {
      return dbClient.groupDao().selectByUserLogin(dbSession, userDto.getLogin());
    }
  }

  @Override
  @CheckForNull
  public Long getLastSonarlintConnectionDate() {
    return userDto == null ? null : userDto.getLastSonarlintConnectionDate();
  }

  @Override
  @CheckForNull
  public String getLogin() {
    return userDto == null ? null : userDto.getLogin();
  }

  @Override
  @CheckForNull
  public String getUuid() {
    return userDto == null ? null : userDto.getUuid();
  }

  @Override
  @CheckForNull
  public String getName() {
    return userDto == null ? null : userDto.getName();
  }

  @Override
  public Collection<GroupDto> getGroups() {
    if (groups == null) {
      groups = loadGroups();
    }
    return groups;
  }

  @Override
  public boolean shouldResetPassword() {
    return userDto != null && userDto.isResetPassword();
  }

  @Override
  public boolean isLoggedIn() {
    return userDto != null;
  }

  @Override
  public Optional<IdentityProvider> getIdentityProvider() {
    return ofNullable(userDto).map(d -> computeIdentity(d).getIdentityProvider());
  }

  @Override
  public Optional<ExternalIdentity> getExternalIdentity() {
    return ofNullable(userDto).map(d -> computeIdentity(d).getExternalIdentity());
  }

  @Override
  protected Optional<String> componentUuidToEntityUuid(String componentUuid) {
    String entityUuid = entityUuidByComponentUuid.get(componentUuid);
    if (entityUuid != null) {
      return of(entityUuid);
    }
    try (DbSession dbSession = dbClient.openSession(false)) {
      Optional<ComponentDto> component = dbClient.componentDao().selectByUuid(dbSession, componentUuid);
      if (component.isEmpty()) {
        return Optional.empty();
      }
      // permissions must be checked on the project
      entityUuid = getEntityUuid(dbSession, component.get());
      entityUuidByComponentUuid.put(componentUuid, entityUuid);
      return of(entityUuid);
    }
  }

  @Override
  protected boolean hasEntityUuidPermission(String permission, String entityUuid) {
    return hasPermission(permission, entityUuid);
  }

  @Override
  protected boolean hasChildProjectsPermission(String permission, String applicationUuid) {
    Set<String> childProjectUuids = loadChildProjectUuids(applicationUuid);
    Set<String> projectsWithPermission = keepEntitiesUuidsByPermission(permission, childProjectUuids);
    return projectsWithPermission.containsAll(childProjectUuids);
  }

  @Override
  protected boolean hasPortfolioChildProjectsPermission(String permission, String portfolioUuid) {
    Set<ComponentDto> portfolioHierarchyComponents = resolvePortfolioHierarchyComponents(portfolioUuid);
    Set<String> branchUuids = findBranchUuids(portfolioHierarchyComponents);
    Set<String> projectUuids = findProjectUuids(branchUuids);

    Set<String> projectsWithPermission = keepEntitiesUuidsByPermission(permission, projectUuids);
    return projectsWithPermission.containsAll(projectUuids);
  }

  @Override
  protected <T extends EntityDto> List<T> doKeepAuthorizedEntities(String permission, Collection<T> entities) {
    if (isRoot()) {
      return new ArrayList<>(entities);
    }

    Set<String> projectsUuids = entities.stream().map(EntityDto::getUuid).collect(Collectors.toSet());
    // TODO in SONAR-19445
    Set<String> authorizedEntitiesUuids = keepEntitiesUuidsByPermission(permission, projectsUuids);

    return entities.stream()
      .filter(project -> authorizedEntitiesUuids.contains(project.getUuid()))
      .toList();
  }

  private Set<String> keepEntitiesUuidsByPermission(String permission, Collection<String> entityUuids) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      String userUuid = userDto == null ? null : userDto.getUuid();
      return dbClient.authorizationDao().keepAuthorizedEntityUuids(dbSession, entityUuids, userUuid, permission);
    }
  }

  private static Set<String> findBranchUuids(Set<ComponentDto> portfolioHierarchyComponents) {
    return portfolioHierarchyComponents.stream()
      .map(ComponentDto::getCopyComponentUuid)
      .collect(toSet());
  }

  private Set<String> findProjectUuids(Set<String> branchesComponentsUuid) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      List<ComponentDto> componentDtos = dbClient.componentDao().selectByUuids(dbSession, branchesComponentsUuid);
      return getProjectUuids(dbSession, componentDtos);
    }
  }

  private String getEntityUuid(DbSession dbSession, ComponentDto componentDto) {
    // Portfolio & subPortfolio don't have branch, so branchUuid represents the portfolio uuid.
    // technical project store root portfolio uuid in branchUuid
    if (isPortfolioOrSubPortfolio(componentDto) || isTechnicalProject(componentDto)) {
      return componentDto.branchUuid();
    }
    Optional<BranchDto> branchDto = dbClient.branchDao().selectByUuid(dbSession, componentDto.branchUuid());
    return branchDto.map(BranchDto::getProjectUuid).orElseThrow(() -> new IllegalStateException("No branch found for component : " + componentDto));
  }

  private Set<String> getProjectUuids(DbSession dbSession, Collection<ComponentDto> components) {
    Set<String> mainProjectUuids = new HashSet<>();

    // the result of following stream could be project or application
    Collection<String> componentsWithBranch = components.stream()
      .filter(c -> !(isTechnicalProject(c) || isPortfolioOrSubPortfolio(c)))
      .map(ComponentDto::branchUuid)
      .toList();

    dbClient.branchDao().selectByUuids(dbSession, componentsWithBranch).stream()
      .map(BranchDto::getProjectUuid).forEach(mainProjectUuids::add);

    components.stream()
      .filter(c -> isTechnicalProject(c) || isPortfolioOrSubPortfolio(c))
      .map(ComponentDto::branchUuid)
      .forEach(mainProjectUuids::add);

    return mainProjectUuids;
  }

  private static boolean isTechnicalProject(ComponentDto componentDto) {
    return ComponentQualifiers.PROJECT.equals(componentDto.qualifier()) && ComponentScopes.FILE.equals(componentDto.scope());
  }

  private static boolean isPortfolioOrSubPortfolio(ComponentDto componentDto) {
    return !Objects.isNull(componentDto.qualifier()) && QUALIFIERS.contains(componentDto.qualifier());
  }

  private boolean hasPermission(String permission, String entityUuid) {
    Set<String> entityPermissions = permissionsByEntityUuid.computeIfAbsent(entityUuid, this::loadEntityPermissions);
    return entityPermissions.contains(permission);
  }

  private Set<String> loadEntityPermissions(String entityUuid) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      Optional<EntityDto> entity = dbClient.entityDao().selectByUuid(dbSession, entityUuid);
      if (entity.isEmpty()) {
        return Collections.emptySet();
      }
      if (entity.get().isPrivate()) {
        return loadDbPermissions(dbSession, entityUuid);
      }
      Set<String> projectPermissions = new HashSet<>();
      projectPermissions.addAll(PUBLIC_PERMISSIONS);
      projectPermissions.addAll(loadDbPermissions(dbSession, entityUuid));
      return Collections.unmodifiableSet(projectPermissions);
    }
  }

  private Set<String> loadChildProjectUuids(String applicationUuid) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      BranchDto branchDto = dbClient.branchDao().selectMainBranchByProjectUuid(dbSession, applicationUuid)
        .orElseThrow();
      Set<String> projectBranchesUuid = dbClient.componentDao()
        .selectDescendants(dbSession, ComponentTreeQuery.builder()
          .setBaseUuid(branchDto.getUuid())
          .setQualifiers(singleton(ComponentQualifiers.PROJECT))
          .setScopes(singleton(ComponentScopes.FILE))
          .setStrategy(Strategy.CHILDREN).build())
        .stream()
        .map(ComponentDto::getCopyComponentUuid)
        .collect(toSet());

      return dbClient.branchDao().selectByUuids(dbSession, projectBranchesUuid).stream()
        .map(BranchDto::getProjectUuid)
        .collect(toSet());

    }
  }

  private List<ComponentDto> getDirectChildComponents(String portfolioUuid) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      return dbClient.componentDao().selectDescendants(dbSession, ComponentTreeQuery.builder()
        .setBaseUuid(portfolioUuid)
        .setQualifiers(Arrays.asList(ComponentQualifiers.PROJECT, ComponentQualifiers.SUBVIEW))
        .setStrategy(Strategy.CHILDREN).build());
    }
  }

  private Set<ComponentDto> resolvePortfolioHierarchyComponents(String parentComponentUuid) {
    Set<ComponentDto> portfolioHierarchyProjects = new HashSet<>();
    resolvePortfolioHierarchyComponents(parentComponentUuid, portfolioHierarchyProjects);
    return portfolioHierarchyProjects;
  }

  private void resolvePortfolioHierarchyComponents(String parentComponentUuid, Set<ComponentDto> hierarchyChildComponents) {
    List<ComponentDto> childComponents = getDirectChildComponents(parentComponentUuid);

    if (childComponents.isEmpty()) {
      return;
    }

    childComponents.forEach(c -> {
      if (c.getCopyComponentUuid() != null) {
        hierarchyChildComponents.add(c);
      }

      if (ComponentQualifiers.SUBVIEW.equals(c.qualifier())) {
        resolvePortfolioHierarchyComponents(c.uuid(), hierarchyChildComponents);
      }
    });
  }

  @Override
  protected boolean hasPermissionImpl(OrganizationPermission permission, String organizationUuid) {
    Set<OrganizationPermission> permissions = permissionsByOrganizationUuid.computeIfAbsent(organizationUuid, this::loadOrganizationPermissions);
    return permissions.contains(permission);
  }

  private Set<OrganizationPermission> loadOrganizationPermissions(String organizationUuid) {
    Set<String> permissionKeys;
    try (DbSession dbSession = dbClient.openSession(false)) {
      if (userDto != null && userDto.getUuid() != null) {
        permissionKeys = dbClient.authorizationDao().selectOrganizationPermissions(dbSession, organizationUuid, userDto.getUuid());
      } else {
        permissionKeys = dbClient.authorizationDao().selectOrganizationPermissionsOfAnonymous(dbSession, organizationUuid);
      }
    }
    return permissionKeys.stream()
      .map(OrganizationPermission::fromKey)
      .collect(toSet());
  }

  private Set<String> loadDbPermissions(DbSession dbSession, String entityUuid) {
    if (userDto != null && userDto.getUuid() != null) {
      return dbClient.authorizationDao().selectEntityPermissions(dbSession, entityUuid, userDto.getUuid());
    }
    return dbClient.authorizationDao().selectEntityPermissionsOfAnonymous(dbSession, entityUuid);
  }

  @Override
  protected List<ComponentDto> doKeepAuthorizedComponents(String permission, Collection<ComponentDto> components) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      Set<String> projectUuids = getProjectUuids(dbSession, components);

      Map<String, ComponentDto> originalComponents = findComponentsByCopyComponentUuid(components,
        dbSession);

      Set<String> originalComponentsProjectUuids = getProjectUuids(dbSession, originalComponents.values());

      Set<String> allProjectUuids = new HashSet<>(projectUuids);
      allProjectUuids.addAll(originalComponentsProjectUuids);

      Set<String> authorizedProjectUuids = keepAuthorizedProjectsUuids(dbSession, permission, allProjectUuids);

      return components.stream()
        .filter(c -> {
          if (c.getCopyComponentUuid() != null) {
            var componentDto = originalComponents.get(c.getCopyComponentUuid());
            return componentDto != null && authorizedProjectUuids.contains(getEntityUuid(dbSession, componentDto));
          }

          return authorizedProjectUuids.contains(c.branchUuid()) || authorizedProjectUuids.contains(
            getEntityUuid(dbSession, c));
        })
        .toList();
    }
  }

  protected Set<String> keepAuthorizedProjectsUuids(DbSession dbSession, String permission, Collection<String> entityUuids) {
    return dbClient.authorizationDao().keepAuthorizedEntityUuids(dbSession, entityUuids, getUuid(), permission);
  }

  private Map<String, ComponentDto> findComponentsByCopyComponentUuid(Collection<ComponentDto> components, DbSession dbSession) {
    Set<String> copyComponentsUuid = components.stream()
      .map(ComponentDto::getCopyComponentUuid)
      .filter(Objects::nonNull)
      .collect(toSet());
    return dbClient.componentDao().selectByUuids(dbSession, copyComponentsUuid).stream()
      .collect(Collectors.toMap(ComponentDto::uuid, componentDto -> componentDto));
  }

  @Override
  public boolean isSystemAdministrator() {
    // organization feature is enabled -> requires to be root
    return isRoot();
  }

  @Override
  public boolean isActive() {
    return userDto.isActive();
  }

  @Override
  public boolean isAuthenticatedBrowserSession() {
    return isAuthenticatedBrowserSession;
  }

  @Override
  public boolean isRoot() {
    return userDto != null && userDto.isRoot();
  }

  @Override
  public boolean hasMembershipImpl(OrganizationDto organizationDto) {
    return isMember(organizationDto.getUuid());
  }

  @Override
  public boolean hasMembershipImpl(String organizationKey) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      Optional<OrganizationDto> organizationDto = dbClient.organizationDao().selectByKey(dbSession, organizationKey);
      return organizationDto.filter(dto -> isMember(dto.getUuid())).isPresent();
    }
  }

  private boolean isMember(String organizationUuid) {
    if (!isLoggedIn()) {
      return false;
    }
    if (isRoot()) {
      return true;
    }

    if (organizationMembership.contains(organizationUuid)) {
      return true;
    }
    try (DbSession dbSession = dbClient.openSession(false)) {
      Optional<OrganizationMemberDto> organizationMemberDto = dbClient.organizationMemberDao().select(dbSession, organizationUuid, requireNonNull(getUuid()));
      if (organizationMemberDto.isPresent()) {
        organizationMembership.add(organizationUuid);
      }
      return organizationMembership.contains(organizationUuid);
    }
  }
}
