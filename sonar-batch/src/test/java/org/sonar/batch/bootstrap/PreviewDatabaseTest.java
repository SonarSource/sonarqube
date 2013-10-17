/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.database.DatabaseProperties;
import org.sonar.api.utils.HttpDownloader;
import org.sonar.api.utils.SonarException;
import org.sonar.api.utils.TempFolder;

import java.io.File;
import java.net.SocketTimeoutException;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class PreviewDatabaseTest {
  Settings settings;
  ServerClient server = mock(ServerClient.class);
  TempFolder tempUtils = mock(TempFolder.class);
  File databaseFile;
  private AnalysisMode mode;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Before
  public void setUp() throws Exception {
    databaseFile = temp.newFile("preview.h2.db");
    when(tempUtils.newFile("preview", ".h2.db")).thenReturn(databaseFile);
    settings = new Settings();
    settings.setProperty(CoreProperties.PROJECT_KEY_PROPERTY, "group:project");

    mode = mock(AnalysisMode.class);
    when(mode.isPreview()).thenReturn(true);
  }

  @Test
  public void should_be_disabled_if_not_preview() {
    when(mode.isPreview()).thenReturn(false);
    new PreviewDatabase(settings, server, tempUtils, mode).start();

    verifyZeroInteractions(tempUtils, server);
  }

  @Test
  public void should_download_database() {
    new PreviewDatabase(settings, server, tempUtils, mode).start();

    verify(server).download("/batch_bootstrap/db?project=group:project", databaseFile, 60000);
  }

  @Test
  public void should_download_database_with_deprecated_overriden_timeout() {
    settings.setProperty(CoreProperties.DRY_RUN_READ_TIMEOUT_SEC, 80);
    new PreviewDatabase(settings, server, tempUtils, mode).start();

    verify(server).download("/batch_bootstrap/db?project=group:project", databaseFile, 80000);
  }

  @Test
  public void should_download_database_with_overriden_timeout() {
    settings.setProperty(CoreProperties.PREVIEW_READ_TIMEOUT_SEC, 80);
    new PreviewDatabase(settings, server, tempUtils, mode).start();

    verify(server).download("/batch_bootstrap/db?project=group:project", databaseFile, 80000);
  }

  @Test
  public void should_download_database_on_branch() {
    settings.setProperty(CoreProperties.PROJECT_BRANCH_PROPERTY, "mybranch");
    new PreviewDatabase(settings, server, tempUtils, mode).start();

    verify(server).download("/batch_bootstrap/db?project=group:project:mybranch", databaseFile, 60000);
  }

  @Test
  public void should_replace_database_settings() {
    new PreviewDatabase(settings, server, tempUtils, mode).start();

    assertThat(settings.getString(DatabaseProperties.PROP_DIALECT)).isEqualTo("h2");
    assertThat(settings.getString(DatabaseProperties.PROP_DRIVER)).isEqualTo("org.h2.Driver");
    assertThat(settings.getString(DatabaseProperties.PROP_USER)).isEqualTo("sonar");
    assertThat(settings.getString(DatabaseProperties.PROP_PASSWORD)).isEqualTo("sonar");
    assertThat(settings.getString(DatabaseProperties.PROP_URL)).isEqualTo("jdbc:h2:" + StringUtils.removeEnd(databaseFile.getAbsolutePath(), ".h2.db"));
  }

  @Test
  public void should_fail_on_invalid_role() {
    doThrow(new SonarException(new HttpDownloader.HttpException(null, 401))).when(server).download("/batch_bootstrap/db?project=group:project", databaseFile, 60000);

    thrown.expect(SonarException.class);
    thrown.expectMessage("You don't have access rights to project [group:project]");

    new PreviewDatabase(settings, server, tempUtils, mode).start();
  }

  @Test
  public void should_fail_on_read_timeout() {
    doThrow(new SonarException(new SocketTimeoutException())).when(server).download("/batch_bootstrap/db?project=group:project", databaseFile, 60000);

    thrown.expect(SonarException.class);
    thrown.expectMessage("Preview database read timed out after 60000 ms. You can try to increase read timeout with property -Dsonar.preview.readTimeout (in seconds)");

    new PreviewDatabase(settings, server, tempUtils, mode).start();
  }

  @Test
  public void should_fail() {
    doThrow(new SonarException("BUG")).when(server).download("/batch_bootstrap/db?project=group:project", databaseFile, 60000);

    thrown.expect(SonarException.class);
    thrown.expectMessage("BUG");

    new PreviewDatabase(settings, server, tempUtils, mode).start();
  }

  @Test
  public void project_should_be_optional() {
    // on non-scan tasks
    settings.removeProperty(CoreProperties.PROJECT_KEY_PROPERTY);
    new PreviewDatabase(settings, server, tempUtils, mode).start();
    verify(server).download("/batch_bootstrap/db", databaseFile, 60000);
  }
}
