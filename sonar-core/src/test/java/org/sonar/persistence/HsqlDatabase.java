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
import org.sonar.jpa.dialect.HsqlDb;
import org.sonar.jpa.session.CustomHibernateConnectionProvider;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * In-memory database used for Hibernate unit tests only. For information MyBatis tests use Derby.
 *
 * @since 2.12
 */
public class HsqlDatabase implements Database {

  private BasicDataSource datasource;

  public HsqlDatabase start() {
    startDatabase();
    return this;
  }

  void startDatabase() {
    try {
      Properties properties = new Properties();
      properties.put("driverClassName", "org.hsqldb.jdbcDriver");
      properties.put("username", "sa");
      properties.put("password", "");
      properties.put("url", "jdbc:hsqldb:mem:sonar");

      properties.put("maxActive", "3");
      properties.put("maxIdle", "3");
      datasource = (BasicDataSource) BasicDataSourceFactory.createDataSource(properties);

    } catch (Exception e) {
      throw new IllegalStateException("Fail to start HSQL", e);
    }
  }

  public HsqlDatabase stop() {
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
    return new HsqlDb();
  }

  public String getSchema() {
    return null;
  }

  public Properties getHibernateProperties() {
    Properties properties = new Properties();
    properties.put("hibernate.hbm2ddl.auto", "create-drop");
    properties.put(Environment.DIALECT, getDialect().getHibernateDialectClass().getName());
    properties.put(Environment.CONNECTION_PROVIDER, CustomHibernateConnectionProvider.class.getName());
    return properties;
  }
}
