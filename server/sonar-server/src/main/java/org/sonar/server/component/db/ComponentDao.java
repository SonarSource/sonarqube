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

package org.sonar.server.component.db;

import com.google.common.base.Function;
import org.sonar.api.ServerSide;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Scopes;
import org.sonar.api.utils.System2;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.component.FilePathWithHashDto;
import org.sonar.core.component.UuidWithProjectUuidDto;
import org.sonar.core.component.db.ComponentMapper;
import org.sonar.core.persistence.DaoComponent;
import org.sonar.core.persistence.DaoUtils;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.db.BaseDao;
import org.sonar.server.exceptions.NotFoundException;

import javax.annotation.CheckForNull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @since 4.3
 */
@ServerSide
public class ComponentDao extends BaseDao<ComponentMapper, ComponentDto, String> implements DaoComponent {

  public ComponentDao() {
    this(System2.INSTANCE);
  }

  public ComponentDao(System2 system) {
    super(ComponentMapper.class, system);
  }

  public ComponentDto getById(Long id, DbSession session) {
    ComponentDto componentDto = getNullableById(id, session);
    if (componentDto == null) {
      throw new NotFoundException(String.format("Project with id '%s' not found", id));
    }
    return componentDto;
  }

  @CheckForNull
  public ComponentDto getNullableById(Long id, DbSession session) {
    return mapper(session).selectById(id);
  }

  @CheckForNull
  public ComponentDto getNullableByUuid(DbSession session, String uuid) {
    return mapper(session).selectByUuid(uuid);
  }

  public ComponentDto getByUuid(DbSession session, String uuid) {
    ComponentDto componentDto = getNullableByUuid(session, uuid);
    if (componentDto == null) {
      throw new NotFoundException(String.format("Component with uuid '%s' not found", uuid));
    }
    return componentDto;
  }

  public boolean existsById(Long id, DbSession session) {
    return mapper(session).countById(id) > 0;
  }

  public List<ComponentDto> findModulesByProject(String projectKey, DbSession session) {
    return mapper(session).findModulesByProject(projectKey);
  }

  public List<ComponentDto> findSubProjectsByComponentUuids(DbSession session, Collection<String> keys) {
    if (keys.isEmpty()) {
      return Collections.emptyList();
    }
    return mapper(session).findSubProjectsByComponentUuids(keys);
  }

  public List<ComponentDto> selectDescendantModules(DbSession session, String rootComponentUuid) {
    return mapper(session).selectDescendantModules(rootComponentUuid, Scopes.PROJECT, false);
  }

  public List<ComponentDto> selectEnabledDescendantModules(DbSession session, String rootComponentUuid) {
    return mapper(session).selectDescendantModules(rootComponentUuid, Scopes.PROJECT, true);
  }

  public List<FilePathWithHashDto> selectEnabledDescendantFiles(DbSession session, String rootComponentUuid) {
    return mapper(session).selectDescendantFiles(rootComponentUuid, Scopes.FILE, true);
  }

  public List<FilePathWithHashDto> selectEnabledFilesFromProject(DbSession session, String rootComponentUuid) {
    return mapper(session).selectEnabledFilesFromProject(rootComponentUuid);
  }

  public List<ComponentDto> getByIds(final DbSession session, Collection<Long> ids) {
    return DaoUtils.executeLargeInputs(ids, new Function<List<Long>, List<ComponentDto>>() {
      @Override
      public List<ComponentDto> apply(List<Long> partition) {
        return mapper(session).findByIds(partition);
      }
    });
  }

  public List<ComponentDto> getByUuids(final DbSession session, Collection<String> uuids) {
    return DaoUtils.executeLargeInputs(uuids, new Function<List<String>, List<ComponentDto>>() {
      @Override
      public List<ComponentDto> apply(List<String> partition) {
        return mapper(session).findByUuids(partition);
      }
    });
  }

  public List<String> selectExistingUuids(final DbSession session, Collection<String> uuids) {
    return DaoUtils.executeLargeInputs(uuids, new Function<List<String>, List<String>>() {
      @Override
      public List<String> apply(List<String> partition) {
        return mapper(session).selectExistingUuids(partition);
      }
    });
  }

  @Override
  protected List<ComponentDto> doGetByKeys(DbSession session, Collection<String> keys) {
    return mapper(session).findByKeys(keys);
  }

  @Override
  @CheckForNull
  protected ComponentDto doGetNullableByKey(DbSession session, String key) {
    return mapper(session).selectByKey(key);
  }

  @Override
  protected ComponentDto doInsert(DbSession session, ComponentDto item) {
    mapper(session).insert(item);
    return item;
  }

  @Override
  protected void doDeleteByKey(DbSession session, String key) {
    mapper(session).deleteByKey(key);
  }

  public List<String> findProjectUuids(DbSession session) {
    return mapper(session).findProjectUuids();
  }

  public List<UuidWithProjectUuidDto> selectAllViewsAndSubViews(DbSession session) {
    return mapper(session).selectUuidsForQualifiers(Qualifiers.VIEW, Qualifiers.SUBVIEW);
  }

  public List<String> selectProjectsFromView(DbSession session, String viewUuid, String projectViewUuid) {
    return mapper(session).selectProjectsFromView("%." + viewUuid + ".%", projectViewUuid);
  }

}
