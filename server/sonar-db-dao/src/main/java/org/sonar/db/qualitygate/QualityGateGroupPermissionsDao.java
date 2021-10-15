/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
package org.sonar.db.qualitygate;

import java.util.Collection;
import java.util.List;
import org.sonar.api.utils.System2;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.Pagination;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.SearchGroupMembershipDto;
import org.sonar.db.user.SearchGroupsQuery;

import static org.sonar.core.util.stream.MoreCollectors.toList;
import static org.sonar.db.DatabaseUtils.executeLargeInputs;

public class QualityGateGroupPermissionsDao implements Dao {
  private final System2 system2;

  public QualityGateGroupPermissionsDao(System2 system2) {
    this.system2 = system2;
  }

  public boolean exists(DbSession dbSession, QualityGateDto qualityGate, GroupDto group) {
    return this.exists(dbSession, qualityGate.getUuid(), group.getUuid());
  }

  public boolean exists(DbSession dbSession, String qualityGateUuid, String groupUuid) {
    return mapper(dbSession).selectByQualityGateAndGroup(qualityGateUuid, groupUuid) != null;
  }

  public boolean exists(DbSession dbSession, QualityGateDto qualityGate, Collection<GroupDto> groups) {
    return !executeLargeInputs(groups.stream().map(GroupDto::getUuid).collect(toList()),
      partition -> mapper(dbSession).selectByQualityGateAndGroups(qualityGate.getUuid(), partition))
      .isEmpty();
  }

  public void insert(DbSession dbSession, QualityGateGroupPermissionsDto dto) {
    mapper(dbSession).insert(dto, system2.now());
  }

  private static QualityGateGroupPermissionsMapper mapper(DbSession dbSession) {
    return dbSession.getMapper(QualityGateGroupPermissionsMapper.class);
  }

  public List<SearchGroupMembershipDto> selectByQuery(DbSession dbSession, SearchGroupsQuery query, Pagination pagination) {
    return mapper(dbSession).selectByQuery(query, pagination);
  }

  public int countByQuery(DbSession dbSession, SearchGroupsQuery query) {
    return mapper(dbSession).countByQuery(query);
  }
}
