/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
package org.sonar.db.project;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.sonar.api.utils.System2;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;

public class ProjectDao implements Dao {
  private final System2 system2;

  public ProjectDao(System2 system2) {
    this.system2 = system2;
  }

  public void insert(DbSession session, ProjectDto item) {
    mapper(session).insert(item);
  }

  public Optional<ProjectDto> selectProjectByKey(DbSession session, String key) {
    return Optional.ofNullable(mapper(session).selectProjectByKey(key));
  }

  public Optional<ProjectDto> selectApplicationByKey(DbSession session, String key) {
    return Optional.ofNullable(mapper(session).selectApplicationByKey(key));
  }

  public Optional<ProjectDto> selectProjectOrAppByKey(DbSession session, String key) {
    return Optional.ofNullable(mapper(session).selectProjectOrAppByKey(key));
  }

  public List<ProjectDto> selectProjectsByKeys(DbSession session, Set<String> keys) {
    if (keys.isEmpty()) {
      return Collections.emptyList();
    }
    return mapper(session).selectProjectsByKeys(keys);
  }

  public List<ProjectDto> selectProjects(DbSession session) {
    return mapper(session).selectProjects();
  }

  public Optional<ProjectDto> selectByUuid(DbSession session, String uuid) {
    return Optional.ofNullable(mapper(session).selectByUuid(uuid));
  }

  public List<ProjectDto> selectByOrganizationUuid(DbSession session, String organizationUuid) {
    return mapper(session).selectByOrganizationUuid(organizationUuid);
  }

  public List<ProjectDto> selectProjectsByOrganizationUuid(DbSession session, String organizationUuid) {
    return mapper(session).selectProjectsByOrganizationUuid(organizationUuid);
  }

  public List<ProjectDto> selectByUuids(DbSession session, Set<String> uuids) {
    if (uuids.isEmpty()) {
      return Collections.emptyList();
    }
    return mapper(session).selectByUuids(uuids);
  }

  public void updateKey(DbSession session, String uuid, String newKey) {
    mapper(session).updateKey(uuid, newKey, system2.now());
  }

  public void updateVisibility(DbSession session, String uuid, boolean isPrivate) {
    mapper(session).updateVisibility(uuid, isPrivate, system2.now());
  }

  public void updateTags(DbSession session, ProjectDto project) {
    mapper(session).updateTags(project);
  }

  public void update(DbSession session, ProjectDto project) {
    mapper(session).update(project);
  }

  private static ProjectMapper mapper(DbSession session) {
    return session.getMapper(ProjectMapper.class);
  }
}
