/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.db.scim;

import java.util.List;
import java.util.Optional;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.Pagineable;

public class ScimGroupDao implements Dao {
  private final UuidFactory uuidFactory;

  public ScimGroupDao(UuidFactory uuidFactory) {
    this.uuidFactory = uuidFactory;
  }

  public List<ScimGroupDto> findAll(DbSession dbSession) {
    return mapper(dbSession).findAll();
  }

  public List<ScimGroupDto> findScimGroups(DbSession dbSession, ScimGroupQuery query, Pagineable pagination) {
    return mapper(dbSession).findScimGroups(query, pagination);
  }

  public Optional<ScimGroupDto> findByScimUuid(DbSession dbSession, String scimGroupUuid) {
    return Optional.ofNullable(mapper(dbSession).findByScimUuid(scimGroupUuid));
  }

  public Optional<ScimGroupDto> findByGroupUuid(DbSession dbSession, String groupUuid) {
    return Optional.ofNullable(mapper(dbSession).findByGroupUuid(groupUuid));
  }

  public int countScimGroups(DbSession dbSession, ScimGroupQuery query) {
    return mapper(dbSession).countScimGroups(query);
  }

  public ScimGroupDto enableScimForGroup(DbSession dbSession, String groupUuid) {
    ScimGroupDto scimGroupDto = new ScimGroupDto(uuidFactory.create(), groupUuid);
    mapper(dbSession).insert(scimGroupDto);
    return scimGroupDto;
  }

  public void deleteByGroupUuid(DbSession dbSession, String groupUuid) {
    mapper(dbSession).deleteByGroupUuid(groupUuid);
  }

  public void deleteByScimUuid(DbSession dbSession, String scimUuid) {
    mapper(dbSession).deleteByScimUuid(scimUuid);
  }

  private static ScimGroupMapper mapper(DbSession session) {
    return session.getMapper(ScimGroupMapper.class);
  }

  public String getManagedGroupSqlFilter(boolean filterByManaged) {
    return String.format("%s exists (select group_uuid from scim_groups sg where sg.group_uuid = uuid)", filterByManaged ? "" : "not");
  }

  public void deleteAll(DbSession dbSession) {
    mapper(dbSession).deleteAll();
  }
}
