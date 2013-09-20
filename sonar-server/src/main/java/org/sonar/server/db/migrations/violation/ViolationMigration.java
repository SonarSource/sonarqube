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

import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.utils.MessageException;
import org.sonar.core.persistence.Database;
import org.sonar.server.db.DatabaseMigration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

/**
 * Used in the Active Record Migration 401
 */
public class ViolationMigration implements DatabaseMigration {

  public static final int GROUP_SIZE = 1000;

  private Logger logger = LoggerFactory.getLogger(ViolationMigration.class);

  @Override
  public void execute(Database db) {
    try {
      truncateIssueTables(db);
      migrate(db);

    } catch (SQLException e) {
      logger.error("Fail to convert violations to issues", e);
      SqlUtil.log(logger, e);
      throw MessageException.of("Fail to convert violations to issues");

    } catch (Exception e) {
      logger.error("Fail to convert violations to issues", e);
      throw MessageException.of("Fail to convert violations to issues");
    }
  }

  private void truncateIssueTables(Database db) throws SQLException {
    Connection connection = null;
    try {
      QueryRunner runner = new QueryRunner();
      connection = db.getDataSource().getConnection();
      connection.setAutoCommit(true);

      // lower-case table names for SQLServer....
      runner.update(connection, "TRUNCATE TABLE issues");
      runner.update(connection, "TRUNCATE TABLE issue_changes");

    } finally {
      DbUtils.closeQuietly(connection);
    }

  }

  public void migrate(Database db) throws Exception {
    Referentials referentials = new Referentials(db);
    ViolationConverters converters = new ViolationConverters(db, referentials);
    Connection readConnection = db.getDataSource().getConnection();
    try {
      new QueryRunner().query(readConnection, "select id from rule_failures", new ViolationIdHandler(converters));
    } finally {
      DbUtils.closeQuietly(readConnection);
    }
    converters.waitForFinished();
  }

  private static class ViolationIdHandler implements ResultSetHandler {
    private final ViolationConverters converters;

    private ViolationIdHandler(ViolationConverters converters) {
      this.converters = converters;
    }

    @Override
    public Object handle(ResultSet rs) throws SQLException {
      // int is enough, it allows to upgrade up to 2 billions violations !
      int total = 0;
      int cursor = 0;

      Object[] violationIds = new Object[GROUP_SIZE];
      while (rs.next()) {
        long violationId = rs.getLong(1);
        violationIds[cursor++] = violationId;
        if (cursor == GROUP_SIZE) {
          converters.convert(violationIds);
          violationIds = new Object[GROUP_SIZE];
          cursor = 0;
        }
        total++;
      }
      if (cursor > 0) {
        for (int i=0 ; i<violationIds.length ; i++) {
          if (violationIds[i]==null) {
            violationIds[i]=-1;
          }
        }
        converters.convert(violationIds);
      }
      LoggerFactory.getLogger(getClass()).info(String.format("%d violations migrated to issues", total));
      return null;
    }
  }

}
