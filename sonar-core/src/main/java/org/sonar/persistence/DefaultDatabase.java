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
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.config.Settings;
import org.sonar.api.database.DatabaseProperties;

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

  public DefaultDatabase(Settings settings) {
    this.settings = settings;
  }

  public final void start() {
    try {
      doBeforeStart();

      LOG.info("Create JDBC datasource");
      datasource = (BasicDataSource) BasicDataSourceFactory.createDataSource(getCommonsDbcpProperties());

    } catch (Exception e) {
      throw new IllegalStateException("Fail to connect to database", e);
    }
  }

  protected void doBeforeStart() {
  }

  public final void stop() {
    doBeforeStop();
    if (datasource != null) {
      try {
        datasource.close();
      } catch (SQLException e) {
        throw new IllegalStateException("Fail to stop JDBC connection pool", e);
      }
    }
  }

  protected void doBeforeStop() {

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

  Properties getCommonsDbcpProperties() {
    Properties result = new Properties();
    Properties props = getProperties();
    for (Map.Entry<Object, Object> entry : props.entrySet()) {
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
