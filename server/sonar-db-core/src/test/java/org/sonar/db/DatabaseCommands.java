/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.db;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import javax.sql.DataSource;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.lang.StringUtils;
import org.dbunit.dataset.datatype.DefaultDataTypeFactory;
import org.dbunit.dataset.datatype.IDataTypeFactory;
import org.dbunit.dataset.datatype.ToleratedDeltaMap;
import org.dbunit.ext.h2.H2DataTypeFactory;
import org.dbunit.ext.mssql.MsSqlDataTypeFactory;
import org.dbunit.ext.mysql.MySqlDataTypeFactory;
import org.dbunit.ext.oracle.Oracle10DataTypeFactory;
import org.dbunit.ext.postgresql.PostgresqlDataTypeFactory;
import org.sonar.db.dialect.Dialect;
import org.sonar.db.dialect.MsSql;
import org.sonar.db.dialect.MySql;
import org.sonar.db.dialect.Oracle;
import org.sonar.db.dialect.PostgreSql;
import org.sonar.db.version.SqTables;

public abstract class DatabaseCommands {
  private final IDataTypeFactory dbUnitFactory;

  private DatabaseCommands(DefaultDataTypeFactory dbUnitFactory) {
    this.dbUnitFactory = dbUnitFactory;

    // Hack for MsSQL failure in IssueMapperTest.
    // All the Double fields should be listed here.
    dbUnitFactory.addToleratedDelta(new ToleratedDeltaMap.ToleratedDelta("issues", "effort_to_fix", 0.0001));
  }

  public final IDataTypeFactory getDbUnitFactory() {
    return dbUnitFactory;
  }

  abstract List<String> resetSequenceSql(String table, int minSequenceValue);

  String truncateSql(String table) {
    return "TRUNCATE TABLE " + table;
  }

  boolean useLoginAsSchema() {
    return false;
  }

  public static DatabaseCommands forDialect(Dialect dialect) {
    DatabaseCommands command = ImmutableMap.of(
      org.sonar.db.dialect.H2.ID, H2,
      MsSql.ID, MSSQL,
      MySql.ID, MYSQL,
      Oracle.ID, ORACLE,
      PostgreSql.ID, POSTGRESQL).get(dialect.getId());

    return Preconditions.checkNotNull(command, "Unknown database: " + dialect);
  }

  private static final DatabaseCommands H2 = new DatabaseCommands(new H2DataTypeFactory()) {
    @Override
    List<String> resetSequenceSql(String table, int minSequenceValue) {
      return Arrays.asList("ALTER TABLE " + table + " ALTER COLUMN ID RESTART WITH " + minSequenceValue);
    }
  };

  private static final DatabaseCommands POSTGRESQL = new DatabaseCommands(new PostgresqlDataTypeFactory()) {
    @Override
    List<String> resetSequenceSql(String table, int minSequenceValue) {
      return Arrays.asList("ALTER SEQUENCE " + table + "_id_seq RESTART WITH " + minSequenceValue);
    }
  };

  private static final DatabaseCommands ORACLE = new DatabaseCommands(new Oracle10DataTypeFactory()) {
    @Override
    List<String> resetSequenceSql(String table, int minSequenceValue) {
      String sequence = StringUtils.upperCase(table) + "_SEQ";
      return Arrays.asList(
        "DROP SEQUENCE " + sequence,
        "CREATE SEQUENCE " + sequence + " INCREMENT BY 1 MINVALUE 1 START WITH " + minSequenceValue);
    }

    @Override
    String truncateSql(String table) {
      return "DELETE FROM " + table;
    }

    @Override
    boolean useLoginAsSchema() {
      return true;
    }
  };

  private static final DatabaseCommands MSSQL = new DatabaseCommands(new MsSqlDataTypeFactory()) {
    @Override
    public void resetPrimaryKeys(DataSource dataSource) {
    }

    @Override
    List<String> resetSequenceSql(String table, int minSequenceValue) {
      return null;
    }

    @Override
    protected boolean shouldTruncate(Connection connection, String table) {
      // truncate all tables on mssql, else unexpected errors in some tests
      return true;
    }
  };

  private static final DatabaseCommands MYSQL = new DatabaseCommands(new MySqlDataTypeFactory()) {
    @Override
    public void resetPrimaryKeys(DataSource dataSource) {
    }

    @Override
    List<String> resetSequenceSql(String table, int minSequenceValue) {
      return null;
    }
  };

  public void truncateDatabase(DataSource dataSource) throws SQLException {
    Connection connection = dataSource.getConnection();
    Statement statement = null;
    try {
      connection.setAutoCommit(false);
      statement = connection.createStatement();
      for (String table : SqTables.TABLES) {
        try {
          if (shouldTruncate(connection, table)) {
            statement.executeUpdate(truncateSql(table));
            connection.commit();
          }
        } catch (Exception e) {
          connection.rollback();
          throw new IllegalStateException("Fail to truncate table " + table, e);
        }
      }
    } finally {
      DbUtils.closeQuietly(connection);
      DbUtils.closeQuietly(statement);
    }
  }

  protected boolean shouldTruncate(Connection connection, String table) throws SQLException {
    Statement stmt = connection.createStatement();
    ResultSet rs = null;
    try {
      rs = stmt.executeQuery("select count(1) from " + table);
      if (rs.next()) {
        return rs.getInt(1) > 0;
      }

    } catch (SQLException ignored) {
      // probably because table does not exist. That's the case with H2 tests.
    } finally {
      DbUtils.closeQuietly(rs);
      DbUtils.closeQuietly(stmt);
    }
    return false;
  }

  public void resetPrimaryKeys(DataSource dataSource) throws SQLException {
    Connection connection = null;
    Statement statement = null;
    ResultSet resultSet = null;
    try {
      connection = dataSource.getConnection();
      connection.setAutoCommit(false);

      statement = connection.createStatement();
      for (String table : SqTables.TABLES) {
        try {
          resultSet = statement.executeQuery("SELECT CASE WHEN MAX(ID) IS NULL THEN 1 ELSE MAX(ID)+1 END FROM " + table);
          resultSet.next();
          int maxId = resultSet.getInt(1);
          resultSet.close();

          for (String resetCommand : resetSequenceSql(table, maxId)) {
            statement.executeUpdate(resetCommand);
          }
          connection.commit();
        } catch (Exception e) {
          connection.rollback(); // this table has no primary key
        }
      }
    } finally {
      DbUtils.closeQuietly(connection, statement, resultSet);
    }
  }
}
