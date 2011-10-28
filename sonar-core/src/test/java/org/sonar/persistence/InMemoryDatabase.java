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
import org.hibernate.cfg.Environment;
import org.sonar.jpa.dialect.Derby;
import org.sonar.jpa.dialect.Dialect;
import org.sonar.jpa.session.CustomHibernateConnectionProvider;

import javax.sql.DataSource;
import java.sql.*;
import java.util.Properties;

/**
 * Derby in-memory database, used for unit tests only.
 *
 * @since 2.12
 */
public class InMemoryDatabase implements Database {

  private static BasicDataSource datasource;

  public InMemoryDatabase start() {
    if (datasource == null) {
      startDatabase();
      createSchema();
    }
    truncateTables();
    return this;
  }

  /**
   * IMPORTANT: DB name changed from "sonar" to "sonar2" in order to not conflict with {@link org.sonar.test.persistence.DatabaseTestCase}
   */
  void startDatabase() {
    try {
      Properties properties = new Properties();
      properties.put("driverClassName", "org.apache.derby.jdbc.EmbeddedDriver");
      properties.put("username", "sonar");
      properties.put("password", "sonar");
      properties.put("url", "jdbc:derby:memory:sonar2;create=true;user=sonar;password=sonar");

      // limit to 2 because of Hibernate and MyBatis
      properties.put("maxActive", "2");
      properties.put("maxIdle", "2");
      datasource = (BasicDataSource) BasicDataSourceFactory.createDataSource(properties);

    } catch (Exception e) {
      throw new IllegalStateException("Fail to start Derby", e);
    }
  }

  void createSchema() {
    Connection connection = null;
    try {
      connection = datasource.getConnection();
      DdlUtils.createSchema(connection, "derby");

    } catch (SQLException e) {
      throw new IllegalStateException("Fail to create schema", e);

    } finally {
      closeQuietly(connection);
    }
  }

  private void truncateTables() {
    Connection connection = null;
    try {
      connection = datasource.getConnection();

      DatabaseMetaData meta = connection.getMetaData();

      ResultSet res = meta.getTables(null, null, null, new String[] { "TABLE" });
      while (res.next()) {
        String tableName = res.getString("TABLE_NAME");
        connection.prepareStatement("TRUNCATE TABLE " + tableName).execute();
      }
      res.close();

      // See https://issues.apache.org/jira/browse/DERBY-5403
      res = meta.getColumns(null, null, null, "ID");
      while (res.next()) {
        String tableName = res.getString("TABLE_NAME");
        connection.prepareStatement("ALTER TABLE " + tableName + " ALTER COLUMN ID RESTART WITH 1").execute();
      }
      res.close();

    } catch (SQLException e) {
      throw new IllegalStateException("Fail to truncate tables", e);

    } finally {
      closeQuietly(connection); // Important, otherwise tests can stuck
    }
  }

  void stopDatabase() {
    try {
      if (datasource != null) {
        datasource.close();
      }
      DriverManager.getConnection("jdbc:derby:;shutdown=true");

    } catch (SQLException e) {
      // See http://db.apache.org/derby/docs/dev/getstart/rwwdactivity3.html
      // XJ015 indicates successful shutdown of Derby
      // 08006 successful shutdown of a single database
      if (!"XJ015".equals(e.getSQLState())) {
        throw new IllegalStateException("Fail to stop Derby", e);
      }
    }
  }

  public InMemoryDatabase stop() {
    return this;
  }

  public DataSource getDataSource() {
    return datasource;
  }

  public Dialect getDialect() {
    return new Derby();
  }

  public Properties getHibernateProperties() {
    Properties properties = new Properties();
    properties.put("hibernate.hbm2ddl.auto", "validate");
    properties.put(Environment.DIALECT, getDialect().getHibernateDialectClass().getName());
    properties.put(Environment.CONNECTION_PROVIDER, CustomHibernateConnectionProvider.class.getName());
    return properties;
  }

  private static void closeQuietly(Connection connection) {
    if (connection != null) {
      try {
        connection.close();
      } catch (SQLException e) {
        // ignore
      }
    }
  }

}
