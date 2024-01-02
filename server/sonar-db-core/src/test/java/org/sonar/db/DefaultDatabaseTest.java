/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import com.zaxxer.hikari.HikariDataSource;
import java.util.Properties;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.db.dialect.PostgreSql;
import org.sonar.process.logging.LogbackHelper;

import static org.apache.commons.lang.StringUtils.removeStart;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

@RunWith(DataProviderRunner.class)
public class DefaultDatabaseTest {

  private final LogbackHelper logbackHelper = mock(LogbackHelper.class);
  private static final String SONAR_JDBC = "sonar.jdbc.";

  @Rule
  public final LogTester logTester = new LogTester();

  @Test
  public void shouldLoadDefaultValues() {
    DefaultDatabase db = new DefaultDatabase(logbackHelper, new MapSettings());
    db.initSettings();

    Properties props = db.getProperties();
    assertThat(props.getProperty("sonar.jdbc.url")).isEqualTo("jdbc:h2:tcp://localhost/sonar;NON_KEYWORDS=VALUE");
    assertThat(props.getProperty("sonar.jdbc.driverClassName")).isEqualTo("org.h2.Driver");
    assertThat(db).hasToString("Database[jdbc:h2:tcp://localhost/sonar;NON_KEYWORDS=VALUE]");
  }

  @Test
  public void getSettings_shouldReturnExpectedSettings() {
    MapSettings settings = new MapSettings();
    settings.setProperty("test", "test");
    DefaultDatabase db = new DefaultDatabase(logbackHelper, settings);
    assertThat(db.getSettings()).isEqualTo(settings);
  }
  @Test
  public void shouldExtractHikariProperties() {
    Properties props = new Properties();
    props.setProperty("sonar.jdbc.driverClassName", "my.Driver");
    props.setProperty("sonar.jdbc.username", "me");
    props.setProperty("sonar.jdbc.dataSource.password", "my_password");
    props.setProperty("sonar.jdbc.dataSource.portNumber", "9999");
    props.setProperty("sonar.jdbc.connectionTimeout", "8000");
    props.setProperty("sonar.jdbc.maximumPoolSize", "80");
    props.setProperty("sonar.jdbc.minimumIdle", "10");
    props.setProperty("sonar.jdbc.schema", "db-schema");
    props.setProperty("sonar.jdbc.validationTimeout", "5000");
    props.setProperty("sonar.jdbc.catalog", "db-catalog");
    props.setProperty("sonar.jdbc.initializationFailTimeout", "5000");
    props.setProperty("sonar.jdbc.maxLifetime", "1800000");
    props.setProperty("sonar.jdbc.leakDetectionThreshold", "0");
    props.setProperty("sonar.jdbc.keepaliveTime", "30000");
    props.setProperty("sonar.jdbc.idleTimeout", "600000");

    Properties hikariProps = DefaultDatabase.extractCommonsHikariProperties(props);

    assertThat(hikariProps).hasSize(15);
    assertThat(hikariProps.getProperty("driverClassName")).isEqualTo("my.Driver");
    assertThat(hikariProps.getProperty("dataSource.user")).isEqualTo("me");
    assertThat(hikariProps.getProperty("dataSource.password")).isEqualTo("my_password");
    assertThat(hikariProps.getProperty("dataSource.portNumber")).isEqualTo("9999");
    assertThat(hikariProps.getProperty("connectionTimeout")).isEqualTo("8000");
    assertThat(hikariProps.getProperty("maximumPoolSize")).isEqualTo("80");
    assertThat(hikariProps.getProperty("minimumIdle")).isEqualTo("10");
    assertThat(hikariProps.getProperty("schema")).isEqualTo("db-schema");
    assertThat(hikariProps.getProperty("validationTimeout")).isEqualTo("5000");
    assertThat(hikariProps.getProperty("catalog")).isEqualTo("db-catalog");
    assertThat(hikariProps.getProperty("initializationFailTimeout")).isEqualTo("5000");
    assertThat(hikariProps.getProperty("maxLifetime")).isEqualTo("1800000");
    assertThat(hikariProps.getProperty("leakDetectionThreshold")).isEqualTo("0");
    assertThat(hikariProps.getProperty("keepaliveTime")).isEqualTo("30000");
    assertThat(hikariProps.getProperty("idleTimeout")).isEqualTo("600000");

  }

  @Test
  public void logWarningIfDeprecatedPropertyUsed() {
    Properties props = new Properties();

    props.setProperty("sonar.jdbc.maxIdle", "5");
    props.setProperty("sonar.jdbc.minEvictableIdleTimeMillis", "300000");
    props.setProperty("sonar.jdbc.timeBetweenEvictionRunsMillis", "1000");
    props.setProperty("sonar.jdbc.connectionTimeout", "8000");

    DefaultDatabase.extractCommonsHikariProperties(props);

    assertThat(logTester.logs())
      .contains("Property [sonar.jdbc.maxIdle] has no effect as pool connection implementation changed, check 9.7 upgrade notes.")
      .contains("Property [sonar.jdbc.minEvictableIdleTimeMillis] has no effect as pool connection implementation changed, check 9.7 upgrade notes.")
      .contains("Property [sonar.jdbc.timeBetweenEvictionRunsMillis] has no effect as pool connection implementation changed, check 9.7 upgrade notes.");
  }

  @Test
  @UseDataProvider("sonarJdbcAndHikariProperties")
  public void shouldExtractCommonsDbcpPropertiesIfDuplicatedPropertiesWithSameValue(String jdbcProperty, String dbcpProperty) {
    Properties props = new Properties();
    props.setProperty(jdbcProperty, "100");
    props.setProperty(dbcpProperty, "100");

    Properties commonsDbcpProps = DefaultDatabase.extractCommonsHikariProperties(props);

    assertThat(commonsDbcpProps.getProperty(removeStart(dbcpProperty, SONAR_JDBC))).isEqualTo("100");
  }

  @Test
  @UseDataProvider("sonarJdbcAndHikariProperties")
  public void shouldThrowISEIfDuplicatedResolvedPropertiesWithDifferentValue(String jdbcProperty, String hikariProperty) {
    Properties props = new Properties();
    props.setProperty(jdbcProperty, "100");
    props.setProperty(hikariProperty, "200");

    assertThatThrownBy(() -> DefaultDatabase.extractCommonsHikariProperties(props))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining(String.format("Duplicate property declaration for resolved jdbc key '%s': conflicting values are", removeStart(hikariProperty, SONAR_JDBC)));
  }

  @Test
  public void shouldCompleteProperties() {
    MapSettings settings = new MapSettings();

    DefaultDatabase db = new DefaultDatabase(logbackHelper, settings) {
      @Override
      protected void doCompleteProperties(Properties properties) {
        properties.setProperty("sonar.jdbc.maxActive", "2");
      }
    };
    db.initSettings();

    Properties props = db.getProperties();

    assertThat(props.getProperty("sonar.jdbc.maxActive")).isEqualTo("2");
  }

  @Test
  public void shouldStart() {
    MapSettings settings = new MapSettings();
    settings.setProperty("sonar.jdbc.url", "jdbc:h2:mem:sonar;NON_KEYWORDS=VALUE");
    settings.setProperty("sonar.jdbc.driverClassName", "org.h2.Driver");
    settings.setProperty("sonar.jdbc.username", "sonar");
    settings.setProperty("sonar.jdbc.password", "sonar");
    settings.setProperty("sonar.jdbc.maximumPoolSize", "1");

    DefaultDatabase db = new DefaultDatabase(logbackHelper, settings);
    db.start();
    db.stop();

    assertThat(db.getDialect().getId()).isEqualTo("h2");
    assertThat(((HikariDataSource) db.getDataSource()).getMaximumPoolSize()).isOne();
  }

  @Test
  public void shouldGuessDialectFromUrl() {
    MapSettings settings = new MapSettings();
    settings.setProperty("sonar.jdbc.url", "jdbc:postgresql://localhost/sonar");

    DefaultDatabase database = new DefaultDatabase(logbackHelper, settings);
    database.initSettings();

    assertThat(database.getDialect().getId()).isEqualTo(PostgreSql.ID);
  }

  @Test
  public void shouldGuessDefaultDriver() {
    MapSettings settings = new MapSettings();
    settings.setProperty("sonar.jdbc.url", "jdbc:postgresql://localhost/sonar");

    DefaultDatabase database = new DefaultDatabase(logbackHelper, settings);
    database.initSettings();

    assertThat(database.getProperties().getProperty("sonar.jdbc.driverClassName")).isEqualTo("org.postgresql.Driver");
  }

  @DataProvider
  public static Object[][] sonarJdbcAndHikariProperties() {
    return new Object[][] {
      {"sonar.jdbc.maxWait", "sonar.jdbc.connectionTimeout"},
      {"sonar.jdbc.maxActive", "sonar.jdbc.maximumPoolSize"}
    };
  }
}
