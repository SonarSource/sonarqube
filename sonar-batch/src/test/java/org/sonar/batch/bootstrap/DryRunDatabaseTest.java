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
package org.sonar.batch.bootstrap;

import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.api.config.Settings;
import org.sonar.api.database.DatabaseProperties;
import org.sonar.api.utils.HttpDownloader;
import org.sonar.api.utils.SonarException;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class DryRunDatabaseTest {
  Settings settings = new Settings();
  ServerClient server = mock(ServerClient.class);
  TempDirectories tempDirectories = mock(TempDirectories.class);
  ProjectReactor projectReactor = new ProjectReactor(ProjectDefinition.create().setKey("group:project"));
  File databaseFile;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Before
  public void setUp() throws Exception {
    databaseFile = temp.newFile("dryrun.h2.db");
    when(tempDirectories.getFile("", "dryrun.h2.db")).thenReturn(databaseFile);
    settings.setProperty(CoreProperties.DRY_RUN, true);
  }

  @Test
  public void should_be_disabled_if_not_dry_run() {
    settings.setProperty(CoreProperties.DRY_RUN, false);
    new DryRunDatabase(settings, server, tempDirectories, projectReactor).start();

    verifyZeroInteractions(tempDirectories, server);
  }

  @Test
  public void should_download_database() {
    new DryRunDatabase(settings, server, tempDirectories, projectReactor).start();

    verify(server).download("/batch_bootstrap/db?project=group:project", databaseFile);
  }

  @Test
  public void should_replace_database_settings() {
    new DryRunDatabase(settings, server, tempDirectories, projectReactor).start();

    assertThat(settings.getString(DatabaseProperties.PROP_DIALECT)).isEqualTo("h2");
    assertThat(settings.getString(DatabaseProperties.PROP_DRIVER)).isEqualTo("org.h2.Driver");
    assertThat(settings.getString(DatabaseProperties.PROP_USER)).isEqualTo("sonar");
    assertThat(settings.getString(DatabaseProperties.PROP_PASSWORD)).isEqualTo("sonar");
    assertThat(settings.getString(DatabaseProperties.PROP_URL)).isEqualTo("jdbc:h2:" + StringUtils.removeEnd(databaseFile.getAbsolutePath(), ".h2.db"));
  }

  @Test
  public void should_fail_on_invalid_role() {
    doThrow(new SonarException(new HttpDownloader.HttpException(null, 401))).when(server).download("/batch_bootstrap/db?project=group:project", databaseFile);

    thrown.expect(SonarException.class);
    thrown.expectMessage("You don't have access rights to project [group:project]");

    new DryRunDatabase(settings, server, tempDirectories, projectReactor).start();
  }

  @Test
  public void should_fail() {
    doThrow(new SonarException("BUG")).when(server).download("/batch_bootstrap/db?project=group:project", databaseFile);

    thrown.expect(SonarException.class);
    thrown.expectMessage("BUG");

    new DryRunDatabase(settings, server, tempDirectories, projectReactor).start();
  }

  @Test
  public void project_should_be_optional() {
    // on non-scan tasks
    new DryRunDatabase(settings, server, tempDirectories).start();
    verify(server).download("/batch_bootstrap/db", databaseFile);
  }
}
