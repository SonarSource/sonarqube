/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.permission.index;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;

/**
 * No streaming because of union of joins -> no need to use ResultSetIterator
 */
public class AuthorizationDao {

  public static final class Dto {
    private final String projectUuid;
    private final long updatedAt;
    private final List<String> users = Lists.newArrayList();
    private final List<String> groups = Lists.newArrayList();

    public Dto(String projectUuid, long updatedAt) {
      this.projectUuid = projectUuid;
      this.updatedAt = updatedAt;
    }

    public String getProjectUuid() {
      return projectUuid;
    }

    public long getUpdatedAt() {
      return updatedAt;
    }

    public List<String> getUsers() {
      return users;
    }

    public Dto addUser(String s) {
      users.add(s);
      return this;
    }

    public Dto addGroup(String s) {
      groups.add(s);
      return this;
    }

    public List<String> getGroups() {
      return groups;
    }
  }

  private static final String SQL_TEMPLATE =
    "SELECT " +
      "  project_authorization.project as project, " +
      "  project_authorization.login as login, " +
      "  project_authorization.permission_group as permission_group, " +
      "  project_authorization.updated_at as updated_at " +
      "FROM ( " +

      // project is returned when no authorization
      "      SELECT " +
      "      projects.uuid AS project, " +
      "      projects.authorization_updated_at AS updated_at, " +
      "      NULL AS login, " +
      "      NULL  AS permission_group " +
      "      FROM projects " +
      "      WHERE " +
      "        projects.qualifier = 'TRK' " +
      "        AND projects.copy_component_uuid is NULL " +
      "        {dateCondition} " +
      "      UNION " +

      // users

      "      SELECT " +
      "      projects.uuid AS project, " +
      "      projects.authorization_updated_at AS updated_at, " +
      "      users.login  AS login, " +
      "      NULL  AS permission_group " +
      "      FROM projects " +
      "      INNER JOIN user_roles ON user_roles.resource_id = projects.id AND user_roles.role = 'user' " +
      "      INNER JOIN users ON users.id = user_roles.user_id " +
      "      WHERE " +
      "        projects.qualifier = 'TRK' " +
      "        AND projects.copy_component_uuid is NULL " +
      "        {dateCondition} " +
      "      UNION " +

      // groups without Anyone

      "      SELECT " +
      "      projects.uuid AS project, " +
      "      projects.authorization_updated_at AS updated_at, " +
      "      NULL  AS login, " +
      "      groups.name  AS permission_group " +
      "      FROM projects " +
      "      INNER JOIN group_roles ON group_roles.resource_id = projects.id AND group_roles.role = 'user' " +
      "      INNER JOIN groups ON groups.id = group_roles.group_id " +
      "      WHERE " +
      "        projects.qualifier = 'TRK' " +
      "        AND projects.copy_component_uuid is NULL " +
      "        {dateCondition} " +
      "        AND group_id IS NOT NULL " +
      "      UNION " +

      // Anyone groups

      "      SELECT " +
      "      projects.uuid AS project, " +
      "      projects.authorization_updated_at AS updated_at, " +
      "      NULL         AS login, " +
      "      'Anyone'     AS permission_group " +
      "      FROM projects " +
      "      INNER JOIN group_roles ON group_roles.resource_id = projects.id AND group_roles.role='user' " +
      "      WHERE " +
      "        projects.qualifier = 'TRK' " +
      "        AND projects.copy_component_uuid is NULL " +
      "        {dateCondition} " +
      "        AND group_roles.group_id IS NULL " +
      "    ) project_authorization";

  Collection<Dto> selectAfterDate(DbClient dbClient, DbSession session, long afterDate) {
    try {
      Map<String, Dto> dtosByProjectUuid = Maps.newHashMap();
      PreparedStatement stmt = null;
      ResultSet rs = null;
      try {
        stmt = createStatement(dbClient, session, afterDate);
        rs = stmt.executeQuery();
        while (rs.next()) {
          processRow(rs, dtosByProjectUuid);
        }
        return dtosByProjectUuid.values();
      } finally {
        DbUtils.closeQuietly(rs);
        DbUtils.closeQuietly(stmt);
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Fail to select authorizations after date: " + afterDate, e);
    }
  }

  private static PreparedStatement createStatement(DbClient dbClient, DbSession session, long afterDate) throws SQLException {
    String sql;
    if (afterDate > 0L) {
      sql = StringUtils.replace(SQL_TEMPLATE, "{dateCondition}", " AND projects.authorization_updated_at>? ");
    } else {
      sql = StringUtils.replace(SQL_TEMPLATE, "{dateCondition}", "");
    }
    PreparedStatement stmt = dbClient.getMyBatis().newScrollingSelectStatement(session, sql);
    if (afterDate > 0L) {
      for (int i = 1; i <= 4; i++) {
        stmt.setLong(i, afterDate);
      }
    }
    return stmt;
  }

  private static void processRow(ResultSet rs, Map<String, Dto> dtosByProjectUuid) throws SQLException {
    String projectUuid = rs.getString(1);
    String userLogin = rs.getString(2);
    String group = rs.getString(3);

    Dto dto = dtosByProjectUuid.get(projectUuid);
    if (dto == null) {
      long updatedAt = rs.getLong(4);
      dto = new Dto(projectUuid, updatedAt);
      dtosByProjectUuid.put(projectUuid, dto);
    }
    if (StringUtils.isNotBlank(userLogin)) {
      dto.addUser(userLogin);
    }
    if (StringUtils.isNotBlank(group)) {
      dto.addGroup(group);
    }
  }
}
