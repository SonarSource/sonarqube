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

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbcp.BasicDataSourceFactory;
import org.sonar.jpa.dialect.Derby;
import org.sonar.jpa.dialect.Dialect;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Derby in-memory database, used for unit tests only.
 *
 * @since 2.12
 */
public class InMemoryDatabase implements Database {

  private BasicDataSource datasource;

  public InMemoryDatabase start() {
    startDatabase();
    executeDdl();
    return this;
  }

  void startDatabase() {
    try {
      Properties properties = new Properties();
      properties.put("driverClassName", "org.apache.derby.jdbc.EmbeddedDriver");
      properties.put("username", "sonar");
      properties.put("password", "sonar");
      properties.put("url", "jdbc:derby:memory:sonar;create=true;user=sonar;password=sonar");
      properties.put("maxActive", "1");
      properties.put("maxIdle", "1");
      datasource = (BasicDataSource) BasicDataSourceFactory.createDataSource(properties);

    } catch (Exception e) {
      throw new IllegalStateException("Fail to start Derby", e);
    }
  }

  void executeDdl() {
    Connection connection = null;
    try {
      connection = datasource.getConnection();
      DdlUtils.execute(connection, "derby");

    } catch (SQLException e) {
      throw new IllegalStateException("Fail to execute DDL", e);
      
    } finally {
      if (connection != null) {
        try {
          connection.close();
        } catch (SQLException e) {
          // crazy close method !
        }
      }
    }
  }

  public InMemoryDatabase stop() {
    try {
      if (datasource != null) {
        datasource.close();
      }
      DriverManager.getConnection("jdbc:derby:memory:sonar;drop=true");

    } catch (SQLException e) {
      // silently ignore stop failure
    }
    return this;
  }

  public DataSource getDataSource() {
    return datasource;
  }

  public Dialect getDialect() {
    return new Derby();
  }
}
