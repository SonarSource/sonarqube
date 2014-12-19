/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.component;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Collections2;
import org.apache.commons.collections.CollectionUtils;
import org.sonar.api.ServerComponent;
import org.sonar.api.web.UserRole;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.preview.PreviewCache;
import org.sonar.core.resource.ResourceKeyUpdaterDao;
import org.sonar.server.db.DbClient;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;

public class ComponentService implements ServerComponent {

  private final DbClient dbClient;

  private final ResourceKeyUpdaterDao resourceKeyUpdaterDao;
  private final PreviewCache previewCache;

  public ComponentService(DbClient dbClient, ResourceKeyUpdaterDao resourceKeyUpdaterDao, PreviewCache previewCache) {
    this.dbClient = dbClient;
    this.resourceKeyUpdaterDao = resourceKeyUpdaterDao;
    this.previewCache = previewCache;
  }

  public ComponentDto getByKey(String key) {
    DbSession session = dbClient.openSession(false);
    try {
      return dbClient.componentDao().getByKey(session, key);
    } finally {
      session.close();
    }
  }

  @CheckForNull
  public ComponentDto getNullableByKey(String key) {
    DbSession session = dbClient.openSession(false);
    try {
      return dbClient.componentDao().getNullableByKey(session, key);
    } finally {
      session.close();
    }
  }

  public ComponentDto getByUuid(String uuid) {
    DbSession session = dbClient.openSession(false);
    try {
      return dbClient.componentDao().getByUuid(session, uuid);
    } finally {
      session.close();
    }
  }

  @CheckForNull
  public ComponentDto getNullableByUuid(String uuid) {
    DbSession session = dbClient.openSession(false);
    try {
      return dbClient.componentDao().getNullableByUuid(session, uuid);
    } finally {
      session.close();
    }
  }

  public void updateKey(String projectOrModuleKey, String newKey) {
    UserSession.get().checkComponentPermission(UserRole.ADMIN, projectOrModuleKey);

    DbSession session = dbClient.openSession(false);
    try {
      ComponentDto projectOrModule = getByKey(projectOrModuleKey);
      resourceKeyUpdaterDao.updateKey(projectOrModule.getId(), newKey);
      session.commit();

      previewCache.reportResourceModification(newKey);

      session.commit();
    } finally {
      session.close();
    }
  }

  public Map<String, String> checkModuleKeysBeforeRenaming(String projectKey, String stringToReplace, String replacementString) {
    UserSession.get().checkProjectPermission(UserRole.ADMIN, projectKey);
    DbSession session = dbClient.openSession(false);
    try {
      ComponentDto project = getByKey(projectKey);
      return resourceKeyUpdaterDao.checkModuleKeysBeforeRenaming(project.getId(), stringToReplace, replacementString);
    } finally {
      session.close();
    }
  }

  public void bulkUpdateKey(String projectKey, String stringToReplace, String replacementString) {
    UserSession.get().checkProjectPermission(UserRole.ADMIN, projectKey);

    DbSession session = dbClient.openSession(false);
    try {
      ComponentDto project = getByKey(projectKey);

      resourceKeyUpdaterDao.bulkUpdateKey(project.getId(), stringToReplace, replacementString);
      session.commit();

      ComponentDto newProject = dbClient.componentDao().getById(project.getId(), session);
      previewCache.reportResourceModification(newProject.key());

      session.commit();
    } finally {
      session.close();
    }
  }

  public Collection<String> componentUuids(@Nullable Collection<String> componentKeys) {
    DbSession session = dbClient.openSession(false);
    try {
      return componentUuids(session, componentKeys, false);
    } finally {
      session.close();
    }
  }

  public Collection<String> componentUuids(DbSession session, @Nullable Collection<String> componentKeys, boolean ignoreMissingComponents) {
    Collection<String> componentUuids = newArrayList();
    if (componentKeys != null && !componentKeys.isEmpty()) {
      List<ComponentDto> components = dbClient.componentDao().getByKeys(session, componentKeys);

      if (!ignoreMissingComponents && components.size() < componentKeys.size()) {
        Collection<String> foundKeys = Collections2.transform(components, new Function<ComponentDto, String>() {
          @Override
          public String apply(ComponentDto component) {
            return component.key();
          }
        });
        throw new NotFoundException("The following component keys do not match any component:\n" +
          Joiner.on('\n').join(CollectionUtils.subtract(componentKeys, foundKeys)));
      }

      for (ComponentDto component : components) {
        componentUuids.add(component.uuid());
      }
    }
    return componentUuids;
  }
}
