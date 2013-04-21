/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.batch.bootstrap;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.database.DatabaseProperties;
import org.sonar.core.persistence.BadDatabaseVersion;
import org.sonar.core.persistence.DatabaseVersion;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DatabaseCompatibilityTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  DatabaseVersion databaseVersion;
  ServerMetadata server;
  Settings settings;

  @Before
  public void init() {
    server = mock(ServerMetadata.class);
    when(server.getURL()).thenReturn("http://localhost:9000");
    when(server.getServerId()).thenReturn("123456");

    settings = new Settings();
    settings.setProperty(DatabaseProperties.PROP_URL, "jdbc:postgresql://localhost/foo");
    settings.setProperty(DatabaseProperties.PROP_USER, "bar");
    settings.setProperty(CoreProperties.SERVER_ID, "123456");
    settings.setProperty(CoreProperties.DRY_RUN, false);

    databaseVersion = mock(DatabaseVersion.class);
  }

  @Test
  public void shouldFailIfRequiresDowngrade() {
    when(databaseVersion.getStatus()).thenReturn(DatabaseVersion.Status.REQUIRES_DOWNGRADE);

    thrown.expect(BadDatabaseVersion.class);
    thrown.expectMessage("Database relates to a more recent version of Sonar. Please check your settings (JDBC settings, version of Maven plugin)");

    new DatabaseCompatibility(databaseVersion, server, settings).start();
  }

  @Test
  public void shouldFailIfRequiresUpgrade() {
    when(databaseVersion.getStatus()).thenReturn(DatabaseVersion.Status.REQUIRES_UPGRADE);

    thrown.expect(BadDatabaseVersion.class);
    thrown.expectMessage("Database must be upgraded.");

    new DatabaseCompatibility(databaseVersion, server, settings).start();
  }

  @Test
  public void shouldFailIfNotSameServerId() throws Exception {
    settings.setProperty(CoreProperties.SERVER_ID, "11111111");

    thrown.expect(BadDatabaseVersion.class);
    thrown.expectMessage("The current batch process and the configured remote server do not share the same DB configuration.");
    thrown.expectMessage("- Batch side: jdbc:postgresql://localhost/foo (bar / *****)");
    thrown.expectMessage("- Server side: check the configuration at http://localhost:9000/system");

    new DatabaseCompatibility(databaseVersion, server, settings).start();
  }

  @Test
  public void shouldUseDefaultUserNameWhenFaillingIfNotSameServerIdAndNoUserNameFound() throws Exception {
    settings.setProperty(CoreProperties.SERVER_ID, "11111111");

    settings.removeProperty(DatabaseProperties.PROP_USER);

    thrown.expect(BadDatabaseVersion.class);
    thrown.expectMessage("- Batch side: jdbc:postgresql://localhost/foo (sonar / *****)");

    new DatabaseCompatibility(databaseVersion, server, settings).start();
  }

  @Test
  public void shouldFailIfCantGetServerId() throws Exception {
    when(server.getServerId()).thenThrow(new IllegalStateException());

    thrown.expect(IllegalStateException.class);

    new DatabaseCompatibility(mock(DatabaseVersion.class), server, settings).start();
  }

  @Test
  public void shouldDoNothingIfUpToDate() {
    when(databaseVersion.getStatus()).thenReturn(DatabaseVersion.Status.UP_TO_DATE);
    new DatabaseCompatibility(databaseVersion, server, settings).start();
    // no error
  }

  @Test
  public void should_not_verify_compatibility_if_dry_run() {
    settings.setProperty(CoreProperties.SERVER_ID, "11111111");
    settings.setProperty(CoreProperties.DRY_RUN, true);

    new DatabaseCompatibility(databaseVersion, server, settings).start();

    // no failure
  }
}
