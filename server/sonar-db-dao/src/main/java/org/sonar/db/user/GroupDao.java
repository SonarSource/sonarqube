/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import org.sonar.db.DaoUtils;
import org.sonar.db.DbSession;
import org.sonar.db.WildcardPosition;
import org.sonar.db.audit.AuditPersister;
import org.sonar.db.audit.model.UserGroupNewValue;

import static org.sonar.db.DatabaseUtils.executeLargeInputs;

public class GroupDao implements Dao {

  private final System2 system;
  private final AuditPersister auditPersister;

  public GroupDao(System2 system, AuditPersister auditPersister) {
    this.system = system;
    this.auditPersister = auditPersister;
  }

  /**
   * @param dbSession
   * @param name      non-null group name
   * @return the group with the given name
   */
  public Optional<GroupDto> selectByName(DbSession dbSession, String name) {
    return Optional.ofNullable(mapper(dbSession).selectByName(name));
  }

  public List<GroupDto> selectByNames(DbSession dbSession, Collection<String> names) {
    return executeLargeInputs(names, pageOfNames -> mapper(dbSession).selectByNames(pageOfNames));
  }

  @CheckForNull
  public GroupDto selectByUuid(DbSession dbSession, String groupUuid) {
    return mapper(dbSession).selectByUuid(groupUuid);
  }

  public List<GroupDto> selectByUuids(DbSession dbSession, List<String> uuids) {
    return executeLargeInputs(uuids, mapper(dbSession)::selectByUuids);
  }

  public void deleteByUuid(DbSession dbSession, String groupUuid, String groupName) {
    int deletedRows = mapper(dbSession).deleteByUuid(groupUuid);

    if (deletedRows > 0) {
      auditPersister.deleteUserGroup(dbSession, new UserGroupNewValue(groupUuid, groupName));
    }
  }

  public int countByQuery(DbSession session, @Nullable String query) {
    return mapper(session).countByQuery(groupSearchToSql(query));
  }

  public List<GroupDto> selectByQuery(DbSession session, @Nullable String query, int offset, int limit) {
    return mapper(session).selectByQuery(groupSearchToSql(query), new RowBounds(offset, limit));
  }

  public GroupDto insert(DbSession session, GroupDto item) {
    Date createdAt = new Date(system.now());
    item.setCreatedAt(createdAt)
      .setUpdatedAt(createdAt);
    mapper(session).insert(item);
    auditPersister.addUserGroup(session, new UserGroupNewValue(item.getUuid(), item.getName()));
    return item;
  }

  public GroupDto update(DbSession session, GroupDto item) {
    item.setUpdatedAt(new Date(system.now()));
    mapper(session).update(item);
    auditPersister.updateUserGroup(session, new UserGroupNewValue(item));
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
    return DaoUtils.buildLikeValue(upperCasedNameQuery, WildcardPosition.BEFORE_AND_AFTER);
  }

  private static GroupMapper mapper(DbSession session) {
    return session.getMapper(GroupMapper.class);
  }
}
