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
package org.sonar.server.db.migrations.v36;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.sonar.core.persistence.Database;

import com.google.common.collect.Maps;

/**
 * Data loaded from database before migrating violations. It is
 * shared amongst converter parallel tasks.
 */
class Referentials {

  static final int VIOLATION_GROUP_SIZE = 1000;

  private final Map<Long, String> loginsByUserId;
  private final Map<Long, String> plansById;
  private final Queue<long[]> groupsOfViolationIds;
  private long totalViolations = 0L;

  Referentials(Database database) throws SQLException {
    loginsByUserId = selectLongString(database, "select id,login from users");
    plansById = selectLongString(database, "select id,kee from action_plans");
    groupsOfViolationIds = initGroupOfViolationIds(database);
  }

  @CheckForNull
  String actionPlan(@Nullable Long id) {
    return id != null ? plansById.get(id) : null;
  }

  @CheckForNull
  String userLogin(@Nullable Long id) {
    return id != null ? loginsByUserId.get(id) : null;
  }

  long totalViolations() {
    return totalViolations;
  }

  Long[] pollGroupOfViolationIds() {
    long[] longs = groupsOfViolationIds.poll();
    if (longs == null) {
      return new Long[0];
    }
    Long[] objects = new Long[longs.length];
    for (int i = 0; i < longs.length; i++) {
      objects[i] = Long.valueOf(longs[i]);
    }
    return objects;
  }

  private Map<Long, String> selectLongString(Database database, String sql) throws SQLException {
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

  private Queue<long[]> initGroupOfViolationIds(Database database) throws SQLException {
    Connection connection = database.getDataSource().getConnection();
    Statement stmt = null;
    ResultSet rs = null;
    try {
      connection.setAutoCommit(false);
      stmt = connection.createStatement();
      stmt.setFetchSize(10000);
      rs = stmt.executeQuery("select id from rule_failures");
      Queue<long[]> queue = new ConcurrentLinkedQueue<long[]>();

      totalViolations = 0;
      long[] block = new long[VIOLATION_GROUP_SIZE];
      int cursor = 0;
      while (rs.next()) {
        block[cursor] = rs.getLong(1);
        cursor++;
        totalViolations++;
        if (cursor == VIOLATION_GROUP_SIZE) {
          queue.add(block);
          block = new long[VIOLATION_GROUP_SIZE];
          cursor = 0;
        }
      }
      if (cursor > 0) {
        queue.add(block);
      }
      return queue;
    } finally {
      DbUtils.closeQuietly(connection, stmt, rs);
    }
  }

}
