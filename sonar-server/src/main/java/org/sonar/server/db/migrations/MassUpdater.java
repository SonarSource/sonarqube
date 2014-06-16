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

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Update a table by iterating a sub-set of rows. For each row a SQL UPDATE request
 * is executed.
 */
public class MassUpdater {

  private static final Logger LOGGER = LoggerFactory.getLogger(MassUpdater.class);
  private static final int DEFAULT_GROUP_SIZE = 1000;
  private final Database db;
  private final int groupSize;

  public MassUpdater(Database db) {
    this(db, DEFAULT_GROUP_SIZE);
  }

  public MassUpdater(Database db, int groupSize) {
    this.db = db;
    this.groupSize = groupSize;
  }

  public static interface InputLoader<S> {
    String selectSql();

    @CheckForNull
    S load(ResultSet rs) throws SQLException;
  }

  public static interface InputConverter<S> {
    String updateSql();

    /**
     * Return false if you do not want to update this statement
     */
    boolean convert(S input, PreparedStatement updateStatement) throws SQLException;
  }

  public static interface PeriodicUpdater {

    /**
     * Return false if you do not want to update this statement
     */
    boolean update(Connection writeConnection) throws SQLException;
  }

  public <S> void execute(InputLoader<S> inputLoader, InputConverter<S> converter) {
    execute(inputLoader, converter, null);
  }

  public <S> void execute(InputLoader<S> inputLoader, InputConverter<S> converter, @Nullable PeriodicUpdater periodicUpdater) {
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
      if (db.getDialect().getId().equals(MySql.ID)) {
        stmt.setFetchSize(Integer.MIN_VALUE);
      } else {
        stmt.setFetchSize(groupSize);
      }
      rs = stmt.executeQuery(convertSelectSql(inputLoader.selectSql(), db));

      int cursor = 0;
      while (rs.next()) {
        S row = inputLoader.load(rs);
        if (row == null) {
          continue;
        }
        if (converter.convert(row, writeStatement)) {
          writeStatement.addBatch();
          writeStatement.clearParameters();
          cursor++;
          count++;
        }

        if (cursor == groupSize) {
          writeStatement.executeBatch();
          if (periodicUpdater != null) {
            periodicUpdater.update(writeConnection);
          }
          writeConnection.commit();
          cursor = 0;
        }
      }
      if (cursor > 0) {
        writeStatement.executeBatch();
        if (periodicUpdater != null) {
          periodicUpdater.update(writeConnection);
        }
        writeConnection.commit();
      }

    } catch (SQLException e) {
      SqlUtil.log(LOGGER, e);
      throw processError(e);
    } catch (Exception e) {
      throw processError(e);
    } finally {
      DbUtils.closeQuietly(writeStatement);
      DbUtils.closeQuietly(writeConnection);
      DbUtils.closeQuietly(readConnection, stmt, rs);

      LOGGER.info("{} rows have been updated", count);
    }
  }

  private static MessageException processError(Exception e) {
    String message = String.format("Fail to migrate data, error is : %s", e.getMessage());
    LOGGER.error(message, e);
    throw MessageException.of(message);
  }

  @VisibleForTesting
  static String convertSelectSql(String selectSql, Database db) {
    String newSelectSql = selectSql;
    newSelectSql = newSelectSql.replace("${_true}", db.getDialect().getTrueSqlValue());
    newSelectSql = newSelectSql.replace("${_false}", db.getDialect().getFalseSqlValue());
    return newSelectSql;
  }

}
