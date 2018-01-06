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
package org.sonar.db.user;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.session.RowBounds;
import org.sonar.api.utils.System2;
import org.sonar.db.Dao;
import org.sonar.db.DaoDatabaseUtils;
import org.sonar.db.DbSession;
import org.sonar.db.WildcardPosition;

import static org.sonar.db.DatabaseUtils.executeLargeInputs;

public class GroupDao implements Dao {

  private final System2 system;

  public GroupDao(System2 system) {
    this.system = system;
  }

  /**
   * @param dbSession
   * @param organizationUuid non-null UUID of organization (no support of "default" organization)
   * @param name non-null group name
   * @return the group with the given organization key and name
   */
  public Optional<GroupDto> selectByName(DbSession dbSession, String organizationUuid, String name) {
    return Optional.ofNullable(mapper(dbSession).selectByName(organizationUuid, name));
  }

  public List<GroupDto> selectByNames(DbSession dbSession, String organizationUuid, Collection<String> names) {
    return executeLargeInputs(names, pageOfNames -> mapper(dbSession).selectByNames(organizationUuid, pageOfNames));
  }

  @CheckForNull
  public GroupDto selectById(DbSession dbSession, int groupId) {
    return mapper(dbSession).selectById(groupId);
  }

  public List<GroupDto> selectByIds(DbSession dbSession, List<Integer> ids) {
    return executeLargeInputs(ids, mapper(dbSession)::selectByIds);
  }

  public void deleteById(DbSession dbSession, int groupId) {
    mapper(dbSession).deleteById(groupId);
  }

  public void deleteByOrganization(DbSession dbSession, String organizationUuid) {
    mapper(dbSession).deleteByOrganization(organizationUuid);
  }

  public int countByQuery(DbSession session, String organizationUuid, @Nullable String query) {
    return mapper(session).countByQuery(organizationUuid, groupSearchToSql(query));
  }

  public List<GroupDto> selectByQuery(DbSession session, String organizationUuid, @Nullable String query, int offset, int limit) {
    return mapper(session).selectByQuery(organizationUuid, groupSearchToSql(query), new RowBounds(offset, limit));
  }

  public GroupDto insert(DbSession session, GroupDto item) {
    Date createdAt = new Date(system.now());
    item.setCreatedAt(createdAt)
      .setUpdatedAt(createdAt);
    mapper(session).insert(item);
    return item;
  }

  public GroupDto update(DbSession session, GroupDto item) {
    item.setUpdatedAt(new Date(system.now()));
    mapper(session).update(item);
    return item;
  }

  public List<GroupDto> selectByUserLogin(DbSession session, String login) {
    return mapper(session).selectByUserLogin(login);
  }

  @CheckForNull
  private static String groupSearchToSql(@Nullable String query) {
    if (query == null) {
      return null;
    }

    String upperCasedNameQuery = StringUtils.upperCase(query, Locale.ENGLISH);
    return DaoDatabaseUtils.buildLikeValue(upperCasedNameQuery, WildcardPosition.BEFORE_AND_AFTER);
  }

  public List<GroupDto> selectByOrganizationUuid(DbSession dbSession, String organizationUuid) {
    return mapper(dbSession).selectByOrganizationUuid(organizationUuid);
  }

  private static GroupMapper mapper(DbSession session) {
    return session.getMapper(GroupMapper.class);
  }
}
