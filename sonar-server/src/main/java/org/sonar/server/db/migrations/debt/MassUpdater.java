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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.utils.MessageException;
import org.sonar.core.persistence.Database;
import org.sonar.core.persistence.dialect.MySql;
import org.sonar.server.db.migrations.util.SqlUtil;

import java.sql.*;

public class MassUpdater {

  private static final Logger logger = LoggerFactory.getLogger(MassUpdater.class);

  static final int GROUP_SIZE = 10000;

  private static final String FAILURE_MESSAGE = "Fail to migrate data";


  private final Database db;

  public MassUpdater(Database db) {
    this.db = db;
  }

  interface InputLoader<S> {
    String selectSql();

    S load(ResultSet rs) throws SQLException;
  }

  interface InputConverter<S> {
    String updateSql();

    void convert(S input, PreparedStatement statement) throws SQLException;
  }

  public <S> void execute(InputLoader<S> inputLoader, InputConverter<S> converter) {
    int count = 0;
    try {
      Connection readConnection = db.getDataSource().getConnection();
      Statement stmt = null;
      ResultSet rs = null;

      Connection writeConnection = db.getDataSource().getConnection();
      PreparedStatement writeStatement = null;
      try {
        writeConnection.setAutoCommit(false);
        writeStatement = writeConnection.prepareStatement(converter.updateSql());

        readConnection.setAutoCommit(false);

        stmt = readConnection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        stmt.setFetchSize(GROUP_SIZE);
        if (db.getDialect().getId().equals(MySql.ID)) {
          stmt.setFetchSize(Integer.MIN_VALUE);
        } else {
          stmt.setFetchSize(GROUP_SIZE);
        }
        rs = stmt.executeQuery(inputLoader.selectSql());

        int cursor = 0;
        while (rs.next()) {
          converter.convert(inputLoader.load(rs), writeStatement);
          writeStatement.addBatch();

          cursor++;
          count++;
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
      } finally {
        if (writeStatement != null) {
          writeStatement.close();
        }
        DbUtils.closeQuietly(writeConnection);
        DbUtils.closeQuietly(readConnection, stmt, rs);

        logger.info("{} rows have been updated", count);
      }
    } catch (SQLException e) {
      logger.error(FAILURE_MESSAGE, e);
      SqlUtil.log(logger, e);
      throw MessageException.of(FAILURE_MESSAGE);

    } catch (Exception e) {
      logger.error(FAILURE_MESSAGE, e);
      throw MessageException.of(FAILURE_MESSAGE);
    }
  }

}
