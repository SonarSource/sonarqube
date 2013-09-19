/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.server.db.migrations.violation;

import com.google.common.collect.Maps;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.sonar.core.persistence.Database;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

public class Referentials {
  private final Database database;
  private final Map<Long, String> loginsByUserId;
  private final Map<Long, String> plansById;

  public Referentials(Database database) throws SQLException {
    this.database = database;
    loginsByUserId = selectLongString("select id,login from users");
    plansById = selectLongString("select id,kee from action_plans");
  }

  @CheckForNull
  String actionPlan(@Nullable Long id) {
    return id != null ? plansById.get(id) : null;
  }

  @CheckForNull
  String userLogin(@Nullable Long id) {
    return id != null ? loginsByUserId.get(id) : null;
  }

  private Map<Long, String> selectLongString(String sql) throws SQLException {
    Connection connection = database.getDataSource().getConnection();
    try {
      return new QueryRunner().query(connection, sql, new ResultSetHandler<Map<Long, String>>() {
        @Override
        public Map<Long, String> handle(ResultSet rs) throws SQLException {
          Map<Long, String> map = Maps.newHashMap();
          while (rs.next()) {
            map.put(rs.getLong(1), rs.getString(2));
          }
          return map;
        }
      });
    } finally {
      DbUtils.closeQuietly(connection);
    }
  }
}
