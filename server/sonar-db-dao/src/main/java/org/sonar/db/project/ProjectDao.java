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
package org.sonar.db.project;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.api.utils.System2;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.Pagination;
import org.sonar.db.audit.AuditPersister;
import org.sonar.db.audit.model.ComponentNewValue;

import static java.util.Collections.emptyList;
import static org.sonar.db.DatabaseUtils.executeLargeInputs;

public class ProjectDao implements Dao {
  private final System2 system2;
  private final AuditPersister auditPersister;

  public ProjectDao(System2 system2, AuditPersister auditPersister) {
    this.system2 = system2;
    this.auditPersister = auditPersister;
  }

  public void insert(DbSession session, ProjectDto project) {
    this.insert(session, project, false);
  }

  public void insert(DbSession session, ProjectDto project, boolean track) {
    if (track) {
      auditPersister.addComponent(session, new ComponentNewValue(project));
    }
    mapper(session).insert(project);
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

  public List<ProjectDto> selectAllApplications(DbSession session) {
    return mapper(session).selectAllApplications();
  }

  public List<ProjectDto> selectProjectsByKeys(DbSession session, Collection<String> keys) {
    if (keys.isEmpty()) {
      return emptyList();
    }
    return mapper(session).selectProjectsByKeys(keys);
  }

  public List<ProjectDto> selectApplicationsByKeys(DbSession session, Set<String> keys) {
    if (keys.isEmpty()) {
      return emptyList();
    }

    return executeLargeInputs(keys, partition -> mapper(session).selectApplicationsByKeys(partition));
  }

  public Optional<ProjectDto> selectByBranchUuid(DbSession dbSession, String branchUuid) {
    return Optional.ofNullable(mapper(dbSession).selectByBranchUuid(branchUuid));
  }

  public List<ProjectDto> selectProjects(DbSession session) {
    return mapper(session).selectProjects();
  }

  public Optional<ProjectDto> selectByUuid(DbSession session, String uuid) {
    return Optional.ofNullable(mapper(session).selectByUuid(uuid));
  }

  public List<ProjectDto> selectAll(DbSession session) {
    return mapper(session).selectAll();
  }

  public List<ProjectDto> selectByUuids(DbSession session, Set<String> uuids) {
    if (uuids.isEmpty()) {
      return emptyList();
    }
    return executeLargeInputs(uuids, partition -> mapper(session).selectByUuids(partition));
  }

  public List<ProjectDto> selectByUuids(DbSession session, Set<String> uuids, Pagination pagination) {
    if (uuids.isEmpty()) {
      return emptyList();
    }
    return mapper(session).selectByUuidsWithPagination(uuids, pagination);
  }

  public void updateVisibility(DbSession session, String uuid, boolean isPrivate) {
    mapper(session).updateVisibility(uuid, isPrivate, system2.now());
  }

  public void updateAiCodeAssurance(DbSession session, String uuid, boolean aiCodeAssurance) {
    mapper(session).updateAiCodeAssurance(uuid, aiCodeAssurance, system2.now());
  }

  public void updateAiCodeFixEnablementForAllProjects(DbSession dbSession, boolean featureEnablement) {
    mapper(dbSession).updateAiCodeFixEnablementForAllProjects(featureEnablement, system2.now());
  }

  public void updateTags(DbSession session, ProjectDto project) {
    mapper(session).updateTags(project);
  }

  public void update(DbSession session, ProjectDto project) {
    auditPersister.updateComponent(session, new ComponentNewValue(project));
    mapper(session).update(project);
  }

  private static ProjectMapper mapper(DbSession session) {
    return session.getMapper(ProjectMapper.class);
  }

  public void updateNcloc(DbSession dbSession, String projectUuid, long ncloc) {
    mapper(dbSession).updateNcloc(projectUuid, ncloc);
  }

  public long getNclocSum(DbSession dbSession) {
    return getNclocSum(dbSession, null);
  }

  public long getNclocSum(DbSession dbSession, @Nullable String projectUuidToExclude) {
    return Optional.ofNullable(mapper(dbSession).getNclocSum(projectUuidToExclude)).orElse(0L);
  }

  public int countIndexedProjects(DbSession session) {
    return mapper(session).countIndexedProjects();
  }

  public int countProjects(DbSession session) {
    return mapper(session).countProjects();
  }
}
