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
package org.sonar.db.user;

import java.util.List;
import java.util.Optional;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;

public class ExternalGroupDao implements Dao {

  public void insert(DbSession dbSession, ExternalGroupDto externalGroupDto) {
    mapper(dbSession).insert(externalGroupDto);
  }

  public Optional<ExternalGroupDto> selectByGroupUuid(DbSession dbSession, String groupUuid) {
    return mapper(dbSession).selectByGroupUuid(groupUuid);
  }

  public List<ExternalGroupDto> selectByIdentityProvider(DbSession dbSession, String identityProvider) {
    return mapper(dbSession).selectByIdentityProvider(identityProvider);
  }

  public void deleteByGroupUuid(DbSession dbSession, String groupUuid) {
    mapper(dbSession).deleteByGroupUuid(groupUuid);
  }

  public Optional<ExternalGroupDto> selectByExternalIdAndIdentityProvider(DbSession dbSession, String externalId, String identityProvider) {
    return mapper(dbSession).selectByExternalIdAndIdentityProvider(externalId, identityProvider);
  }

  private static ExternalGroupMapper mapper(DbSession session) {
    return session.getMapper(ExternalGroupMapper.class);
  }

  public String getManagedGroupSqlFilter(boolean filterByManaged) {
    if (filterByManaged) {
      return  "(exists (select group_uuid from external_groups eg where eg.group_uuid = uuid) "
        + "or exists (select group_uuid from github_orgs_groups gog where gog.group_uuid = uuid))";
    }
    return "(not exists (select group_uuid from external_groups eg where eg.group_uuid = uuid) "
      + "and not exists (select group_uuid from github_orgs_groups gog where gog.group_uuid = uuid))";
  }

  public void deleteByExternalIdentityProvider(DbSession dbSession, String externalIdentityProvider) {
    mapper(dbSession).deleteByExternalIdentityProvider(externalIdentityProvider);
  }
}
