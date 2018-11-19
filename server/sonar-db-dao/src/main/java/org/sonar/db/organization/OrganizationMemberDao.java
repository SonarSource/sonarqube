/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;

import static org.sonar.db.DatabaseUtils.executeLargeInputsWithoutOutput;

public class OrganizationMemberDao implements Dao {
  private static OrganizationMemberMapper mapper(DbSession dbSession) {
    return dbSession.getMapper(OrganizationMemberMapper.class);
  }

  public Optional<OrganizationMemberDto> select(DbSession dbSession, String organizationUuid, int userId) {
    return Optional.ofNullable(mapper(dbSession).select(organizationUuid, userId));
  }

  public List<String> selectLoginsByOrganizationUuid(DbSession dbSession, String organizationUuid) {
    return mapper(dbSession).selectLogins(organizationUuid);
  }

  public List<Integer> selectUserIdsByOrganizationUuid(DbSession dbSession, String organizationUuid) {
    return mapper(dbSession).selectUserIds(organizationUuid);
  }

  public void insert(DbSession dbSession, OrganizationMemberDto organizationMemberDto) {
    mapper(dbSession).insert(organizationMemberDto);
  }

  public void delete(DbSession dbSession, String organizationMemberUuid, Integer userId) {
    mapper(dbSession).delete(organizationMemberUuid, userId);
  }

  public void deleteByOrganizationUuid(DbSession dbSession, String organizationMemberUuid) {
    mapper(dbSession).deleteByOrganization(organizationMemberUuid);
  }

  public void deleteByUserId(DbSession dbSession, int userId) {
    mapper(dbSession).deleteByUserId(userId);
  }

  public Set<String> selectOrganizationUuidsByUser(DbSession dbSession, int userId) {
    return mapper(dbSession).selectOrganizationUuidsByUser(userId);
  }

  /**
   *
   * @param loginOrganizationConsumer {@link BiConsumer}<String,String> (login, organization uuid)
   */
  public void selectForUserIndexing(DbSession dbSession, Collection<String> logins, BiConsumer<String, String> loginOrganizationConsumer) {
    executeLargeInputsWithoutOutput(logins, list -> mapper(dbSession).selectForIndexing(list)
      .forEach(row -> loginOrganizationConsumer.accept(row.get("login"), row.get("organizationUuid"))));
  }

  /**
   *
   * @param loginOrganizationConsumer {@link BiConsumer}<String,String> (login, organization uuid)
   */
  public void selectAllForUserIndexing(DbSession dbSession, BiConsumer<String, String> loginOrganizationConsumer) {
    mapper(dbSession).selectAllForIndexing()
      .forEach(row -> loginOrganizationConsumer.accept(row.get("login"), row.get("organizationUuid")));
  }
}
