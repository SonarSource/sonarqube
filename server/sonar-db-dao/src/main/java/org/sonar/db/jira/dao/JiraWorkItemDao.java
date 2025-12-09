/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.db.jira.dao;

import java.util.List;
import java.util.Optional;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.jira.JiraWorkItemMapper;
import org.sonar.db.jira.dto.JiraWorkItemDto;

public class JiraWorkItemDao implements Dao {

  private final System2 system2;
  private final UuidFactory uuidFactory;

  public JiraWorkItemDao(System2 system2, UuidFactory uuidFactory) {
    this.system2 = system2;
    this.uuidFactory = uuidFactory;
  }

  public JiraWorkItemDto insertOrUpdate(DbSession dbSession, JiraWorkItemDto dto) {
    long now = system2.now();
    var mapper = getMapper(dbSession);

    dto.setUpdatedAt(now);
    if (dto.getId() == null) {
      dto
        .setId(uuidFactory.create())
        .setCreatedAt(now);
      mapper.insert(dto);
    } else {
      mapper.update(dto);
    }

    return dto;
  }

  public Optional<JiraWorkItemDto> findById(DbSession dbSession, String id) {
    return Optional.ofNullable(getMapper(dbSession).findById(id));
  }

  public List<JiraWorkItemDto> findByResource(DbSession dbSession, String sonarProjectId, String resourceId, String resourceType) {
    return getMapper(dbSession).findByResource(sonarProjectId, resourceId, resourceType);
  }

  public int deleteById(DbSession dbSession, String id) {
    return getMapper(dbSession).deleteById(id);
  }

  public int deleteLinkedResource(
    DbSession dbSession,
    String workItemId,
    String resourceId,
    String resourceType
  ) {
    return getMapper(dbSession).deleteLinkedResource(workItemId, resourceId, resourceType);
  }

  public void insertLinkedResource(
    DbSession dbSession,
    String workItemId,
    String resourceId,
    String resourceType
  ) {
    getMapper(dbSession).insertLinkedResource(
      uuidFactory.create(),
      workItemId,
      resourceId,
      resourceType,
      system2.now()
    );
  }

  public int countAll(DbSession dbSession) {
    return getMapper(dbSession).countAll();
  }

  public int countDistinctResourcesByType(DbSession dbSession, String resourceType) {
    return getMapper(dbSession).countDistinctResourcesByType(resourceType);
  }

  public int countDistinctCreators(DbSession dbSession) {
    return getMapper(dbSession).countDistinctCreators();
  }

  public int countBySonarProjectId(DbSession dbSession, String sonarProjectId) {
    return getMapper(dbSession).countBySonarProjectId(sonarProjectId);
  }

  private static JiraWorkItemMapper getMapper(DbSession dbSession) {
    return dbSession.getMapper(JiraWorkItemMapper.class);
  }
}
