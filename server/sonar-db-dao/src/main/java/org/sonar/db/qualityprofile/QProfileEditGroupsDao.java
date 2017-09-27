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
package org.sonar.db.qualityprofile;

import java.util.Collection;
import java.util.List;
import org.sonar.api.utils.System2;
import org.sonar.db.Dao;
import org.sonar.db.DatabaseUtils;
import org.sonar.db.DbSession;
import org.sonar.db.Pagination;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.user.GroupDto;

import static org.sonar.core.util.stream.MoreCollectors.toList;

public class QProfileEditGroupsDao implements Dao {

  private final System2 system2;

  public QProfileEditGroupsDao(System2 system2) {
    this.system2 = system2;
  }

  public boolean exists(DbSession dbSession, QProfileDto profile, GroupDto group) {
    return mapper(dbSession).selectByQProfileAndGroup(profile.getKee(), group.getId()) != null;
  }

  public int countByQuery(DbSession dbSession, SearchGroupsQuery query) {
    return mapper(dbSession).countByQuery(query);
  }

  public List<GroupMembershipDto> selectByQuery(DbSession dbSession, SearchGroupsQuery query, Pagination pagination) {
    return mapper(dbSession).selectByQuery(query, pagination);
  }

  public List<String> selectQProfileUuidsByOrganizationAndGroups(DbSession dbSession, OrganizationDto organization, Collection<GroupDto> groups) {
    return DatabaseUtils.executeLargeInputs(groups.stream().map(GroupDto::getId).collect(toList()), g ->
      mapper(dbSession).selectQProfileUuidsByOrganizationAndGroups(organization.getUuid(), g));
  }

  public void insert(DbSession dbSession, QProfileEditGroupsDto dto) {
    mapper(dbSession).insert(dto, system2.now());
  }

  public void deleteByQProfileAndGroup(DbSession dbSession, QProfileDto profile, GroupDto group) {
    mapper(dbSession).delete(profile.getKee(), group.getId());
  }

  private static QProfileEditGroupsMapper mapper(DbSession dbSession) {
    return dbSession.getMapper(QProfileEditGroupsMapper.class);
  }
}
