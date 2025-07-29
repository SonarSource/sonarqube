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
package org.sonar.server.common.component;

import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.api.utils.System2;
import org.sonar.core.i18n.I18n;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentQualifiers;
import org.sonar.db.component.ComponentScopes;
import org.sonar.db.permission.ProjectPermission;
import org.sonar.db.portfolio.PortfolioDto;
import org.sonar.db.portfolio.PortfolioDto.SelectionMode;
import org.sonar.db.project.CreationMethod;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.property.PropertyDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.common.permission.Operation;
import org.sonar.server.common.permission.PermissionTemplateService;
import org.sonar.server.common.permission.PermissionUpdater;
import org.sonar.server.common.permission.UserPermissionChange;
import org.sonar.server.component.ComponentCreationData;
import org.sonar.server.es.Indexers;
import org.sonar.server.favorite.FavoriteUpdater;
import org.sonar.server.permission.PermissionService;
import org.sonar.server.project.DefaultBranchNameResolver;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.singletonList;
import static org.sonar.core.component.ComponentKeys.ALLOWED_CHARACTERS_MESSAGE;
import static org.sonar.core.component.ComponentKeys.isValidProjectKey;
import static org.sonar.db.permission.ProjectPermission.PUBLIC_PERMISSIONS;
import static org.sonar.server.exceptions.BadRequestException.checkRequest;
import static org.sonar.server.exceptions.BadRequestException.throwBadRequestException;

public class ComponentUpdater {

  static final String SUGGESTION_FEATURE_ENABLED_PROPERTY = "sonar.ai.suggestions.enabled";
  static final String ENABLED_FOR_ALL_PROJECTS = "ENABLED_FOR_ALL_PROJECTS";
  private static final Set<String> PROJ_APP_QUALIFIERS = Set.of(ComponentQualifiers.PROJECT, ComponentQualifiers.APP);
  private static final String KEY_ALREADY_EXISTS_ERROR = "Could not create %s with key: \"%s\". A similar key already exists: \"%s\"";
  private static final String MALFORMED_KEY_ERROR = "Malformed key for %s: '%s'. %s.";
  private final DbClient dbClient;
  private final I18n i18n;
  private final System2 system2;
  private final PermissionTemplateService permissionTemplateService;
  private final FavoriteUpdater favoriteUpdater;
  private final Indexers indexers;
  private final UuidFactory uuidFactory;
  private final DefaultBranchNameResolver defaultBranchNameResolver;
  private final PermissionUpdater<UserPermissionChange> userPermissionUpdater;
  private final PermissionService permissionService;

  public ComponentUpdater(DbClient dbClient, I18n i18n, System2 system2,
    PermissionTemplateService permissionTemplateService, FavoriteUpdater favoriteUpdater,
    Indexers indexers, UuidFactory uuidFactory, DefaultBranchNameResolver defaultBranchNameResolver, PermissionUpdater<UserPermissionChange> userPermissionUpdater,
    PermissionService permissionService) {
    this.dbClient = dbClient;
    this.i18n = i18n;
    this.system2 = system2;
    this.permissionTemplateService = permissionTemplateService;
    this.favoriteUpdater = favoriteUpdater;
    this.indexers = indexers;
    this.uuidFactory = uuidFactory;
    this.defaultBranchNameResolver = defaultBranchNameResolver;
    this.userPermissionUpdater = userPermissionUpdater;
    this.permissionService = permissionService;
  }

  /**
   * - Create component
   * - Apply default permission template
   * - Add component to favorite if the component has the 'Project Creators' permission
   * - Index component in es indexes
   */
  public ComponentCreationData create(DbSession dbSession, ComponentCreationParameters componentCreationParameters) {
    ComponentCreationData componentCreationData = createWithoutCommit(dbSession, componentCreationParameters);
    commitAndIndex(dbSession, componentCreationData);
    return componentCreationData;
  }

  public void commitAndIndex(DbSession dbSession, ComponentCreationData componentCreationData) {
    if (componentCreationData.portfolioDto() != null) {
      indexers.commitAndIndexEntities(dbSession, singletonList(componentCreationData.portfolioDto()), Indexers.EntityEvent.CREATION);
    } else if (componentCreationData.projectDto() != null) {
      indexers.commitAndIndexEntities(dbSession, singletonList(componentCreationData.projectDto()), Indexers.EntityEvent.CREATION);
    }
  }

  /**
   * Create component without committing.
   * Don't forget to call commitAndIndex(...) when ready to commit.
   */
  public ComponentCreationData createWithoutCommit(DbSession dbSession, ComponentCreationParameters componentCreationParameters) {
    checkKeyFormat(componentCreationParameters.newComponent().qualifier(), componentCreationParameters.newComponent().key());
    checkKeyAlreadyExists(dbSession, componentCreationParameters.newComponent());

    long now = system2.now();

    ComponentDto componentDto = createRootComponent(dbSession, componentCreationParameters.newComponent(), now);

    BranchDto mainBranch = null;
    ProjectDto projectDto = null;
    PortfolioDto portfolioDto = null;

    if (isProjectOrApp(componentDto)) {
      var isAiCodeFixEnabled = isAiCodeFixEnabledForAllProjects();
      projectDto = toProjectDto(componentDto, now, componentCreationParameters.creationMethod(), isAiCodeFixEnabled);
      dbClient.projectDao().insert(dbSession, projectDto);
      addToFavourites(dbSession, projectDto, componentCreationParameters.userUuid(), componentCreationParameters.userLogin());
      mainBranch = createMainBranch(dbSession, componentDto.uuid(), projectDto.getUuid(), componentCreationParameters.mainBranchName());
      if (componentCreationParameters.isManaged()) {
        applyPublicPermissionsForCreator(dbSession, projectDto, componentCreationParameters.userUuid());
      } else {
        permissionTemplateService.applyDefaultToNewComponent(dbSession, projectDto, componentCreationParameters.userUuid());
      }
    } else if (isPortfolio(componentDto)) {
      portfolioDto = toPortfolioDto(componentDto, now);
      dbClient.portfolioDao().insert(dbSession, portfolioDto, false);
      permissionTemplateService.applyDefaultToNewComponent(dbSession, portfolioDto, componentCreationParameters.userUuid());
    } else {
      throw new IllegalArgumentException("Component " + componentDto + " is not a top level entity");
    }

    return new ComponentCreationData(componentDto, portfolioDto, mainBranch, projectDto);
  }

  private boolean isAiCodeFixEnabledForAllProjects() {
    return Optional.ofNullable(dbClient.propertiesDao().selectGlobalProperty(SUGGESTION_FEATURE_ENABLED_PROPERTY))
      .map(PropertyDto::getValue)
      .stream().anyMatch(ENABLED_FOR_ALL_PROJECTS::equals);
  }

  private void applyPublicPermissionsForCreator(DbSession dbSession, ProjectDto projectDto, @Nullable String userUuid) {
    if (userUuid != null) {
      UserDto userDto = dbClient.userDao().selectByUuid(dbSession, userUuid);
      checkState(userDto != null, "User with uuid '%s' doesn't exist", userUuid);
      userPermissionUpdater.apply(dbSession,
        PUBLIC_PERMISSIONS.stream()
        .map(permission -> toUserPermissionChange(permission, projectDto, userDto))
        .collect(Collectors.toSet()));
    }
  }

  private UserPermissionChange toUserPermissionChange(ProjectPermission permission, ProjectDto projectDto, UserDto userDto) {
    return new UserPermissionChange(Operation.ADD, permission.getKey(), projectDto, userDto, permissionService);
  }

  private void addToFavourites(DbSession dbSession, ProjectDto projectDto, @Nullable String userUuid, @Nullable String userLogin) {
    if (permissionTemplateService.hasDefaultTemplateWithPermissionOnProjectCreator(dbSession, projectDto)) {
      favoriteUpdater.add(dbSession, projectDto, userUuid, userLogin, false);
    }
  }

  private void checkKeyFormat(String qualifier, String key) {
    checkRequest(isValidProjectKey(key), MALFORMED_KEY_ERROR, getQualifierToDisplay(qualifier), key, ALLOWED_CHARACTERS_MESSAGE);
  }

  private void checkKeyAlreadyExists(DbSession dbSession, NewComponent newComponent) {
    Optional<PortfolioDto> portfolios = dbClient.portfolioDao().selectByKey(dbSession, newComponent.key());
    if (portfolios.isPresent()) {
      throwBadRequestException("Could not create component with key: \"%s\". Key already in use.", newComponent.key());
    }
    List<ComponentDto> componentDtos = dbClient.componentDao().selectByKeyCaseInsensitive(dbSession, newComponent.key());
    if (!componentDtos.isEmpty()) {
      String alreadyExistingKeys = componentDtos
        .stream()
        .map(ComponentDto::getKey)
        .collect(Collectors.joining(", "));
      throwBadRequestException(KEY_ALREADY_EXISTS_ERROR, getQualifierToDisplay(newComponent.qualifier()), newComponent.key(), alreadyExistingKeys);
    }
  }

  private ComponentDto createRootComponent(DbSession session, NewComponent newComponent, long now) {
    String uuid = uuidFactory.create();

    ComponentDto component = new ComponentDto()
      .setUuid(uuid)
      .setUuidPath(ComponentDto.UUID_PATH_OF_ROOT)
      .setBranchUuid(uuid)
      .setKey(newComponent.key())
      .setName(newComponent.name())
      .setDescription(newComponent.description())
      .setLongName(newComponent.name())
      .setScope(ComponentScopes.PROJECT)
      .setQualifier(newComponent.qualifier())
      .setPrivate(newComponent.isPrivate())
      .setCreatedAt(new Date(now));

    dbClient.componentDao().insert(session, component, true);
    return component;
  }

  private ProjectDto toProjectDto(ComponentDto component, long now, CreationMethod creationMethod, boolean isAiCodeFixEnabled) {
    return new ProjectDto()
      .setUuid(uuidFactory.create())
      .setKey(component.getKey())
      .setQualifier(component.qualifier())
      .setName(component.name())
      .setPrivate(component.isPrivate())
      .setDescription(component.description())
      .setCreationMethod(creationMethod)
      .setAiCodeFixEnabled(isAiCodeFixEnabled)
      .setUpdatedAt(now)
      .setCreatedAt(now);
  }

  private static PortfolioDto toPortfolioDto(ComponentDto component, long now) {
    return new PortfolioDto()
      .setUuid(component.uuid())
      .setRootUuid(component.branchUuid())
      .setKey(component.getKey())
      .setName(component.name())
      .setPrivate(component.isPrivate())
      .setDescription(component.description())
      .setSelectionMode(SelectionMode.NONE.name())
      .setUpdatedAt(now)
      .setCreatedAt(now);
  }

  private static boolean isProjectOrApp(ComponentDto componentDto) {
    return PROJ_APP_QUALIFIERS.contains(componentDto.qualifier());
  }

  private static boolean isPortfolio(ComponentDto componentDto) {
    return ComponentQualifiers.VIEW.contains(componentDto.qualifier());
  }

  private BranchDto createMainBranch(DbSession session, String componentUuid, String projectUuid, @Nullable String mainBranch) {
    BranchDto branch = new BranchDto()
      .setBranchType(BranchType.BRANCH)
      .setUuid(componentUuid)
      .setIsMain(true)
      .setKey(Optional.ofNullable(mainBranch).orElse(defaultBranchNameResolver.getEffectiveMainBranchName()))
      .setMergeBranchUuid(null)
      .setExcludeFromPurge(true)
      .setProjectUuid(projectUuid);
    dbClient.branchDao().upsert(session, branch);
    return branch;
  }

  private String getQualifierToDisplay(String qualifier) {
    return i18n.message(Locale.getDefault(), "qualifier." + qualifier, "Project");
  }

}
