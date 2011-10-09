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
package org.sonar.server.database;

import org.apache.commons.dbcp.BasicDataSourceFactory;
import org.apache.commons.lang.StringUtils;
import org.hibernate.cfg.Environment;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.Logs;
import org.sonar.api.utils.SonarException;
import org.sonar.jpa.entity.SchemaMigration;
import org.sonar.jpa.session.AbstractDatabaseConnector;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

public class JndiDatabaseConnector extends AbstractDatabaseConnector {

  private DataSource datasource = null;

  public JndiDatabaseConnector(Settings configuration) {
    super(configuration, false);
  }

  @Override
  public boolean isOperational() {
    if (isStarted() && getDatabaseVersion() != SchemaMigration.LAST_VERSION) {
      // connector was started and connection OK but schema version was not OK
      // call start again to check if this is now ok (schema created by rails)
      start();
    }
    return super.isOperational();
  }

  @Override
  public void start() {
    if (!isStarted()) {
      createDatasource();
    }
    if (!super.isOperational()) {
      super.start();
    }
  }

  @Override
  public void stop() {
    datasource = null;
    super.stop();
  }


  private void createDatasource() {
    try {
      Logs.INFO.info("Creating JDBC datasource");
      Properties properties = new Properties();
      List<String> jdbcKeys = configuration.getKeysStartingWith("sonar.jdbc.");
      for (String jdbcKey : jdbcKeys) {
        properties.setProperty(StringUtils.removeStart(jdbcKey, "sonar.jdbc."), configuration.getString(jdbcKey));
      }

      // This property is required by the Ruby Oracle enhanced adapter.
      // It directly uses the Connection implementation provided by the Oracle driver
      properties.setProperty("accessToUnderlyingConnectionAllowed", "true");

      datasource = BasicDataSourceFactory.createDataSource(properties);
      CustomHibernateConnectionProvider.datasource = datasource;
    } catch (Exception e) {
      throw new SonarException("Fail to connect to database", e);
    }
  }

  public Connection getConnection() throws SQLException {
    if (datasource != null) {
      Connection connection = datasource.getConnection();
      return connection;
    }
    return null;
  }

  @Override
  public void setupEntityManagerFactory(Properties factoryProps) {
    factoryProps.put(Environment.CONNECTION_PROVIDER, CustomHibernateConnectionProvider.class.getName());
  }

}