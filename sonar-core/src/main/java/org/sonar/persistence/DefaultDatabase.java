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

import com.google.common.collect.Lists;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbcp.BasicDataSourceFactory;
import org.apache.commons.lang.StringUtils;
import org.hibernate.cfg.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.config.Settings;
import org.sonar.api.database.DatabaseProperties;
import org.sonar.jpa.dialect.*;
import org.sonar.jpa.session.CustomHibernateConnectionProvider;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * @since 2.12
 */
public class DefaultDatabase implements Database {

  private static final Logger LOG = LoggerFactory.getLogger(Database.class);

  private Settings settings;
  private BasicDataSource datasource;
  private Dialect dialect;
  private String schema;

  public DefaultDatabase(Settings settings) {
    this.settings = settings;
  }

  public final DefaultDatabase start() {
    try {
      doBeforeStart();

      Properties properties = getProperties();
      dialect = initDialect(properties);
      schema = initSchema(properties, dialect);
      datasource = initDatasource(properties, dialect, schema);
      return this;

    } catch (Exception e) {
      throw new IllegalStateException("Fail to connect to database", e);
    }
  }

  static Dialect initDialect(Properties properties) {
    Dialect result = DialectRepository.find(properties.getProperty("sonar.jdbc.dialect"), properties.getProperty("sonar.jdbc.url"));
    if (result != null && Derby.ID.equals(result.getId())) {
      LoggerFactory.getLogger(DefaultDatabase.class).warn("Derby database should be used for evaluation purpose only");
    }
    return result;
  }

  static String initSchema(Properties properties, Dialect dialect) {
    String result = null;
    if (PostgreSql.ID.equals(dialect.getId())) {
      result = getSchemaPropertyValue(properties, "sonar.jdbc.postgreSearchPath");

    } else if (Oracle.ID.equals(dialect.getId())) {
      result = getSchemaPropertyValue(properties, "sonar.hibernate.default_schema");
    }
    return StringUtils.isNotBlank(result) ? result : null;
  }

  static BasicDataSource initDatasource(Properties properties, Dialect dialect, String schema) throws Exception {
    LOG.info("Create JDBC datasource");
    BasicDataSource result = (BasicDataSource) BasicDataSourceFactory.createDataSource(extractCommonsDbcpProperties(properties));
    result.setConnectionInitSqls(getConnectionInitStatements(dialect, schema));
    return result;
  }

  protected void doBeforeStart() {
  }

  public final DefaultDatabase stop() {
    doBeforeStop();
    if (datasource != null) {
      try {
        datasource.close();
      } catch (SQLException e) {
        throw new IllegalStateException("Fail to stop JDBC connection pool", e);
      }
    }
    return this;
  }

  protected void doBeforeStop() {

  }

  public final Dialect getDialect() {
    return dialect;
  }

  public final String getSchema() {
    return schema;
  }

  public Properties getHibernateProperties() {
    Properties props = new Properties();

    List<String> hibernateKeys = settings.getKeysStartingWith("sonar.hibernate.");
    for (String hibernateKey : hibernateKeys) {
      props.put(StringUtils.removeStart(hibernateKey, "sonar."), settings.getString(hibernateKey));
    }
    props.put(Environment.DIALECT, getDialect().getHibernateDialectClass().getName());
    props.put("hibernate.generate_statistics", settings.getBoolean(DatabaseProperties.PROP_HIBERNATE_GENERATE_STATISTICS));
    props.put("hibernate.hbm2ddl.auto", "validate");
    props.put(Environment.CONNECTION_PROVIDER, CustomHibernateConnectionProvider.class.getName());

    if (schema!=null) {
      props.put("hibernate.default_schema", schema);
    }
    return props;
  }

  public final DataSource getDataSource() {
    return datasource;
  }

  public final Properties getProperties() {
    Properties properties = new Properties();
    completeProperties(settings, properties, "sonar.jdbc.");
    completeProperties(settings, properties, "sonar.hibernate.");
    completeDefaultProperties(properties);
    doCompleteProperties(properties);
    return properties;
  }

  protected void doCompleteProperties(Properties properties) {

  }

  static void completeProperties(Settings settings, Properties properties, String prefix) {
    List<String> jdbcKeys = settings.getKeysStartingWith(prefix);
    for (String jdbcKey : jdbcKeys) {
      String value = settings.getString(jdbcKey);
      properties.setProperty(jdbcKey, value);
    }
  }

  static Properties extractCommonsDbcpProperties(Properties properties) {
    Properties result = new Properties();
    for (Map.Entry<Object, Object> entry : properties.entrySet()) {
      String key = (String) entry.getKey();
      if (StringUtils.startsWith(key, "sonar.jdbc.")) {
        result.setProperty(StringUtils.removeStart(key, "sonar.jdbc."), (String) entry.getValue());
      }
    }

    // This property is required by the Ruby Oracle enhanced adapter.
    // It directly uses the Connection implementation provided by the Oracle driver
    result.setProperty("accessToUnderlyingConnectionAllowed", "true");
    return result;
  }

  static List<String> getConnectionInitStatements(Dialect dialect, String schema) {
    List<String> result = Lists.newArrayList();
    if (StringUtils.isNotBlank(schema)) {
      if (PostgreSql.ID.equals(dialect.getId())) {
        result.add("SET SEARCH_PATH TO " + schema);

      } else if (Oracle.ID.equals(dialect.getId())) {
        result.add("ALTER SESSION SET CURRENT SCHEMA = " + schema);
      }
    }
    return result;
  }

  static String getSchemaPropertyValue(Properties props, String deprecatedKey) {
    String value = props.getProperty("sonar.jdbc.schema");
    if (StringUtils.isBlank(value) && deprecatedKey != null) {
      value = props.getProperty(deprecatedKey);
    }
    return StringUtils.isNotBlank(value) ? value : null;
  }

  private static void completeDefaultProperties(Properties props) {
    completeDefaultProperty(props, DatabaseProperties.PROP_DRIVER, props.getProperty(DatabaseProperties.PROP_DRIVER_DEPRECATED, DatabaseProperties.PROP_DRIVER_DEFAULT_VALUE));
    completeDefaultProperty(props, DatabaseProperties.PROP_URL, DatabaseProperties.PROP_URL_DEFAULT_VALUE);
    completeDefaultProperty(props, DatabaseProperties.PROP_USER, props.getProperty(DatabaseProperties.PROP_USER_DEPRECATED, DatabaseProperties.PROP_USER_DEFAULT_VALUE));
    completeDefaultProperty(props, DatabaseProperties.PROP_PASSWORD, DatabaseProperties.PROP_PASSWORD_DEFAULT_VALUE);
    completeDefaultProperty(props, DatabaseProperties.PROP_HIBERNATE_HBM2DLL, "validate");
  }

  private static void completeDefaultProperty(Properties props, String key, String defaultValue) {
    if (props.getProperty(key) == null) {
      props.setProperty(key, defaultValue);
    }
  }
}
