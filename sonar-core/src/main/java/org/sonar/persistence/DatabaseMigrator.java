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
package org.sonar.persistence;

import org.apache.ibatis.session.SqlSession;
import org.slf4j.LoggerFactory;
import org.sonar.api.ServerComponent;

import java.sql.Connection;

/**
 * Restore schema by executing DDL scripts. Only Derby database is supported. Other databases are created by Ruby on Rails migrations.
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
    if (DdlUtils.supportsDialect(database.getDialect().getId())) {
      LoggerFactory.getLogger(getClass()).info("Create database");
      SqlSession session = myBatis.openSession();
      Connection connection = session.getConnection();
      try {
        DdlUtils.createSchema(connection, database.getDialect().getId());
      } finally {
        session.close();
      }
      return true;
    }
    return false;
  }
}
