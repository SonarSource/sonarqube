/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import java.util.Properties;
import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.sonar.api.config.Settings;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.db.dialect.PostgreSql;
import org.sonar.process.logging.LogbackHelper;

import static org.apache.commons.lang.StringUtils.removeStart;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Mockito.mock;

@RunWith(DataProviderRunner.class)
public class DefaultDatabaseTest {
  private LogbackHelper logbackHelper = mock(LogbackHelper.class);
  private static final String SONAR_JDBC = "sonar.jdbc.";


  @Rule
  public ExpectedException expectedException = none();

  @Test
  public void shouldLoadDefaultValues() {
    DefaultDatabase db = new DefaultDatabase(logbackHelper, new MapSettings());
    db.initSettings();

    Properties props = db.getProperties();
    assertThat(props.getProperty("sonar.jdbc.url")).isEqualTo("jdbc:h2:tcp://localhost/sonar");
    assertThat(props.getProperty("sonar.jdbc.driverClassName")).isEqualTo("org.h2.Driver");
    assertThat(db.toString()).isEqualTo("Database[jdbc:h2:tcp://localhost/sonar]");
  }

  @Test
  public void shouldExtractCommonsDbcpProperties() {
    Properties props = new Properties();
    props.setProperty("sonar.jdbc.driverClassName", "my.Driver");
    props.setProperty("sonar.jdbc.username", "me");
    props.setProperty("sonar.jdbc.maxActive", "5");
    props.setProperty("sonar.jdbc.maxWait", "5000");

    Properties commonsDbcpProps = DefaultDatabase.extractCommonsDbcpProperties(props);

    assertThat(commonsDbcpProps.getProperty("username")).isEqualTo("me");
    assertThat(commonsDbcpProps.getProperty("driverClassName")).isEqualTo("my.Driver");
    assertThat(commonsDbcpProps.getProperty("maxTotal")).isEqualTo("5");
    assertThat(commonsDbcpProps.getProperty("maxWaitMillis")).isEqualTo("5000");
  }

  @Test
  @UseDataProvider("sonarJdbcAndDbcpProperties")
  public void shouldExtractCommonsDbcpPropertiesIfDuplicatedPropertiesWithSameValue(String jdbcProperty, String dbcpProperty) {
    Properties props = new Properties();
    props.setProperty(jdbcProperty, "100");
    props.setProperty(dbcpProperty, "100");

    Properties commonsDbcpProps = DefaultDatabase.extractCommonsDbcpProperties(props);

    assertThat(commonsDbcpProps.getProperty(removeStart(dbcpProperty, SONAR_JDBC))).isEqualTo("100");
  }

  @Test
  @UseDataProvider("sonarJdbcAndDbcpProperties")
  public void shouldThrowISEIfDuplicatedResolvedPropertiesWithDifferentValue(String jdbcProperty, String dbcpProperty) {
    Properties props = new Properties();
    props.setProperty(jdbcProperty, "100");
    props.setProperty(dbcpProperty, "200");

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage(String.format("Duplicate property declaration for resolved jdbc key '%s': conflicting values are", removeStart(dbcpProperty, SONAR_JDBC)));

    DefaultDatabase.extractCommonsDbcpProperties(props);
  }

  @Test
  public void shouldCompleteProperties() {
    Settings settings = new MapSettings();

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
    Settings settings = new MapSettings();
    settings.setProperty("sonar.jdbc.url", "jdbc:h2:mem:sonar");
    settings.setProperty("sonar.jdbc.driverClassName", "org.h2.Driver");
    settings.setProperty("sonar.jdbc.username", "sonar");
    settings.setProperty("sonar.jdbc.password", "sonar");
    settings.setProperty("sonar.jdbc.maxActive", "1");

    DefaultDatabase db = new DefaultDatabase(logbackHelper, settings);
    db.start();
    db.stop();

    assertThat(db.getDialect().getId()).isEqualTo("h2");
    assertThat(((BasicDataSource) db.getDataSource()).getMaxTotal()).isEqualTo(1);
  }

  @Test
  public void shouldGuessDialectFromUrl() {
    Settings settings = new MapSettings();
    settings.setProperty("sonar.jdbc.url", "jdbc:postgresql://localhost/sonar");

    DefaultDatabase database = new DefaultDatabase(logbackHelper, settings);
    database.initSettings();

    assertThat(database.getDialect().getId()).isEqualTo(PostgreSql.ID);
  }

  @Test
  public void shouldGuessDefaultDriver() {
    Settings settings = new MapSettings();
    settings.setProperty("sonar.jdbc.url", "jdbc:postgresql://localhost/sonar");

    DefaultDatabase database = new DefaultDatabase(logbackHelper, settings);
    database.initSettings();

    assertThat(database.getProperties().getProperty("sonar.jdbc.driverClassName")).isEqualTo("org.postgresql.Driver");
  }

  @DataProvider
  public static Object[][] sonarJdbcAndDbcpProperties() {
    return new Object[][] {
      {"sonar.jdbc.maxActive", "sonar.jdbc.maxTotal"},
      {"sonar.jdbc.maxWait", "sonar.jdbc.maxWaitMillis"}
    };
  }
}
