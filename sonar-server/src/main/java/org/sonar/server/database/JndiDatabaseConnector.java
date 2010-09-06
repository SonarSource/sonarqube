/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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
import org.sonar.api.database.DatabaseProperties;
import org.sonar.api.utils.Logs;
import org.sonar.jpa.entity.SchemaMigration;
import org.sonar.jpa.session.AbstractDatabaseConnector;

import javax.naming.*;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Properties;

public class JndiDatabaseConnector extends AbstractDatabaseConnector {

  private DataSource datasource = null;

  public JndiDatabaseConnector(Configuration configuration) {
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
      // get the datasource from JNDI
      datasource = loadDatasourceFromJndi();
      // bind the datasource to JNDI if it is not already done
      if (datasource == null) {
        createAndBindDatasource();
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

  private String getJndiName() {
    return getConfiguration().getString(DatabaseProperties.PROP_JNDI_NAME, "jdbc/sonar");
  }

  private DataSource loadDatasourceFromJndi() {
    Context ctx;
    try {
      ctx = new InitialContext();
    } catch (NamingException e) {
      throw new JndiException("can not instantiate a JNDI context", e);
    }

    try {
      String jndiName = getJndiName();
      DataSource source = (DataSource) ctx.lookup(jndiName);
      Logs.INFO.info("Use JDBC datasource from JNDI, name=" + jndiName);
      return source;
    } catch (NamingException e) {
      // datasource not found
    } finally {
      try {
        ctx.close();
      } catch (NamingException e) {
      }
    }
    return null;
  }

  private void createAndBindDatasource() {
    Reference ref = createDatasourceReference();

    // bind the datasource to JNDI
    Context ctx = null;
    try {
      ctx = new InitialContext();
      createJNDISubContexts(ctx, getJndiName());
      ctx.rebind(getJndiName(), ref);
      datasource = (DataSource) ctx.lookup(getJndiName());
      Logs.INFO.info("JDBC datasource bound to JNDI, name=" + getJndiName());

    } catch (NamingException e) {
      throw new JndiException("Can not bind JDBC datasource to JNDI", e);

    } finally {
      if (ctx != null) {
        try {
          ctx.close();
        } catch (NamingException e) {
        }
      }
    }
  }

  private Reference createDatasourceReference() {
    try {
      Reference ref = new Reference(DataSource.class.getName(), UniqueDatasourceFactory.class.getName(), null);
      Configuration dsConfig = getConfiguration().subset("sonar.jdbc");
      for (Iterator<String> it = dsConfig.getKeys(); it.hasNext();) {
        String key = it.next();
        String value = dsConfig.getString(key);
        ref.add(new StringRefAddr(key, value));

        // backward compatibility
        if (value != null && key.equals("user")) {
          ref.add(new StringRefAddr("username", value));
        }
        if (value != null && key.equals("driver")) {
          ref.add(new StringRefAddr("driverClassName", value));
        }
      }
      return ref;
    } catch (Exception e) {
      throw new RuntimeException("Cannot create the JDBC datasource", e);
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
    factoryProps.put("hibernate.connection.datasource", getJndiName());
  }

  private void createJNDISubContexts(Context ctx, String jndiBinding) throws NamingException {
    Name name = new CompositeName(jndiBinding);
    for (int i = 0; i < name.size() - 1; i++) {
      String namingContext = name.get(i);
      try {
        Object obj = ctx.lookup(namingContext);
        if (!(obj instanceof Context)) {
          throw new NamingException(namingContext + " is not a JNDI Context");
        }
      } catch (NameNotFoundException ex) {
        ctx = ctx.createSubcontext(namingContext);
      }
    }
  }

}