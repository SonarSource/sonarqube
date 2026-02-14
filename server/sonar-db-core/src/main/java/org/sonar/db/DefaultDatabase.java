/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.db;

import ch.qos.logback.classic.Level;
import com.google.common.annotations.VisibleForTesting;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.config.internal.Settings;
import org.sonar.db.dialect.Dialect;
import org.sonar.db.dialect.DialectUtils;
import org.sonar.db.dialect.MsSql;
import org.sonar.db.dialect.PostgreSql;
import org.sonar.db.profiling.NullConnectionInterceptor;
import org.sonar.db.profiling.ProfiledConnectionInterceptor;
import org.sonar.db.profiling.ProfiledDataSource;
import org.sonar.db.tokenprovider.AwsTokenProvider;
import org.sonar.db.tokenprovider.AzureTokenProvider;
import org.sonar.db.tokenprovider.MssqlHikariConfigProvider;
import org.sonar.db.tokenprovider.PostgresqlHikariConfigProvider;
import org.sonar.process.logging.LogbackHelper;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static org.apache.commons.lang3.Strings.CS;
import static org.sonar.process.ProcessProperties.Property.JDBC_EMBEDDED_PORT;
import static org.sonar.process.ProcessProperties.Property.JDBC_MAX_IDLE_TIMEOUT;
import static org.sonar.process.ProcessProperties.Property.JDBC_MAX_KEEP_ALIVE_TIME;
import static org.sonar.process.ProcessProperties.Property.JDBC_MAX_LIFETIME;
import static org.sonar.process.ProcessProperties.Property.JDBC_MIN_IDLE;
import static org.sonar.process.ProcessProperties.Property.JDBC_PASSWORD;
import static org.sonar.process.ProcessProperties.Property.JDBC_URL;
import static org.sonar.process.ProcessProperties.Property.JDBC_USERNAME;
import static org.sonar.process.ProcessProperties.Property.JDBC_USE_AWS_MANAGED_IDENTITY;
import static org.sonar.process.ProcessProperties.Property.JDBC_USE_AZURE_MANAGED_IDENTITY;
import static org.sonar.process.ProcessProperties.Property.JDBC_VALIDATION_TIMEOUT;

/**
 * @since 2.12
 */
public class DefaultDatabase implements Database {
  private static final String IGNORED_KEYWORDS_OPTION = ";NON_KEYWORDS=VALUE";
  private static final Logger LOG = LoggerFactory.getLogger(DefaultDatabase.class);

  private static final String DEFAULT_URL = "jdbc:h2:tcp://localhost/sonar" + IGNORED_KEYWORDS_OPTION;
  private static final String SONAR_JDBC = "sonar.jdbc.";
  private static final String SONAR_JDBC_DIALECT = "sonar.jdbc.dialect";
  private static final String SONAR_JDBC_DRIVER = "sonar.jdbc.driverClassName";
  private static final String SONAR_JDBC_MAX_ACTIVE = "sonar.jdbc.maxActive";
  private static final String SONAR_JDBC_MAX_WAIT = "sonar.jdbc.maxWait";
  private static final Set<String> DEPRECATED_SONAR_PROPERTIES = Set.of(
    "sonar.jdbc.maxIdle",
    "sonar.jdbc.minEvictableIdleTimeMillis",
    "sonar.jdbc.timeBetweenEvictionRunsMillis");

  private static final Set<String> ALLOWED_SONAR_PROPERTIES = Set.of(
    JDBC_USERNAME.getKey(),
    JDBC_PASSWORD.getKey(),
    JDBC_EMBEDDED_PORT.getKey(),
    JDBC_USE_AZURE_MANAGED_IDENTITY.getKey(),
    JDBC_USE_AWS_MANAGED_IDENTITY.getKey(),
    JDBC_URL.getKey(),
    JDBC_MIN_IDLE.getKey(),
    SONAR_JDBC_MAX_WAIT,
    SONAR_JDBC_MAX_ACTIVE,
    // allowed hikari cp direct properties
    // see: https://github.com/brettwooldridge/HikariCP#frequently-used
    SONAR_JDBC_DRIVER,
    "sonar.jdbc.dataSource.user",
    "sonar.jdbc.dataSource.password",
    "sonar.jdbc.dataSource.portNumber",
    "sonar.jdbc.jdbcUrl",
    "sonar.jdbc.connectionTimeout",
    "sonar.jdbc.maximumPoolSize",
    "sonar.jdbc.minimumIdle",
    "sonar.jdbc.schema",
    JDBC_VALIDATION_TIMEOUT.getKey(),
    "sonar.jdbc.catalog",
    "sonar.jdbc.initializationFailTimeout",
    JDBC_MAX_LIFETIME.getKey(),
    "sonar.jdbc.leakDetectionThreshold",
    JDBC_MAX_KEEP_ALIVE_TIME.getKey(),
    JDBC_MAX_IDLE_TIMEOUT.getKey());

  private static final Map<String, String> SONAR_JDBC_TO_HIKARI_PROPERTY_MAPPINGS = Map.of(
    JDBC_USERNAME.getKey(), "dataSource.user",
    JDBC_PASSWORD.getKey(), "dataSource.password",
    JDBC_EMBEDDED_PORT.getKey(), "dataSource.portNumber",
    JDBC_URL.getKey(), "jdbcUrl",
    SONAR_JDBC_MAX_WAIT, "connectionTimeout",
    SONAR_JDBC_MAX_ACTIVE, "maximumPoolSize",
    JDBC_MIN_IDLE.getKey(), "minimumIdle");

  private final LogbackHelper logbackHelper;
  private final Settings settings;
  private ProfiledDataSource datasource;
  private Dialect dialect;
  private Properties properties;

  public DefaultDatabase(LogbackHelper logbackHelper, Settings settings) {
    this.logbackHelper = logbackHelper;
    this.settings = settings;
  }

  private static void enableSqlLogging(ProfiledDataSource ds, boolean enable) {
    ds.setConnectionInterceptor(enable ? ProfiledConnectionInterceptor.INSTANCE : NullConnectionInterceptor.INSTANCE);
  }

  private static void completeProperties(Settings settings, Properties properties, String prefix) {
    List<String> jdbcKeys = settings.getKeysStartingWith(prefix);
    for (String jdbcKey : jdbcKeys) {
      String value = settings.getString(jdbcKey);
      properties.setProperty(jdbcKey, value);
    }
  }

  @VisibleForTesting
  static Properties extractCommonsHikariProperties(Properties properties) {
    Properties result = new Properties();
    for (Map.Entry<Object, Object> entry : properties.entrySet()) {
      String key = (String) entry.getKey();
      if (!ALLOWED_SONAR_PROPERTIES.contains(key)) {
        if (DEPRECATED_SONAR_PROPERTIES.contains(key)) {
          LOG.warn("Property [{}] has no effect as pool connection implementation changed, check 9.7 upgrade notes.", key);
        }
        continue;
      }
      if (CS.startsWith(key, SONAR_JDBC) && !CS.contains(key, JDBC_USE_AZURE_MANAGED_IDENTITY.getKey()) && !CS.contains(key, JDBC_USE_AWS_MANAGED_IDENTITY.getKey())) {
        String resolvedKey = toHikariPropertyKey(key);
        String existingValue = (String) result.setProperty(resolvedKey, (String) entry.getValue());
        checkState(existingValue == null || existingValue.equals(entry.getValue()),
          "Duplicate property declaration for resolved jdbc key '%s': conflicting values are '%s' and '%s'", resolvedKey, existingValue, entry.getValue());
        result.setProperty(resolvedKey, (String) entry.getValue());
      }
    }
    return result;
  }

  private static void completeDefaultProperty(Properties props, String key, String defaultValue) {
    if (props.getProperty(key) == null) {
      props.setProperty(key, defaultValue);
    }
  }

  private static String toHikariPropertyKey(String key) {
    if (SONAR_JDBC_TO_HIKARI_PROPERTY_MAPPINGS.containsKey(key)) {
      return SONAR_JDBC_TO_HIKARI_PROPERTY_MAPPINGS.get(key);
    }

    return CS.removeStart(key, SONAR_JDBC);
  }

  @Override
  public void start() {
    initSettings();
    try {
      initDataSource();
      checkConnection();

    } catch (Exception e) {
      throw new IllegalStateException("Fail to connect to database", e);
    }
  }

  @VisibleForTesting
  void initSettings() {
    properties = new Properties();
    completeProperties(settings, properties, SONAR_JDBC);
    completeDefaultProperty(properties, JDBC_URL.getKey(), DEFAULT_URL);
    completeDefaultProperty(properties, JDBC_USE_AZURE_MANAGED_IDENTITY.getKey(), "false");
    completeDefaultProperty(properties, JDBC_USE_AWS_MANAGED_IDENTITY.getKey(), "false");
    doCompleteProperties(properties);

    String jdbcUrl = properties.getProperty(JDBC_URL.getKey());
    dialect = DialectUtils.find(properties.getProperty(SONAR_JDBC_DIALECT), jdbcUrl);
    properties.setProperty(SONAR_JDBC_DRIVER, dialect.getDefaultDriverClassName());
  }

  private void initDataSource() {
    LOG.atInfo()
      .addArgument(() -> properties.getProperty(JDBC_URL.getKey(), DEFAULT_URL))
      .log("Create JDBC data source for {}");
    HikariDataSource ds = createHikariDataSource();
    datasource = new ProfiledDataSource(ds, NullConnectionInterceptor.INSTANCE);
    enableSqlLogging(datasource, logbackHelper.getLoggerLevel("sql") == Level.TRACE);
  }

  private HikariDataSource createHikariDataSource() {
    boolean useIdentityAzure = Boolean.parseBoolean(properties.getProperty(JDBC_USE_AZURE_MANAGED_IDENTITY.getKey(), "false"));
    boolean useIdentityAws = Boolean.parseBoolean(properties.getProperty(JDBC_USE_AWS_MANAGED_IDENTITY.getKey(), "false"));

    String token = null;
    if (useIdentityAzure)
      token = new AzureTokenProvider().getToken(properties);
    if (useIdentityAws)
      token = new AwsTokenProvider().getToken(properties);
    if (token != null && !token.isEmpty()) {
      if (dialect.getId().equals(MsSql.ID))
        return MSSQLserver(token);
      if (dialect.getId().equals(PostgreSql.ID))
        return POSTGRESQLserver(token);
    }
    LOG.warn("Acces token cannot be obtain");
    return createdefaultHikariDataSource(); // oracle and non-identity-managed goes here
  }

  private HikariDataSource createdefaultHikariDataSource() {
    HikariConfig config = new HikariConfig(extractCommonsHikariProperties(properties));
    if (!dialect.getConnectionInitStatements().isEmpty()) {
      config.setConnectionInitSql(dialect.getConnectionInitStatements().get(0));
    }
    config.setConnectionTestQuery(dialect.getValidationQuery());
    return new HikariDataSource(config);
  }

  private HikariDataSource MSSQLserver(String token) {
    HikariConfig config = new MssqlHikariConfigProvider().getConfig(token, properties);
    config.setConnectionTestQuery(dialect.getValidationQuery());
    return new HikariDataSource(config);
  }

  private HikariDataSource POSTGRESQLserver(String token) {
    // Get token from Azure Identity
    HikariConfig config = new PostgresqlHikariConfigProvider().getConfig(token, properties);
    config.setConnectionTestQuery(dialect.getValidationQuery());
    return new HikariDataSource(config);
  }

  private void checkConnection() {
    Connection connection = null;
    try {
      connection = datasource.getConnection();
      dialect.init(connection.getMetaData());
    } catch (SQLException e) {
      throw new IllegalStateException("Can not connect to database. Please check connectivity and settings (see the properties prefixed by 'sonar.jdbc.').", e);
    } finally {
      DatabaseUtils.closeQuietly(connection);
    }
  }

  @Override
  public void stop() {
    if (datasource != null) {
      datasource.close();
    }
  }

  @Override
  public final Dialect getDialect() {
    return dialect;
  }

  @Override
  public final DataSource getDataSource() {
    return datasource;
  }

  public final Properties getProperties() {
    return properties;
  }

  @Override
  public void enableSqlLogging(boolean enable) {
    enableSqlLogging(datasource, enable);
  }

  /**
   * Override this method to add JDBC properties at runtime
   */
  protected void doCompleteProperties(Properties properties) {
    // open-close principle
  }

  @Override
  public String toString() {
    return format("Database[%s]", properties != null ? properties.getProperty(JDBC_URL.getKey()) : "?");
  }

  public Settings getSettings() {
    return settings;
  }
}
