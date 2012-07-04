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

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbcp.BasicDataSourceFactory;
import org.hibernate.cfg.Environment;
import org.sonar.core.persistence.dialect.Dialect;
import org.sonar.core.persistence.dialect.H2;
import org.sonar.jpa.session.CustomHibernateConnectionProvider;

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * H2 in-memory database, used for unit tests only.
 *
 * @since 3.2
 */
public class H2Database implements Database {
  private static BasicDataSource datasource;

  public H2Database start() {
    if (datasource == null) {
      startDatabase();
      createSchema();
    }
    return this;
  }

  /**
   * IMPORTANT: DB name changed from "sonar" to "sonar2" in order to not conflict with {@link DefaultDatabaseTest}
   */
  private void startDatabase() {
    try {
      Properties properties = new Properties();
      properties.put("driverClassName", "org.h2.Driver");
      properties.put("username", "sonar");
      properties.put("password", "sonar");
      properties.put("url", "jdbc:h2:mem:sonar2");

      // limit to 2 because of Hibernate and MyBatis
      properties.put("maxActive", "2");
      properties.put("maxIdle", "2");
      datasource = (BasicDataSource) BasicDataSourceFactory.createDataSource(properties);

    } catch (Exception e) {
      throw new IllegalStateException("Fail to start H2", e);
    }
  }

  private void createSchema() {
    Connection connection = null;
    try {
      connection = datasource.getConnection();
      DdlUtils.createSchema(connection, "h2");
    } catch (SQLException e) {
      throw new IllegalStateException("Fail to create schema", e);
    } finally {
      if (connection != null) {
        try {
          connection.close();
        } catch (SQLException e) {
          // ignore
        }
      }
    }
  }

  public H2Database stop() {
    try {
      if (datasource != null) {
        datasource.close();
        datasource = null;
      }
    } catch (SQLException e) {
      // Ignore error
    }
    return this;
  }

  public DataSource getDataSource() {
    return datasource;
  }

  public Dialect getDialect() {
    return new H2();
  }

  public String getSchema() {
    return null;
  }

  public Properties getHibernateProperties() {
    Properties properties = new Properties();
    properties.put("hibernate.hbm2ddl.auto", "validate");
    properties.put(Environment.CONNECTION_PROVIDER, CustomHibernateConnectionProvider.class.getName());
    return properties;
  }
}
