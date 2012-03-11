/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
package org.sonar.server.platform;

import org.apache.commons.configuration.BaseConfiguration;
import org.junit.Test;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.core.properties.PropertiesDao;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

import java.io.File;
import java.net.URISyntaxException;

import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ServerSettingsTest extends AbstractDbUnitTestCase {

  private static File home = getHome();

  @Test
  public void shouldLoadPropertiesFile() throws URISyntaxException {
    ServerSettings settings = new ServerSettings(new PropertyDefinitions(), new BaseConfiguration(), new File("."), home);

    assertThat(settings.getString("hello"), is("world"));
  }

  @Test
  public void systemPropertiesShouldOverridePropertiesFile() throws URISyntaxException {
    System.setProperty("ServerSettingsTestEnv", "in_env");
    ServerSettings settings = new ServerSettings(new PropertyDefinitions(), new BaseConfiguration(), new File("."), home);

    assertThat(settings.getString("ServerSettingsTestEnv"), is("in_env"));
  }

  @Test(expected = IllegalStateException.class)
  public void shouldFailIfPropertiesFileNotFound() {
    File sonarHome = new File("unknown/path");
    new ServerSettings(new PropertyDefinitions(), new BaseConfiguration(), new File("."), sonarHome);
  }

  @Test
  public void shouldActivateDatabaseSettings() throws URISyntaxException {
    setupData("db/shared");

    ServerSettings settings = new ServerSettings(new PropertyDefinitions(), new BaseConfiguration(), new File("."), home);
    settings.activateDatabaseSettings(new PropertiesDao(getMyBatis()));
    settings.load(home);

    assertThat(settings.getString("global_only"), is("is_global"));
    assertThat(settings.getString("global_and_project"), is("is_global"));
    assertThat(settings.getString("project_only"), nullValue());
  }

  private static File getHome() {
    try {
      return new File(ServerSettingsTest.class.getResource("/org/sonar/server/platform/ServerSettingsTest/").toURI());
    } catch (URISyntaxException e) {
      throw new IllegalStateException(e);
    }
  }
}
