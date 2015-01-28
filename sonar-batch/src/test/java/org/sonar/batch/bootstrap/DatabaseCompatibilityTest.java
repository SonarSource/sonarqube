/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.batch.bootstrap;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.database.DatabaseProperties;
import org.sonar.api.utils.MessageException;
import org.sonar.core.persistence.DatabaseVersion;
import org.sonar.core.properties.PropertiesDao;
import org.sonar.core.properties.PropertyDto;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DatabaseCompatibilityTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  DatabaseVersion databaseVersion;
  ServerClient server;
  Settings settings;
  PropertiesDao propertiesDao;

  private DefaultAnalysisMode mode;

  @Before
  public void init() {
    server = mock(ServerClient.class);
    when(server.getURL()).thenReturn("http://localhost:9000");
    when(server.request("/api/server")).thenReturn("{\"id\":\"123456\",\"version\":\"3.1\",\"status\":\"UP\"}");

    settings = new Settings();
    settings.setProperty(DatabaseProperties.PROP_URL, "jdbc:postgresql://localhost/foo");
    settings.setProperty(DatabaseProperties.PROP_USER, "bar");

    propertiesDao = mock(PropertiesDao.class);
    when(propertiesDao.selectGlobalProperty(CoreProperties.SERVER_ID)).thenReturn(new PropertyDto().setValue("123456"));

    mode = mock(DefaultAnalysisMode.class);

    databaseVersion = mock(DatabaseVersion.class);
  }

  @Test
  public void shouldFailIfRequiresDowngrade() {
    when(databaseVersion.getStatus()).thenReturn(DatabaseVersion.Status.REQUIRES_DOWNGRADE);

    thrown.expect(MessageException.class);
    thrown.expectMessage("Database relates to a more recent version of SonarQube. Please check your settings (JDBC settings, version of Maven plugin)");

    new DatabaseCompatibility(databaseVersion, server, settings, propertiesDao, mode).start();
  }

  @Test
  public void shouldFailIfRequiresUpgrade() {
    when(databaseVersion.getStatus()).thenReturn(DatabaseVersion.Status.REQUIRES_UPGRADE);

    thrown.expect(MessageException.class);
    thrown.expectMessage("Database must be upgraded.");

    new DatabaseCompatibility(databaseVersion, server, settings, propertiesDao, mode).start();
  }

  @Test
  public void shouldFailIfNotSameServerId() throws Exception {
    when(propertiesDao.selectGlobalProperty(CoreProperties.SERVER_ID)).thenReturn(new PropertyDto().setValue("11111111"));

    thrown.expect(MessageException.class);
    thrown.expectMessage("The current batch process and the configured remote server do not share the same DB configuration.");
    thrown.expectMessage("- Batch side: jdbc:postgresql://localhost/foo (bar / *****)");
    thrown.expectMessage("- Server side: check the configuration at http://localhost:9000/system");

    new DatabaseCompatibility(databaseVersion, server, settings, propertiesDao, mode).start();
  }

  @Test
  public void shouldUseDefaultUserNameWhenFaillingIfNotSameServerIdAndNoUserNameFound() throws Exception {
    when(propertiesDao.selectGlobalProperty(CoreProperties.SERVER_ID)).thenReturn(new PropertyDto().setValue("11111111"));

    settings.removeProperty(DatabaseProperties.PROP_USER);

    thrown.expect(MessageException.class);
    thrown.expectMessage("- Batch side: jdbc:postgresql://localhost/foo (sonar / *****)");

    new DatabaseCompatibility(databaseVersion, server, settings, propertiesDao, mode).start();
  }

  @Test
  public void shouldFailIfCantGetServerId() throws Exception {
    when(server.request("/api/server")).thenThrow(new IllegalStateException());

    thrown.expect(IllegalStateException.class);

    new DatabaseCompatibility(mock(DatabaseVersion.class), server, settings, propertiesDao, mode).start();
  }

  @Test
  public void shouldDoNothingIfUpToDate() {
    when(databaseVersion.getStatus()).thenReturn(DatabaseVersion.Status.UP_TO_DATE);
    new DatabaseCompatibility(databaseVersion, server, settings, propertiesDao, mode).start();
    // no error
  }

  @Test
  public void should_not_verify_compatibility_if_preview() {
    settings.setProperty(CoreProperties.SERVER_ID, "11111111");
    when(mode.isPreview()).thenReturn(true);

    new DatabaseCompatibility(databaseVersion, server, settings, propertiesDao, mode).start();

    // no failure
  }
}
