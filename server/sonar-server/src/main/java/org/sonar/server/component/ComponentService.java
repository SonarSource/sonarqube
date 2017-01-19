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
package org.sonar.server.component;

import com.google.common.base.Joiner;
import com.google.common.collect.Collections2;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.i18n.I18n;
import org.sonar.api.resources.Scopes;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.core.component.ComponentKeys;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.component.index.ComponentIndexer;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.measure.index.ProjectMeasuresIndexer;
import org.sonar.server.user.UserSession;

import static com.google.common.collect.Lists.newArrayList;
import static org.sonar.core.component.ComponentKeys.isValidModuleKey;
import static org.sonar.db.component.ComponentDtoFunctions.toKey;
import static org.sonar.db.component.ComponentKeyUpdaterDao.checkIsProjectOrModule;
import static org.sonar.server.ws.WsUtils.checkRequest;

@ServerSide
@ComputeEngineSide
public class ComponentService {
  private final DbClient dbClient;
  private final I18n i18n;
  private final UserSession userSession;
  private final System2 system2;
  private final ProjectMeasuresIndexer projectMeasuresIndexer;
  private final ComponentIndexer componentIndexer;

  public ComponentService(DbClient dbClient, I18n i18n, UserSession userSession, System2 system2, ProjectMeasuresIndexer projectMeasuresIndexer,
    ComponentIndexer componentIndexer) {
    this.dbClient = dbClient;
    this.i18n = i18n;
    this.userSession = userSession;
    this.system2 = system2;
    this.projectMeasuresIndexer = projectMeasuresIndexer;
    this.componentIndexer = componentIndexer;
  }

  public void updateKey(DbSession dbSession, ComponentDto component, String newKey) {
    userSession.checkComponentUuidPermission(UserRole.ADMIN, component.projectUuid());
    checkIsProjectOrModule(component);
    checkProjectOrModuleKeyFormat(newKey);
    dbClient.componentKeyUpdaterDao().updateKey(component.uuid(), newKey);
    dbSession.commit();
    index(component.uuid());
  }

  public void bulkUpdateKey(DbSession dbSession, String projectUuid, String stringToReplace, String replacementString) {
    dbClient.componentKeyUpdaterDao().bulkUpdateKey(dbSession, projectUuid, stringToReplace, replacementString);
    dbSession.commit();
    index(projectUuid);
  }

  // Used by SQ and Governance
  public ComponentDto create(DbSession session, NewComponent newComponent) {
    checkKeyFormat(newComponent.qualifier(), newComponent.key());
    ComponentDto rootComponent = createRootComponent(session, newComponent);
    removeDuplicatedProjects(session, rootComponent.getKey());
    index(rootComponent.uuid());
    return rootComponent;
  }

  private void index(String projectUuid) {
    projectMeasuresIndexer.index(projectUuid);
    componentIndexer.indexByProjectUuid(projectUuid);
  }

  /**
   * No permission check must be done when inserting a new developer as it's done on Compute Engine side.
   * No check must be done on the key
   * No need to remove duplicated components as it's not possible to create the same developer twice in the same time.
   */
  public ComponentDto createDeveloper(DbSession session, NewComponent newComponent) {
    return createRootComponent(session, newComponent);
  }

  private ComponentDto createRootComponent(DbSession session, NewComponent newComponent) {
    checkBranchFormat(newComponent.qualifier(), newComponent.branch());
    String keyWithBranch = ComponentKeys.createKey(newComponent.key(), newComponent.branch());
    if (dbClient.componentDao().selectByKey(session, keyWithBranch).isPresent()) {
      throw new BadRequestException(formatMessage("Could not create %s, key already exists: %s", newComponent.qualifier(), keyWithBranch));
    }

    String uuid = Uuids.create();
    ComponentDto component = new ComponentDto()
      .setOrganizationUuid(newComponent.getOrganizationUuid())
      .setUuid(uuid)
      .setUuidPath(ComponentDto.UUID_PATH_OF_ROOT)
      .setRootUuid(uuid)
      .setModuleUuid(null)
      .setModuleUuidPath(ComponentDto.UUID_PATH_SEPARATOR + uuid + ComponentDto.UUID_PATH_SEPARATOR)
      .setProjectUuid(uuid)
      .setKey(keyWithBranch)
      .setDeprecatedKey(keyWithBranch)
      .setName(newComponent.name())
      .setLongName(newComponent.name())
      .setScope(Scopes.PROJECT)
      .setQualifier(newComponent.qualifier())
      .setCreatedAt(new Date(system2.now()));
    dbClient.componentDao().insert(session, component);
    dbClient.componentIndexDao().indexResource(session, component.uuid());
    session.commit();
    return component;
  }

  /**
   * On MySQL, as PROJECTS.KEE is not unique, if the same project is provisioned multiple times, then it will be duplicated in the database.
   * So, after creating a project, we commit, and we search in the db if their are some duplications and we remove them.
   *
   * SONAR-6332
   */
  private void removeDuplicatedProjects(DbSession session, String projectKey) {
    List<ComponentDto> duplicated = dbClient.componentDao().selectComponentsHavingSameKeyOrderedById(session, projectKey);
    for (int i = 1; i < duplicated.size(); i++) {
      dbClient.componentDao().delete(session, duplicated.get(i).getId());
    }
    session.commit();
  }

  public Collection<String> componentUuids(DbSession session, @Nullable Collection<String> componentKeys, boolean ignoreMissingComponents) {
    Collection<String> componentUuids = newArrayList();
    if (componentKeys != null && !componentKeys.isEmpty()) {
      List<ComponentDto> components = dbClient.componentDao().selectByKeys(session, componentKeys);

      if (!ignoreMissingComponents && components.size() < componentKeys.size()) {
        Collection<String> foundKeys = Collections2.transform(components, toKey());
        Set<String> missingKeys = Sets.newHashSet(componentKeys);
        missingKeys.removeAll(foundKeys);
        throw new NotFoundException("The following component keys do not match any component:\n" +
          Joiner.on('\n').join(missingKeys));
      }

      for (ComponentDto component : components) {
        componentUuids.add(component.uuid());
      }
    }
    return componentUuids;
  }

  public Set<String> getDistinctQualifiers(DbSession session, @Nullable Collection<String> componentUuids) {
    Set<String> componentQualifiers = Sets.newHashSet();
    if (componentUuids != null && !componentUuids.isEmpty()) {
      List<ComponentDto> components = dbClient.componentDao().selectByUuids(session, componentUuids);

      for (ComponentDto component : components) {
        componentQualifiers.add(component.qualifier());
      }
    }
    return componentQualifiers;
  }

  public Collection<ComponentDto> getByUuids(DbSession session, @Nullable Collection<String> componentUuids) {
    Set<ComponentDto> directoryPaths = Sets.newHashSet();
    if (componentUuids != null && !componentUuids.isEmpty()) {
      List<ComponentDto> components = dbClient.componentDao().selectByUuids(session, componentUuids);

      for (ComponentDto component : components) {
        directoryPaths.add(component);
      }
    }
    return directoryPaths;
  }

  private void checkKeyFormat(String qualifier, String kee) {
    checkRequest(isValidModuleKey(kee), formatMessage("Malformed key for %s: %s. Allowed characters are alphanumeric, '-', '_', '.' and ':', with at least one non-digit.",
      qualifier, kee));
  }

  private static void checkProjectOrModuleKeyFormat(String key) {
    checkRequest(isValidModuleKey(key), "Malformed key for '%s'. Allowed characters are alphanumeric, '-', '_', '.' and ':', with at least one non-digit.", key);
  }

  private void checkBranchFormat(String qualifier, @Nullable String branch) {
    if (branch != null && !ComponentKeys.isValidBranch(branch)) {
      throw new BadRequestException(formatMessage("Malformed branch for %s: %s. Allowed characters are alphanumeric, '-', '_', '.' and '/', with at least one non-digit.",
        qualifier, branch));
    }
  }

  private String formatMessage(String message, String qualifier, String key) {
    return String.format(message, i18n.message(Locale.getDefault(), "qualifier." + qualifier, "Project"), key);
  }

}
