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
package org.sonar.core.persistence;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang.StringUtils;
import org.dbunit.dataset.datatype.IDataTypeFactory;
import org.dbunit.ext.h2.H2DataTypeFactory;
import org.dbunit.ext.mssql.MsSqlDataTypeFactory;
import org.dbunit.ext.mysql.MySqlDataTypeFactory;
import org.dbunit.ext.oracle.Oracle10DataTypeFactory;
import org.dbunit.ext.postgresql.PostgresqlDataTypeFactory;
import org.sonar.core.persistence.dialect.Dialect;
import org.sonar.core.persistence.dialect.MsSql;
import org.sonar.core.persistence.dialect.MySql;
import org.sonar.core.persistence.dialect.Oracle;
import org.sonar.core.persistence.dialect.PostgreSql;

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;

public abstract class DatabaseCommands {
  private final IDataTypeFactory dbUnitFactory;

  private DatabaseCommands(IDataTypeFactory dbUnitFactory) {
    this.dbUnitFactory = dbUnitFactory;
  }

  public final IDataTypeFactory getDbUnitFactory() {
    return dbUnitFactory;
  }

  abstract List<String> resetPrimaryKey(String table, int minSequenceValue);

  public static DatabaseCommands forDialect(Dialect dialect) {
    DatabaseCommands command = ImmutableMap.of(
        org.sonar.core.persistence.dialect.H2.ID, H2,
        MsSql.ID, MSSQL,
        MySql.ID, MYSQL,
        Oracle.ID, ORACLE,
        PostgreSql.ID, POSTGRESQL).get(dialect.getId());

    return Preconditions.checkNotNull(command, "Unknown database: " + dialect);
  }

  private static final DatabaseCommands H2 = new DatabaseCommands(new H2DataTypeFactory()) {
    @Override
    List<String> resetPrimaryKey(String table, int minSequenceValue) {
      return Arrays.asList("ALTER TABLE " + table + " ALTER COLUMN ID RESTART WITH " + minSequenceValue);
    }
  };

  private static final DatabaseCommands POSTGRESQL = new DatabaseCommands(new PostgresqlDataTypeFactory()) {
    @Override
    List<String> resetPrimaryKey(String table, int minSequenceValue) {
      return Arrays.asList("ALTER SEQUENCE " + table + "_id_seq RESTART WITH " + minSequenceValue);
    }
  };

  private static final DatabaseCommands ORACLE = new DatabaseCommands(new Oracle10DataTypeFactory()) {
    @Override
    List<String> resetPrimaryKey(String table, int minSequenceValue) {
      String sequence = StringUtils.upperCase(table) + "_SEQ";
      return Arrays.asList(
          "DROP SEQUENCE " + sequence,
          "CREATE SEQUENCE " + sequence + " INCREMENT BY 1 MINVALUE 1 START WITH " + minSequenceValue);
    }
  };

  private static final DatabaseCommands MSSQL = new DatabaseCommands(new MsSqlDataTypeFactory()) {
    @Override
    public void resetPrimaryKeys(DataSource dataSource) {
    }

    @Override
    List<String> resetPrimaryKey(String table, int minSequenceValue) {
      return null;
    }
  };

  private static final DatabaseCommands MYSQL = new DatabaseCommands(new MySqlDataTypeFactory()) {
    @Override
    public void resetPrimaryKeys(DataSource dataSource) {
    }

    @Override
    List<String> resetPrimaryKey(String table, int minSequenceValue) {
      return null;
    }
  };

  public void truncateDatabase(DataSource dataSource) throws SQLException {
    Connection connection = dataSource.getConnection();
    connection.setAutoCommit(false);

    Statement statement = connection.createStatement();
    for (String table : DatabaseUtils.TABLE_NAMES) {
      statement.executeUpdate("TRUNCATE TABLE " + table);
      connection.commit();
    }

    statement.close();
    connection.close();
  }

  public void resetPrimaryKeys(DataSource dataSource) throws SQLException {
    Connection connection = dataSource.getConnection();
    connection.setAutoCommit(false);

    Statement statement = connection.createStatement();
    for (String table : DatabaseUtils.TABLE_NAMES) {
      try {
        ResultSet result = statement.executeQuery("SELECT CASE WHEN MAX(ID) IS NULL THEN 1 ELSE MAX(ID)+1 END FROM " + table);
        result.next();
        int maxId = result.getInt(1);
        result.close();

        for (String resetCommand : resetPrimaryKey(table, maxId)) {
          statement.executeUpdate(resetCommand);
        }
        connection.commit();
      } catch (Exception e) {
        connection.rollback(); // this table has no primary key
      }
    }

    statement.close();
    connection.close();
  }
}
