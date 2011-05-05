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

import org.apache.commons.configuration.Configuration;
import org.apache.commons.dbcp.BasicDataSourceFactory;
import org.apache.commons.lang.StringUtils;
import org.hibernate.cfg.Environment;
import org.sonar.api.database.DatabaseProperties;
import org.sonar.api.utils.Logs;
import org.sonar.api.utils.SonarException;
import org.sonar.jpa.entity.SchemaMigration;
import org.sonar.jpa.session.AbstractDatabaseConnector;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Properties;

public class JndiDatabaseConnector extends AbstractDatabaseConnector {

  static final String JNDI_ENV_CONTEXT = "java:comp/env";
  private DataSource datasource = null;
  private String jndiKey;

  public JndiDatabaseConnector(Configuration configuration) {
    super(configuration, false);
    jndiKey = getConfiguration().getString(DatabaseProperties.PROP_JNDI_NAME);
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
      if (StringUtils.isNotBlank(jndiKey)) {
        loadJndiDatasource();
      } else {
        createDatasource();
      }
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


  private void loadJndiDatasource() {
    Context ctx;
    try {
      ctx = new InitialContext();

    } catch (NamingException e) {
      throw new SonarException("Can not instantiate JNDI context", e);
    }

    try {
      Context envCtx = (Context) ctx.lookup(JNDI_ENV_CONTEXT);
      datasource = (DataSource) envCtx.lookup(jndiKey);
      Logs.INFO.info("JDBC datasource loaded from JNDI: " + jndiKey);

    } catch (NamingException e) {
      throw new SonarException("JNDI context of JDBC datasource not found: " + jndiKey, e);

    } finally {
      try {
        ctx.close();
      } catch (NamingException e) {
      }
    }
  }

  private void createDatasource() {
    try {
      Logs.INFO.info("Creating JDBC datasource");
      Properties properties = new Properties();
      Configuration dsConfig = getConfiguration().subset("sonar.jdbc");
      for (Iterator<String> it = dsConfig.getKeys(); it.hasNext();) {
        String key = it.next();
        properties.setProperty(key, dsConfig.getString(key));
      }

      datasource = BasicDataSourceFactory.createDataSource(properties);
      CustomHibernateConnectionProvider.datasource = datasource;
    } catch (Exception e) {
      throw new SonarException("Fail to connect to database", e);
    }
  }

  public Connection getConnection() throws SQLException {
    if (datasource != null) {
      Connection connection = datasource.getConnection();
      if (getTransactionIsolation() != null) {
        connection.setTransactionIsolation(getTransactionIsolation());
      }
      return connection;
    }
    return null;
  }

  @Override
  public void setupEntityManagerFactory(Properties factoryProps) {
    if (StringUtils.isNotBlank(jndiKey)) {
      factoryProps.put(Environment.DATASOURCE, JNDI_ENV_CONTEXT + "/" + jndiKey);
    } else {
      factoryProps.put(Environment.CONNECTION_PROVIDER, CustomHibernateConnectionProvider.class.getName());
    }
  }

}