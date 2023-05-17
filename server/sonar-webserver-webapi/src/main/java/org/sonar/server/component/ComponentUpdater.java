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
package org.sonar.server.component;

import com.google.common.annotations.VisibleForTesting;
import java.util.Date;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Scopes;
import org.sonar.api.utils.System2;
import org.sonar.core.i18n.I18n;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.portfolio.PortfolioDto;
import org.sonar.db.portfolio.PortfolioDto.SelectionMode;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.es.ProjectIndexer.Cause;
import org.sonar.server.es.ProjectIndexers;
import org.sonar.server.favorite.FavoriteUpdater;
import org.sonar.server.permission.PermissionTemplateService;
import org.sonar.server.project.DefaultBranchNameResolver;
import org.springframework.beans.factory.annotation.Autowired;

import static java.util.Collections.singletonList;
import static org.sonar.core.component.ComponentKeys.ALLOWED_CHARACTERS_MESSAGE;
import static org.sonar.core.component.ComponentKeys.isValidProjectKey;
import static org.sonar.server.exceptions.BadRequestException.checkRequest;
import static org.sonar.server.exceptions.BadRequestException.throwBadRequestException;

public class ComponentUpdater {

  private static final Set<String> MAIN_BRANCH_QUALIFIERS = Set.of(Qualifiers.PROJECT, Qualifiers.APP);
  private static final String KEY_ALREADY_EXISTS_ERROR = "Could not create %s with key: \"%s\". A similar key already exists: \"%s\"";
  private static final String MALFORMED_KEY_ERROR = "Malformed key for %s: '%s'. %s.";
  private final DbClient dbClient;
  private final I18n i18n;
  private final System2 system2;
  private final PermissionTemplateService permissionTemplateService;
  private final FavoriteUpdater favoriteUpdater;
  private final ProjectIndexers projectIndexers;
  private final UuidFactory uuidFactory;
  private final DefaultBranchNameResolver defaultBranchNameResolver;
  private final boolean useDifferentUuids;

  @Autowired
  public ComponentUpdater(DbClient dbClient, I18n i18n, System2 system2,
    PermissionTemplateService permissionTemplateService, FavoriteUpdater favoriteUpdater,
    ProjectIndexers projectIndexers, UuidFactory uuidFactory, DefaultBranchNameResolver defaultBranchNameResolver) {
    this(dbClient, i18n, system2, permissionTemplateService, favoriteUpdater, projectIndexers, uuidFactory, defaultBranchNameResolver, false);
  }

  @VisibleForTesting
  public ComponentUpdater(DbClient dbClient, I18n i18n, System2 system2,
    PermissionTemplateService permissionTemplateService, FavoriteUpdater favoriteUpdater,
    ProjectIndexers projectIndexers, UuidFactory uuidFactory, DefaultBranchNameResolver defaultBranchNameResolver,
    boolean useDifferentUuids) {
    this.dbClient = dbClient;
    this.i18n = i18n;
    this.system2 = system2;
    this.permissionTemplateService = permissionTemplateService;
    this.favoriteUpdater = favoriteUpdater;
    this.projectIndexers = projectIndexers;
    this.uuidFactory = uuidFactory;
    this.defaultBranchNameResolver = defaultBranchNameResolver;
    this.useDifferentUuids = useDifferentUuids;
  }

  /**
   * - Create component
   * - Apply default permission template
   * - Add component to favorite if the component has the 'Project Creators' permission
   * - Index component in es indexes
   */
  public ComponentCreationData create(DbSession dbSession, NewComponent newComponent, @Nullable String userUuid, @Nullable String userLogin) {
    return create(dbSession, newComponent, userUuid, userLogin, null);
  }

  public ComponentCreationData create(DbSession dbSession, NewComponent newComponent, @Nullable String userUuid, @Nullable String userLogin,
    @Nullable String mainBranchName) {
    ComponentCreationData componentCreationData = createWithoutCommit(dbSession, newComponent, userUuid, userLogin, mainBranchName, c -> {
    });
    commitAndIndex(dbSession, componentCreationData.mainBranchComponent());
    return componentCreationData;
  }

  public void commitAndIndex(DbSession dbSession, ComponentDto componentDto) {
    projectIndexers.commitAndIndexComponents(dbSession, singletonList(componentDto), Cause.PROJECT_CREATION);
  }

  /**
   * Create component without committing.
   * Don't forget to call commitAndIndex(...) when ready to commit.
   */
  public ComponentCreationData createWithoutCommit(DbSession dbSession, NewComponent newComponent,
    @Nullable String userUuid, @Nullable String userLogin, Consumer<ComponentDto> componentModifier) {
    return createWithoutCommit(dbSession, newComponent, userUuid, userLogin, null, componentModifier);
  }

  /**
   * Create component without committing.
   * Don't forget to call commitAndIndex(...) when ready to commit.
   */
  public ComponentCreationData createWithoutCommit(DbSession dbSession, NewComponent newComponent,
    @Nullable String userUuid, @Nullable String userLogin, @Nullable String mainBranchName,
    Consumer<ComponentDto> componentModifier) {
    checkKeyFormat(newComponent.qualifier(), newComponent.key());
    checkKeyAlreadyExists(dbSession, newComponent);

    long now = system2.now();

    ComponentDto componentDto = createRootComponent(dbSession, newComponent, componentModifier, now);

    BranchDto mainBranch = null;
    ProjectDto projectDto = null;

    if (isRootProject(componentDto)) {
      projectDto = toProjectDto(componentDto, now);
      dbClient.projectDao().insert(dbSession, projectDto);
      addToFavourites(dbSession, projectDto, userUuid, userLogin, componentDto);
      mainBranch = createMainBranch(dbSession, componentDto.uuid(), projectDto.getUuid(), mainBranchName);
    }

    if (isRootView(componentDto)) {
      PortfolioDto portfolioDto = toPortfolioDto(componentDto, now);
      dbClient.portfolioDao().insert(dbSession, portfolioDto);
    }

    // TODO permissions: probably we want to apply it to the projectDto or portfolioDto
    permissionTemplateService.applyDefaultToNewComponent(dbSession, componentDto, userUuid);
    return new ComponentCreationData(componentDto, mainBranch, projectDto);
  }

  // TODO, when working on permissions, maybe we could remove the last argument from this method
  private void addToFavourites(DbSession dbSession, ProjectDto projectDto, @Nullable String userUuid, @Nullable String userLogin, ComponentDto componentDto) {
    if (permissionTemplateService.hasDefaultTemplateWithPermissionOnProjectCreator(dbSession, componentDto)) {
      favoriteUpdater.add(dbSession, projectDto, userUuid, userLogin, false);
    }
  }

  private void checkKeyFormat(String qualifier, String key) {
    checkRequest(isValidProjectKey(key), MALFORMED_KEY_ERROR, getQualifierToDisplay(qualifier), key, ALLOWED_CHARACTERS_MESSAGE);
  }

  private void checkKeyAlreadyExists(DbSession dbSession, NewComponent newComponent) {
    Optional<ComponentDto> componentDto = newComponent.isProject()
      ? dbClient.componentDao().selectByKeyCaseInsensitive(dbSession, newComponent.key())
      : dbClient.componentDao().selectByKey(dbSession, newComponent.key());

    componentDto.map(ComponentDto::getKey)
      .ifPresent(existingKey -> throwBadRequestException(KEY_ALREADY_EXISTS_ERROR, getQualifierToDisplay(newComponent.qualifier()), newComponent.key(), existingKey));
  }

  private ComponentDto createRootComponent(DbSession session, NewComponent newComponent, Consumer<ComponentDto> componentModifier, long now) {
    String uuid = uuidFactory.create();

    ComponentDto component = new ComponentDto()
      .setUuid(uuid)
      .setUuidPath(ComponentDto.UUID_PATH_OF_ROOT)
      .setBranchUuid(uuid)
      .setKey(newComponent.key())
      .setName(newComponent.name())
      .setDescription(newComponent.description())
      .setLongName(newComponent.name())
      .setScope(Scopes.PROJECT)
      .setQualifier(newComponent.qualifier())
      .setPrivate(newComponent.isPrivate())
      .setCreatedAt(new Date(now));

    componentModifier.accept(component);
    dbClient.componentDao().insert(session, component, true);
    return component;
  }

  private ProjectDto toProjectDto(ComponentDto component, long now) {
    return new ProjectDto()
      .setUuid(useDifferentUuids ? uuidFactory.create() : component.uuid())
      .setKey(component.getKey())
      .setQualifier(component.qualifier())
      .setName(component.name())
      .setPrivate(component.isPrivate())
      .setDescription(component.description())
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

  private static boolean isRootProject(ComponentDto componentDto) {
    return Scopes.PROJECT.equals(componentDto.scope())
      && MAIN_BRANCH_QUALIFIERS.contains(componentDto.qualifier());
  }

  private static boolean isRootView(ComponentDto componentDto) {
    return Scopes.PROJECT.equals(componentDto.scope())
      && Qualifiers.VIEW.contains(componentDto.qualifier());
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
