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

package org.sonar.server.db.migrations.debt;

import org.apache.commons.dbutils.DbUtils;
import org.sonar.core.persistence.Database;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Data loaded from database before migrating debt of issues.
 */
class IssueReferentials {

  static final int GROUP_SIZE = 1000;

  private final Queue<long[]> groupsOfIssuesIds;
  private long totalIssues = 0L;

  IssueReferentials(Database database) throws SQLException {
    groupsOfIssuesIds = initGroupOfIssueIds(database);
  }

  long totalIssues() {
    return totalIssues;
  }

  Long[] pollGroupOfIssueIds() {
    long[] longs = groupsOfIssuesIds.poll();
    if (longs == null) {
      return new Long[0];
    }
    Long[] objects = new Long[longs.length];
    for (int i = 0; i < longs.length; i++) {
      objects[i] = Long.valueOf(longs[i]);
    }
    return objects;
  }

  private Queue<long[]> initGroupOfIssueIds(Database database) throws SQLException {
    Connection connection = database.getDataSource().getConnection();
    Statement stmt = null;
    ResultSet rs = null;
    try {
      connection.setAutoCommit(false);
      stmt = connection.createStatement();
      stmt.setFetchSize(10000);
      rs = stmt.executeQuery("SELECT id FROM issues WHERE technical_debt IS NOT NULL");
      ConcurrentLinkedQueue<long[]> queue = new ConcurrentLinkedQueue<long[]>();

      totalIssues = 0;
      long[] block = new long[GROUP_SIZE];
      int cursor = 0;
      while (rs.next()) {
        block[cursor] = rs.getLong(1);
        cursor++;
        totalIssues++;
        if (cursor == GROUP_SIZE) {
          queue.add(block);
          block = new long[GROUP_SIZE];
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
