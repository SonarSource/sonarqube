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

package org.sonar.db.organization;

import java.util.Optional;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;

public class OrganizationMemberDao implements Dao {
  private static OrganizationMemberMapper mapper(DbSession dbSession) {
    return dbSession.getMapper(OrganizationMemberMapper.class);
  }

  public Optional<OrganizationMemberDto> select(DbSession dbSession, String organizationUuid, int userId) {
    return Optional.ofNullable(mapper(dbSession).select(organizationUuid, userId));
  }

  public void insert(DbSession dbSession, OrganizationMemberDto organizationMemberDto) {
    mapper(dbSession).insert(organizationMemberDto);
  }

  public void delete(DbSession dbSession, String organizationMemberUuid, Integer userId) {
    mapper(dbSession).delete(organizationMemberUuid, userId);
  }
}
