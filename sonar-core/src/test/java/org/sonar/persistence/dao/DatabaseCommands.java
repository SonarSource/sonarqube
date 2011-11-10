/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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
package org.sonar.persistence.dao;

import org.dbunit.dataset.datatype.DefaultDataTypeFactory;
import org.dbunit.dataset.datatype.IDataTypeFactory;
import org.dbunit.ext.mssql.MsSqlDataTypeFactory;
import org.dbunit.ext.mysql.MySqlDataTypeFactory;
import org.dbunit.ext.oracle.Oracle10DataTypeFactory;
import org.dbunit.ext.postgresql.PostgresqlDataTypeFactory;
import org.sonar.jpa.dialect.*;

abstract class DatabaseCommands {

  private IDataTypeFactory dbUnitFactory;

  private DatabaseCommands(IDataTypeFactory dbUnitFactory) {
    this.dbUnitFactory = dbUnitFactory;
  }

  abstract String truncate(String table);

  abstract String resetPrimaryKey(String table);

  final IDataTypeFactory dbUnitFactory() {
    return dbUnitFactory;
  }


  
  static final DatabaseCommands DERBY = new DatabaseCommands(new DefaultDataTypeFactory()) {
    @Override
    String truncate(String table) {
      return "TRUNCATE TABLE " + table;
    }

    @Override
    String resetPrimaryKey(String table) {
      return "ALTER TABLE " + table + " ALTER COLUMN ID RESTART WITH 1";
    }
  };
  
  static final DatabaseCommands MSSQL = new DatabaseCommands(new MsSqlDataTypeFactory()) {
    @Override
    String truncate(String table) {
      return "TRUNCATE TABLE " + table;
    }

    @Override
    String resetPrimaryKey(String table) {
      return "DBCC CHECKIDENT('" + table + "', RESEED, 1)";
    }
  };

  static final DatabaseCommands MYSQL = new DatabaseCommands(new MySqlDataTypeFactory()) {
    @Override
    String truncate(String table) {
      return "TRUNCATE TABLE " + table;
    }

    @Override
    String resetPrimaryKey(String table) {
      return "ALTER TABLE " + table + " AUTO_INCREMENT = 1";
    }
  };

  static final DatabaseCommands ORACLE = new DatabaseCommands(new Oracle10DataTypeFactory()) {
    @Override
    String truncate(String table) {
      return "TRUNCATE TABLE " + table;
    }

    @Override
    String resetPrimaryKey(String table) {
      return "ALTER SEQUENCE " + table + "_SEQ INCREMENT BY - MINVALUE 1;";
    }
  };

  static final DatabaseCommands POSTGRESQL = new DatabaseCommands(new PostgresqlDataTypeFactory()) {
    @Override
    String truncate(String table) {
      return "TRUNCATE TABLE " + table;
    }

    @Override
    String resetPrimaryKey(String table) {
      return "ALTER SEQUENCE " + table + "_id_seq RESTART WITH 1";
    }
  };


  static DatabaseCommands forDialect(Dialect dialect) {
    if (Derby.ID.equals(dialect.getId())) {
      return DERBY;
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
}
