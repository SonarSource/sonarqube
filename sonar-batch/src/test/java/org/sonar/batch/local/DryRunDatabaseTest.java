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
package org.sonar.batch.local;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.api.config.Settings;
import org.sonar.api.database.DatabaseProperties;
import org.sonar.api.utils.SonarException;
import org.sonar.batch.bootstrap.DryRun;
import org.sonar.batch.bootstrap.ProjectReactorReady;
import org.sonar.batch.bootstrap.ServerClient;
import org.sonar.batch.bootstrap.TempDirectories;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class DryRunDatabaseTest {
  DryRunDatabase dryRunDatabase;

  DryRun dryRun = mock(DryRun.class);
  Settings settings = new Settings();
  ServerClient server = mock(ServerClient.class);
  TempDirectories tempDirectories = mock(TempDirectories.class);
  ProjectReactor projectReactor = new ProjectReactor(ProjectDefinition.create().setKey("group:project"));

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Before
  public void setUp() {
    dryRunDatabase = new DryRunDatabase(dryRun, settings, server, tempDirectories, projectReactor, mock(ProjectReactorReady.class));
  }

  @Test
  public void should_disable_if_no_dry_run() {
    dryRunDatabase.start();

    verifyZeroInteractions(tempDirectories, server);
  }

  @Test
  public void should_download_database() {
    File databaseFile = new File("/tmp/dry_run/db.h2.db");
    when(dryRun.isEnabled()).thenReturn(true);
    when(tempDirectories.getFile("dry_run", "db.h2.db")).thenReturn(databaseFile);

    dryRunDatabase.start();

    verify(server).download("/batch_bootstrap/db?project=group:project", databaseFile);
  }

  @Test
  public void should_replace_database_settings() {
    when(dryRun.isEnabled()).thenReturn(true);
    when(tempDirectories.getFile("dry_run", "db.h2.db")).thenReturn(new File("/tmp/dry_run/db.h2.db"));

    dryRunDatabase.start();

    assertThat(settings.getString(DatabaseProperties.PROP_DIALECT)).isEqualTo("h2");
    assertThat(settings.getString(DatabaseProperties.PROP_DRIVER)).isEqualTo("org.h2.Driver");
    assertThat(settings.getString(DatabaseProperties.PROP_USER)).isEqualTo("sonar");
    assertThat(settings.getString(DatabaseProperties.PROP_PASSWORD)).isEqualTo("sonar");
    assertThat(settings.getString(DatabaseProperties.PROP_URL)).isEqualTo("jdbc:h2:/tmp/dry_run/db");
  }

  @Test
  public void should_fail_on_unknown_project() {
    when(dryRun.isEnabled()).thenReturn(true);
    when(tempDirectories.getFile("dry_run", "db.h2.db")).thenReturn(new File("/tmp/dry_run/db.h2.db"));
    doThrow(new SonarException(new FileNotFoundException())).when(server).download("/batch_bootstrap/db?project=group:project", new File("/tmp/dry_run/db.h2.db"));

    thrown.expect(SonarException.class);
    thrown.expectMessage("Project [group:project] doesn't exist on server");

    dryRunDatabase.start();
  }

  @Test
  public void should_fail_on_invalid_role() {
    when(dryRun.isEnabled()).thenReturn(true);
    when(tempDirectories.getFile("dry_run", "db.h2.db")).thenReturn(new File("/tmp/dry_run/db.h2.db"));
    doThrow(new SonarException(new IOException("HTTP 401"))).when(server).download("/batch_bootstrap/db?project=group:project", new File("/tmp/dry_run/db.h2.db"));

    thrown.expect(SonarException.class);
    thrown.expectMessage("You don't have access rights to project [group:project]");

    dryRunDatabase.start();
  }

  @Test
  public void should_fail() {
    when(dryRun.isEnabled()).thenReturn(true);
    when(tempDirectories.getFile("dry_run", "db.h2.db")).thenReturn(new File("/tmp/dry_run/db.h2.db"));
    doThrow(new SonarException("BUG")).when(server).download("/batch_bootstrap/db?project=group:project", new File("/tmp/dry_run/db.h2.db"));

    thrown.expect(SonarException.class);
    thrown.expectMessage("BUG");

    dryRunDatabase.start();
  }
}
