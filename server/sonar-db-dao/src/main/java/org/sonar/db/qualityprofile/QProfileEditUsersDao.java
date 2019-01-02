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
package org.sonar.db.qualityprofile;

import java.util.List;
import org.sonar.api.utils.System2;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.Pagination;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.user.UserDto;

import static org.sonar.core.util.stream.MoreCollectors.toList;
import static org.sonar.db.DatabaseUtils.executeLargeUpdates;

public class QProfileEditUsersDao implements Dao {

  private final System2 system2;

  public QProfileEditUsersDao(System2 system2) {
    this.system2 = system2;
  }

  public boolean exists(DbSession dbSession, QProfileDto profile, UserDto user) {
    return mapper(dbSession).selectByQProfileAndUser(profile.getKee(), user.getId()) != null;
  }

  public int countByQuery(DbSession dbSession, SearchUsersQuery query) {
    return mapper(dbSession).countByQuery(query);
  }

  public List<UserMembershipDto> selectByQuery(DbSession dbSession, SearchUsersQuery query, Pagination pagination) {
    return mapper(dbSession).selectByQuery(query, pagination);
  }

  public List<String> selectQProfileUuidsByOrganizationAndUser(DbSession dbSession, OrganizationDto organization, UserDto userDto) {
    return mapper(dbSession).selectQProfileUuidsByOrganizationAndUser(organization.getUuid(), userDto.getId());
  }

  public void insert(DbSession dbSession, QProfileEditUsersDto dto) {
    mapper(dbSession).insert(dto, system2.now());
  }

  public void deleteByQProfileAndUser(DbSession dbSession, QProfileDto profile, UserDto user) {
    mapper(dbSession).delete(profile.getKee(), user.getId());
  }

  public void deleteByQProfiles(DbSession dbSession, List<QProfileDto> qProfiles) {
    executeLargeUpdates(qProfiles.stream().map(QProfileDto::getKee).collect(toList()), p -> mapper(dbSession).deleteByQProfiles(p));
  }

  public void deleteByUser(DbSession dbSession, UserDto user) {
    mapper(dbSession).deleteByUser(user.getId());
  }

  public void deleteByOrganizationAndUser(DbSession dbSession, OrganizationDto organization, UserDto user) {
    mapper(dbSession).deleteByOrganizationAndUser(organization.getUuid(), user.getId());
  }

  private static QProfileEditUsersMapper mapper(DbSession dbSession) {
    return dbSession.getMapper(QProfileEditUsersMapper.class);
  }
}
