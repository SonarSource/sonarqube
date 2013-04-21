/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.core.persistence;

import org.apache.commons.dbcp.BasicDataSource;
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
  private final String name;
  private BasicDataSource datasource;

  /**
   * IMPORTANT: change DB name in order to not conflict with {@link DefaultDatabaseTest}
   */
  public H2Database(String name) {
    this.name = name;
  }

  public H2Database start() {
    startDatabase();
    createSchema();
    return this;
  }

  private void startDatabase() {
    try {
      datasource = new BasicDataSource();
      datasource.setDriverClassName("org.h2.Driver");
      datasource.setUsername("sonar");
      datasource.setPassword("sonar");
      datasource.setUrl("jdbc:h2:mem:" + name);
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
      DatabaseUtils.closeQuietly(connection);
    }
  }

  public H2Database stop() {
    try {
      datasource.close();
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

  @Override
  public String toString() {
    return "H2 Database[" + name + "]";
  }
}
