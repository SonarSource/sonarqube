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

import org.apache.ibatis.session.SqlSession;
import org.slf4j.LoggerFactory;
import org.sonar.api.ServerComponent;

import java.sql.Connection;

/**
 * Restore schema by executing DDL scripts. Only H2 database is supported.
 * Other databases are created by Ruby on Rails migrations.
 *
 * @since 2.12
 */
public class DatabaseMigrator implements ServerComponent {

  private MyBatis myBatis;
  private Database database;

  public DatabaseMigrator(MyBatis myBatis, Database database) {
    this.myBatis = myBatis;
    this.database = database;
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
      DdlUtils.createSchema(connection, database.getDialect().getId());
    } finally {
      MyBatis.closeQuietly(session);

      // The connection is probably already closed by session.close()
      // but it's not documented in mybatis javadoc.
      DatabaseUtils.closeQuietly(connection);
    }
    return true;
  }
}
