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
package org.sonar.db.provisioning;

import java.util.Optional;
import java.util.Set;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;

public class GithubOrganizationGroupDao implements Dao {

  public Set<GithubOrganizationGroupDto> findAll(DbSession dbSession) {
    return mapper(dbSession).selectAll();
  }

  public void insert(DbSession dbSession, GithubOrganizationGroupDto githubOrganizationGroupDto) {
    mapper(dbSession).insert(githubOrganizationGroupDto);
  }

  public Optional<GithubOrganizationGroupDto> selectByGroupUuid(DbSession dbSession, String groupUuid) {
    return mapper(dbSession).selectByGroupUuid(groupUuid);
  }

  public void deleteByGroupUuid(DbSession dbSession, String groupUuid) {
    mapper(dbSession).deleteByGroupUuid(groupUuid);
  }

  public void deleteAll(DbSession dbSession) {
    mapper(dbSession).deleteAll();
  }

  private static GithubOrganizationGroupMapper mapper(DbSession session) {
    return session.getMapper(GithubOrganizationGroupMapper.class);
  }

}
