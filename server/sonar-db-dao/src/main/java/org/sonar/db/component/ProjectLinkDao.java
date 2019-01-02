/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.db.component;

import java.util.List;
import javax.annotation.CheckForNull;
import org.sonar.api.utils.System2;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;

import static org.sonar.db.DatabaseUtils.executeLargeInputs;

public class ProjectLinkDao implements Dao {

  private final System2 system2;

  public ProjectLinkDao(System2 system2) {
    this.system2 = system2;
  }

  public List<ProjectLinkDto> selectByProjectUuid(DbSession session, String projectUuid) {
    return session.getMapper(ProjectLinkMapper.class).selectByProjectUuid(projectUuid);
  }

  public List<ProjectLinkDto> selectByProjectUuids(DbSession dbSession, List<String> projectUuids) {
    return executeLargeInputs(projectUuids, mapper(dbSession)::selectByProjectUuids);
  }

  @CheckForNull
  public ProjectLinkDto selectByUuid(DbSession session, String uuid) {
    return session.getMapper(ProjectLinkMapper.class).selectByUuid(uuid);
  }

  public ProjectLinkDto insert(DbSession session, ProjectLinkDto dto) {
    long now = system2.now();
    session.getMapper(ProjectLinkMapper.class).insert(dto.setCreatedAt(now).setUpdatedAt(now));
    return dto;
  }

  public void update(DbSession session, ProjectLinkDto dto) {
    session.getMapper(ProjectLinkMapper.class).update(dto.setUpdatedAt(system2.now()));
  }

  public void delete(DbSession session, String uuid) {
    session.getMapper(ProjectLinkMapper.class).delete(uuid);
  }

  private static ProjectLinkMapper mapper(DbSession dbSession) {
    return dbSession.getMapper(ProjectLinkMapper.class);
  }

}
