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
import org.hamcrest.core.Is;
import org.junit.Test;
import org.sonar.api.config.Settings;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import static org.junit.Assert.assertThat;

public class DefaultDatabaseTest {
  @Test
  public void shouldLoadDefaultValues() {
    DefaultDatabase db = new DefaultDatabase(new Settings());
    Properties props = db.getProperties();
    assertThat(props.getProperty("sonar.jdbc.username"), Is.is("sonar"));
    assertThat(props.getProperty("sonar.jdbc.password"), Is.is("sonar"));
    assertThat(props.getProperty("sonar.jdbc.url"), Is.is("jdbc:derby://localhost:1527/sonar"));
    assertThat(props.getProperty("sonar.jdbc.driverClassName"), Is.is("org.apache.derby.jdbc.ClientDriver"));
  }

  @Test
  public void shouldSupportDeprecatedProperties() {
    Settings settings = new Settings();
    settings.setProperty("sonar.jdbc.driver", "my.Driver");
    settings.setProperty("sonar.jdbc.user", "me");

    DefaultDatabase db = new DefaultDatabase(settings);
    Properties props = db.getProperties();

    assertThat(props.getProperty("sonar.jdbc.username"), Is.is("me"));
    assertThat(props.getProperty("sonar.jdbc.driverClassName"), Is.is("my.Driver"));
  }

  @Test
  public void shouldExtractCommonsDbcpProperties() {
    Properties props = new Properties();
    props.setProperty("sonar.jdbc.driverClassName", "my.Driver");
    props.setProperty("sonar.jdbc.username", "me");
    props.setProperty("sonar.jdbc.maxActive", "5");

    Properties commonsDbcpProps = DefaultDatabase.extractCommonsDbcpProperties(props);

    assertThat(commonsDbcpProps.getProperty("username"), Is.is("me"));
    assertThat(commonsDbcpProps.getProperty("driverClassName"), Is.is("my.Driver"));
    assertThat(commonsDbcpProps.getProperty("maxActive"), Is.is("5"));
  }

  @Test
  public void shouldCompleteProperties() {
    Settings settings = new Settings();

    DefaultDatabase db = new DefaultDatabase(settings) {
      @Override
      protected void doCompleteProperties(Properties properties) {
        properties.setProperty("sonar.jdbc.maxActive", "2");
      }
    };

    Properties props = db.getProperties();

    assertThat(props.getProperty("sonar.jdbc.maxActive"), Is.is("2"));
  }

  @Test
  public void shouldStart() throws SQLException {
    Settings settings = new Settings();
    settings.setProperty("sonar.jdbc.url", "jdbc:derby:memory:sonar;create=true;user=sonar;password=sonar");
    settings.setProperty("sonar.jdbc.driverClassName", "org.apache.derby.jdbc.EmbeddedDriver");
    settings.setProperty("sonar.jdbc.username", "sonar");
    settings.setProperty("sonar.jdbc.password", "sonar");
    settings.setProperty("sonar.jdbc.maxActive", "1");

    try {
      DefaultDatabase db = new DefaultDatabase(settings);
      db.start();

      assertThat(db.getDialect().getId(), Is.is("derby"));
      assertThat(((BasicDataSource) db.getDataSource()).getMaxActive(), Is.is(1));
    } finally {
      try {
        DriverManager.getConnection("jdbc:derby:memory:sonar;drop=true");
      } catch (Exception e) {
        // silently ignore
      }
    }
  }
}
