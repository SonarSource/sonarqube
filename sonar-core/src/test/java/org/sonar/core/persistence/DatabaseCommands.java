/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.core.persistence;

import org.apache.commons.lang.StringUtils;
import org.dbunit.dataset.datatype.IDataTypeFactory;
import org.dbunit.ext.h2.H2DataTypeFactory;
import org.dbunit.ext.mssql.InsertIdentityOperation;
import org.dbunit.ext.mssql.MsSqlDataTypeFactory;
import org.dbunit.ext.mysql.MySqlDataTypeFactory;
import org.dbunit.ext.oracle.Oracle10DataTypeFactory;
import org.dbunit.ext.postgresql.PostgresqlDataTypeFactory;
import org.dbunit.operation.DatabaseOperation;
import org.sonar.core.persistence.dialect.Dialect;
import org.sonar.core.persistence.dialect.MsSql;
import org.sonar.core.persistence.dialect.MySql;
import org.sonar.core.persistence.dialect.Oracle;
import org.sonar.core.persistence.dialect.PostgreSql;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;

public abstract class DatabaseCommands {
  private final IDataTypeFactory dbUnitFactory;

  private DatabaseCommands(IDataTypeFactory dbUnitFactory) {
    this.dbUnitFactory = dbUnitFactory;
  }

  public abstract String truncate(String table);

  public abstract List<String> resetPrimaryKey(String table);

  public Object getTrue() {
    return Boolean.TRUE;
  }

  public Object getFalse() {
    return Boolean.FALSE;
  }

  public IDataTypeFactory getDbUnitFactory() {
    return dbUnitFactory;
  }

  public DatabaseOperation getDbunitDatabaseOperation() {
    return new InsertIdentityOperation(DatabaseOperation.INSERT);
  }

  static final DatabaseCommands H2 = new DatabaseCommands(new H2DataTypeFactory()) {
    @Override
    public String truncate(String table) {
      return "TRUNCATE TABLE " + table;
    }

    @Override
    public List<String> resetPrimaryKey(String table) {
      return Arrays.asList("ALTER TABLE " + table + " ALTER COLUMN ID RESTART WITH 1");
    }
  };

  static final DatabaseCommands MSSQL = new DatabaseCommands(new MsSqlDataTypeFactory()) {
    @Override
    public String truncate(String table) {
      return "TRUNCATE TABLE " + table;
    }

    @Override
    public List<String> resetPrimaryKey(String table) {
      return Arrays.asList("DBCC CHECKIDENT('" + table + "', RESEED, 1)");
    }

    @Override
    public DatabaseOperation getDbunitDatabaseOperation() {
      return new InsertIdentityOperation(DatabaseOperation.CLEAN_INSERT);
    }
  };

  static final DatabaseCommands MYSQL = new DatabaseCommands(new MySqlDataTypeFactory()) {
    @Override
    public String truncate(String table) {
      return "TRUNCATE TABLE " + table;
    }

    @Override
    public List<String> resetPrimaryKey(String table) {
      return Arrays.asList("ALTER TABLE " + table + " AUTO_INCREMENT = 1");
    }
  };

  static final DatabaseCommands ORACLE = new DatabaseCommands(new Oracle10DataTypeFactory()) {
    @Override
    public String truncate(String table) {
      return "TRUNCATE TABLE " + table;
    }

    @Override
    public List<String> resetPrimaryKey(String table) {
      String sequence = StringUtils.upperCase(table) + "_SEQ";
      return Arrays.asList(
          "DROP SEQUENCE " + sequence,
          "CREATE SEQUENCE " + sequence + " INCREMENT BY 1 MINVALUE 1 START WITH 1"
          );
    }

    @Override
    public Object getTrue() {
      return 1;
    }

    @Override
    public Object getFalse() {
      return 0;
    }
  };

  static final DatabaseCommands POSTGRESQL = new DatabaseCommands(new PostgresqlDataTypeFactory()) {
    @Override
    public String truncate(String table) {
      return "TRUNCATE TABLE " + table;
    }

    @Override
    public List<String> resetPrimaryKey(String table) {
      return Arrays.asList("ALTER SEQUENCE " + table + "_id_seq RESTART WITH 1");
    }
  };

  public static DatabaseCommands forDialect(Dialect dialect) {
    if (org.sonar.core.persistence.dialect.H2.ID.equals(dialect.getId())) {
      return H2;
    }
    if (MsSql.ID.equals(dialect.getId())) {
      return MSSQL;
    }
    if (MySql.ID.equals(dialect.getId())) {
      return MYSQL;
    }
    if (Oracle.ID.equals(dialect.getId())) {
      return ORACLE;
    }
    if (PostgreSql.ID.equals(dialect.getId())) {
      return POSTGRESQL;
    }
    throw new IllegalArgumentException("Unknown database: " + dialect);
  }

  public void truncateDatabase(Connection connection) throws SQLException {
    Statement statement = connection.createStatement();
    for (String table : DatabaseUtils.TABLE_NAMES) {
      // 1. truncate
      String truncateCommand = truncate(table);
      statement.executeUpdate(truncateCommand);
      connection.commit();

      // 2. reset primary keys
      try {
        for (String resetCommand : resetPrimaryKey(table)) {
          statement.executeUpdate(resetCommand);
        }
        connection.commit();
      } catch (Exception e) {
        // this table has no primary key
        connection.rollback();
      }
    }
    statement.close();
    connection.commit();
    connection.close();
  }
}
