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
package org.sonar.server.db;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.dbutils.DbUtils;
import org.apache.ibatis.session.SqlSession;
import org.slf4j.LoggerFactory;
import org.sonar.api.ServerComponent;
import org.sonar.core.persistence.Database;
import org.sonar.core.persistence.DdlUtils;
import org.sonar.core.persistence.MyBatis;

import java.sql.Connection;

/**
 * Restore schema by executing DDL scripts. Only H2 database is supported.
 * Other databases are created by Ruby on Rails migrations.
 *
 * @since 2.12
 */
public class DatabaseMigrator implements ServerComponent {

  private final MyBatis myBatis;
  private final Database database;
  private final DatabaseMigration[] migrations;

  public DatabaseMigrator(MyBatis myBatis, Database database, DatabaseMigration[] migrations) {
    this.myBatis = myBatis;
    this.database = database;
    this.migrations = migrations;
  }

  /**
   * @return true if the database has been created, false if this database is not supported
   */
  public boolean createDatabase() {
    if (!DdlUtils.supportsDialect(database.getDialect().getId())) {
      return false;
    }

    LoggerFactory.getLogger(getClass()).info("Create database");
    SqlSession session = null;
    Connection connection = null;
    try {
      session = myBatis.openSession();
      connection = session.getConnection();
      createSchema(connection, database.getDialect().getId());
      return true;
    } finally {
      MyBatis.closeQuietly(session);

      // The connection is probably already closed by session.close()
      // but it's not documented in mybatis javadoc.
      DbUtils.closeQuietly(connection);
    }
  }

  public void executeMigration(String className) {
    DatabaseMigration migration = getMigration(className);
    try {
      migration.execute();

    } catch (Exception e) {
      // duplication between log and exception because webapp does not correctly log initial stacktrace
      String msg = "Fail to execute database migration: " + className;
      LoggerFactory.getLogger(getClass()).error(msg, e);
      throw new IllegalStateException(msg, e);
    }
  }

  private DatabaseMigration getMigration(String className) {
    for (DatabaseMigration migration : migrations) {
      if (migration.getClass().getName().equals(className)) {
        return migration;
      }
    }
    throw new IllegalArgumentException("Database migration not found: " + className);
  }

  @VisibleForTesting
  protected void createSchema(Connection connection, String dialectId) {
    DdlUtils.createSchema(connection, dialectId);
  }
}
