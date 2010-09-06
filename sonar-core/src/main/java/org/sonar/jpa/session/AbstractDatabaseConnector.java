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
package org.sonar.jpa.session;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.database.DatabaseProperties;
import org.sonar.jpa.dialect.Dialect;
import org.sonar.jpa.dialect.DialectRepository;
import org.sonar.jpa.entity.SchemaMigration;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

public abstract class AbstractDatabaseConnector implements DatabaseConnector {
  protected static final Logger LOG_SQL = LoggerFactory.getLogger("org.hibernate.SQL");
  protected static final Logger LOG = LoggerFactory.getLogger(AbstractDatabaseConnector.class);

  private Configuration configuration = null;
  private EntityManagerFactory factory = null;
  private int databaseVersion = SchemaMigration.VERSION_UNKNOWN;
  private boolean operational = false;
  private boolean started = false;
  private boolean startsFailIfSchemaOutdated;
  private Integer transactionIsolation = null;
  private Dialect dialect = null;

  protected AbstractDatabaseConnector(Configuration configuration, boolean startsFailIfSchemaOutdated) {
    this.configuration = configuration;
    this.startsFailIfSchemaOutdated = startsFailIfSchemaOutdated;
  }

  protected AbstractDatabaseConnector() {
  }

  public Configuration getConfiguration() {
    return configuration;
  }

  public void setConfiguration(Configuration configuration) {
    this.configuration = configuration;
  }

  public String getDialectId() {
    return dialect.getId();
  }
  /**
   * Indicates if the connector is operational : database connection OK and schema version OK
   */
  public boolean isOperational() {
    return operational;
  }

  /**
   * Indicates if the connector is started : database connection OK and schema version OK or KO
   */
  protected boolean isStarted() {
    return started;
  }

  /**
   * Get the JDBC transaction isolation defined by the configuration
   *
   * @return JDBC transaction isolation
   */
  public Integer getTransactionIsolation() {
    return transactionIsolation;
  }

  public void start() {
    if (!started) {
      transactionIsolation = configuration.getInteger(DatabaseProperties.PROP_ISOLATION, null /* use driver default setting */);
      String jdbcConnectionUrl = testConnection();
      dialect = DialectRepository.find(configuration.getString("sonar.jdbc.dialect"), jdbcConnectionUrl);
      LoggerFactory.getLogger("org.sonar.INFO").info("Database dialect class " + dialect.getClass().getName());
      started = true;
    }
    if (!operational) {
      boolean upToDate = upToDateSchemaVersion();
      if (!upToDate && startsFailIfSchemaOutdated) {
        throw new DatabaseException(databaseVersion, SchemaMigration.LAST_VERSION);
      }
      if (upToDate) {
        factory = createEntityManagerFactory();
        operational = true;
      }
    }
  }

  public void stop() {
    if (factory != null && factory.isOpen()) {
      factory.close();
      factory = null;
    }
    operational = false;
    started = false;
  }

  public abstract void setupEntityManagerFactory(Properties factoryProps);

  public EntityManagerFactory getEntityManagerFactory() {
    return factory;
  }

  protected void setEntityManagerFactory(EntityManagerFactory factory) {
    this.factory = factory;
  }

  protected EntityManagerFactory createEntityManagerFactory() {
    // other settings are stored into /META-INF/persistence.xml
    Properties props = getHibernateProperties();
    logHibernateSettings(props);
    return Persistence.createEntityManagerFactory("sonar", props);
  }

  private void logHibernateSettings(Properties props) {
    if (LOG.isDebugEnabled()) {
      for (Map.Entry<Object, Object> entry : props.entrySet()) {
        LOG.debug(entry.getKey() + ": " + entry.getValue());
      }
    }
  }

  protected Properties getHibernateProperties() {
    Properties props = new Properties();
    if (transactionIsolation != null) {
      props.put("hibernate.connection.isolation", Integer.toString(transactionIsolation));
    }
    props.put("hibernate.hbm2ddl.auto", getConfiguration().getString(DatabaseProperties.PROP_HIBERNATE_HBM2DLL, "validate"));
    props.put("hibernate.dialect", getDialectClass());

    props.put("hibernate.generate_statistics", getConfiguration().getBoolean(DatabaseProperties.PROP_HIBERNATE_GENERATE_STATISTICS, false));
    props.put("hibernate.show_sql", Boolean.valueOf(LOG_SQL.isInfoEnabled()).toString());

    Configuration subset = getConfiguration().subset("sonar.hibernate");
    for (Iterator keys = subset.getKeys(); keys.hasNext();) {
      String key = (String) keys.next();
      if (StringUtils.isNotBlank((String)subset.getProperty(key))) {
        props.put("hibernate." + key, subset.getProperty(key));
      }
    }

    // custom impl setup
    setupEntityManagerFactory(props);


    return props;
  }

  public EntityManager createEntityManager() {
    return factory.createEntityManager();
  }

  private String testConnection() throws DatabaseException {
    Connection connection = null;
    try {
      connection = getConnection();
      return connection.getMetaData().getURL();

    } catch (SQLException e) {
      throw new DatabaseException("Cannot open connection to database: " + e.getMessage(), e);

    } finally {
      close(connection);
    }
  }

  protected int loadVersion() {
    Connection connection = null;
    try {
      connection = getConnection();
      return SchemaMigration.getCurrentVersion(connection);

    } catch (SQLException e) {
      // schema not created
      return 0;
    } finally {
      close(connection);
    }
  }

  private void close(Connection connection) {
    if (connection != null) {
      try {
        connection.close();
      } catch (SQLException e) {
        // why does close() throw a checked-exception ???
      }
    }
  }

  protected boolean upToDateSchemaVersion() {
    if (databaseVersion == SchemaMigration.LAST_VERSION) {
      return true;
    }
    databaseVersion = loadVersion();
    return databaseVersion == SchemaMigration.LAST_VERSION;
  }

  protected int getDatabaseVersion() {
    return databaseVersion;
  }

  public Dialect getDialect() {
    return dialect;
  }

  public String getDialectClass() {
    String dialectClass = configuration.getString(DatabaseProperties.PROP_DIALECT_CLASS);
    if (dialectClass == null && dialect != null) {
      dialectClass = dialect.getHibernateDialectClass().getName();
    }
    return dialectClass;
  }

}
