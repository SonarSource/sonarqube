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
package org.sonar.server.component;

import com.google.common.base.Joiner;
import com.google.common.collect.Collections2;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.server.ServerSide;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.es.ProjectIndexer;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;

import static com.google.common.collect.Lists.newArrayList;
import static org.sonar.core.component.ComponentKeys.isValidModuleKey;
import static org.sonar.db.component.ComponentKeyUpdaterDao.checkIsProjectOrModule;
import static org.sonar.server.ws.WsUtils.checkRequest;

@ServerSide
@ComputeEngineSide
public class ComponentService {
  private final DbClient dbClient;
  private final UserSession userSession;
  private final ProjectIndexer[] projectIndexers;

  public ComponentService(DbClient dbClient, UserSession userSession, ProjectIndexer... projectIndexers) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.projectIndexers = projectIndexers;
  }

  // TODO should be moved to ComponentUpdater
  public void updateKey(DbSession dbSession, ComponentDto component, String newKey) {
    userSession.checkComponentPermission(UserRole.ADMIN, component);
    checkIsProjectOrModule(component);
    checkProjectOrModuleKeyFormat(newKey);
    dbClient.componentKeyUpdaterDao().updateKey(component.uuid(), newKey);
    dbSession.commit();
    index(component.uuid());
  }

  // TODO should be moved to ComponentUpdater
  public void bulkUpdateKey(DbSession dbSession, String projectUuid, String stringToReplace, String replacementString) {
    dbClient.componentKeyUpdaterDao().bulkUpdateKey(dbSession, projectUuid, stringToReplace, replacementString);
    dbSession.commit();
    index(projectUuid);
  }

  private void index(String projectUuid) {
    for (ProjectIndexer projectIndexer : projectIndexers) {
      projectIndexer.indexProject(projectUuid, ProjectIndexer.Cause.PROJECT_KEY_UPDATE);
    }
  }

  public Collection<String> componentUuids(DbSession session, @Nullable Collection<String> componentKeys, boolean ignoreMissingComponents) {
    Collection<String> componentUuids = newArrayList();
    if (componentKeys != null && !componentKeys.isEmpty()) {
      List<ComponentDto> components = dbClient.componentDao().selectByKeys(session, componentKeys);

      if (!ignoreMissingComponents && components.size() < componentKeys.size()) {
        Collection<String> foundKeys = Collections2.transform(components, ComponentDto::getKey);
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

  private static void checkProjectOrModuleKeyFormat(String key) {
    checkRequest(isValidModuleKey(key), "Malformed key for '%s'. Allowed characters are alphanumeric, '-', '_', '.' and ':', with at least one non-digit.", key);
  }

}
