/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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

import static org.sonar.db.DatabaseUtils.executeLargeInputsWithoutOutput;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;

public class OrganizationMemberDao implements Dao {
  private static OrganizationMemberMapper mapper(DbSession dbSession) {
    return dbSession.getMapper(OrganizationMemberMapper.class);
  }

  public Optional<OrganizationMemberDto> select(DbSession dbSession, String organizationUuid, String userUuid) {
    return Optional.ofNullable(mapper(dbSession).select(organizationUuid, userUuid));
  }

  public List<String> selectUserUuidsByOrganizationUuid(DbSession dbSession, String organizationUuid) {
    return mapper(dbSession).selectUserUuids(organizationUuid);
  }

  public void insert(DbSession dbSession, OrganizationMemberDto organizationMemberDto) {
    mapper(dbSession).insert(organizationMemberDto);
  }

  public void delete(DbSession dbSession, String organizationMemberUuid, String userUuid) {
    mapper(dbSession).delete(organizationMemberUuid, userUuid);
  }

  public void deleteByOrganizationUuid(DbSession dbSession, String organizationMemberUuid) {
    mapper(dbSession).deleteByOrganization(organizationMemberUuid);
  }

  public void deleteByUserUuid(DbSession dbSession, String userUuid) {
    mapper(dbSession).deleteByUserUuid(userUuid);
  }

  public Set<String> selectOrganizationUuidsByUser(DbSession dbSession, String userUuid) {
    return mapper(dbSession).selectOrganizationUuidsByUser(userUuid);
  }

  /**
   * @param userUuidOrganizationConsumer {@link BiConsumer}<String,String> (uuid, organization uuid)
   */
  public void selectForUserIndexing(DbSession dbSession, Collection<String> uuids, BiConsumer<String, String> userUuidOrganizationConsumer) {
    executeLargeInputsWithoutOutput(uuids, list -> mapper(dbSession).selectForIndexing(list)
      .forEach(row -> userUuidOrganizationConsumer.accept(row.get("uuid"), row.get("organizationUuid"))));
  }

  /**
   *
   * @param userUuidOrganizationConsumer {@link BiConsumer}<String,String> (uuid, organization uuid)
   */
  public void selectAllForUserIndexing(DbSession dbSession, BiConsumer<String, String> userUuidOrganizationConsumer) {
    mapper(dbSession).selectAllForIndexing()
      .forEach(row -> userUuidOrganizationConsumer.accept(row.get("uuid"), row.get("organizationUuid")));
  }
}
