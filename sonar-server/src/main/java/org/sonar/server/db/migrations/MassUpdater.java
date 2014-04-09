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

package org.sonar.server.db.migrations;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.dbutils.DbUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.utils.MessageException;
import org.sonar.core.persistence.Database;
import org.sonar.core.persistence.dialect.MySql;

import java.sql.*;

/**
 * Update a table by iterating a sub-set of rows. For each row a SQL UPDATE request
 * is executed.
 */
public class MassUpdater {

  private static final Logger LOGGER = LoggerFactory.getLogger(MassUpdater.class);
  private static final String FAILURE_MESSAGE = "Fail to migrate data";
  private static final int GROUP_SIZE = 1000;
  private final Database db;

  public MassUpdater(Database db) {
    this.db = db;
  }

  public static interface InputLoader<S> {
    String selectSql();

    S load(ResultSet rs) throws SQLException;
  }

  public static interface InputConverter<S> {
    String updateSql();

    /**
     * Return false if you do not want to update this statement
     */
    boolean convert(S input, PreparedStatement updateStatement) throws SQLException;
  }

  public <S> void execute(InputLoader<S> inputLoader, InputConverter<S> converter) {
    long count = 0;
    Connection readConnection = null;
    Statement stmt = null;
    ResultSet rs = null;
    Connection writeConnection = null;
    PreparedStatement writeStatement = null;
    try {
      writeConnection = db.getDataSource().getConnection();
      writeConnection.setAutoCommit(false);
      writeStatement = writeConnection.prepareStatement(converter.updateSql());

      readConnection = db.getDataSource().getConnection();
      readConnection.setAutoCommit(false);

      stmt = readConnection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
      stmt.setFetchSize(GROUP_SIZE);
      if (db.getDialect().getId().equals(MySql.ID)) {
        stmt.setFetchSize(Integer.MIN_VALUE);
      } else {
        stmt.setFetchSize(GROUP_SIZE);
      }
      rs = stmt.executeQuery(convertSelectSql(inputLoader.selectSql(), db));

      int cursor = 0;
      while (rs.next()) {
        if (converter.convert(inputLoader.load(rs), writeStatement)) {
          writeStatement.addBatch();
          cursor++;
          count++;
        }

        if (cursor == GROUP_SIZE) {
          writeStatement.executeBatch();
          writeConnection.commit();
          cursor = 0;
        }
      }
      if (cursor > 0) {
        writeStatement.executeBatch();
        writeConnection.commit();
      }

    } catch (SQLException e) {
      LOGGER.error(FAILURE_MESSAGE, e);
      SqlUtil.log(LOGGER, e);
      throw MessageException.of(FAILURE_MESSAGE);

    } catch (Exception e) {
      LOGGER.error(FAILURE_MESSAGE, e);
      throw MessageException.of(FAILURE_MESSAGE);

    } finally {
      DbUtils.closeQuietly(writeStatement);
      DbUtils.closeQuietly(writeConnection);
      DbUtils.closeQuietly(readConnection, stmt, rs);

      LOGGER.info("{} rows have been updated", count);
    }
  }

  @VisibleForTesting
  static String convertSelectSql(String selectSql, Database db){
    // Replace ${_true}
    selectSql = selectSql.replace("${_true}", db.getDialect().getTrueSqlValue());

    // Replace ${_false}
    selectSql = selectSql.replace("${_false}", db.getDialect().getFalseSqlValue());

    return selectSql;
  }

}
