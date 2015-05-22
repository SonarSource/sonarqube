/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.user.db;

import java.util.Date;
import java.util.List;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.session.RowBounds;
import org.sonar.api.utils.System2;
import org.sonar.core.persistence.DaoComponent;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.user.GroupDto;
import org.sonar.core.user.GroupMapper;

/**
 * @since 3.2
 */
public class GroupDao implements DaoComponent {

  private static final String SQL_WILDCARD = "%";
  private System2 system;

  public GroupDao(System2 system) {
    this.system = system;
  }

  public GroupDto selectByKey(DbSession session, String key) {
    return mapper(session).selectByKey(key);
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
    return item;
  }

  public List<GroupDto> findByUserLogin(DbSession session, String login){
    return mapper(session).selectByUserLogin(login);
  }

  private GroupMapper mapper(DbSession session) {
    return session.getMapper(GroupMapper.class);
  }

  private String groupSearchToSql(@Nullable String query) {
    String sql = SQL_WILDCARD;
    if (query != null) {
      sql = StringUtils.replace(StringUtils.upperCase(query), SQL_WILDCARD, "/%");
      sql = StringUtils.replace(sql, "_", "/_");
      sql = SQL_WILDCARD + sql + SQL_WILDCARD;
    }
    return sql;
  }
}
