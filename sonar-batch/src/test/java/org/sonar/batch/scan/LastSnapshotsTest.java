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
package org.sonar.batch.scan;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.database.model.RuleFailureModel;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.HttpDownloader;
import org.sonar.batch.bootstrap.ServerClient;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class LastSnapshotsTest extends AbstractDbUnitTestCase {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void should_return_null_violations_if_no_last_snapshot() {
    setupData("no_last_snapshot");
    ServerClient server = mock(ServerClient.class);

    LastSnapshots lastSnapshots = new LastSnapshots(new Settings(), getSession(), server);

    assertThat(lastSnapshots.getViolations(new File("org/foo", "Bar.c"))).isNull();
    verifyZeroInteractions(server);
  }

  @Test
  public void should_get_violations_of_last_snapshot() {
    setupData("last_snapshot");
    ServerClient server = mock(ServerClient.class);

    LastSnapshots lastSnapshots = new LastSnapshots(new Settings(), getSession(), server);

    List<RuleFailureModel> violations = lastSnapshots.getViolations(newFile());
    assertThat(violations).hasSize(1);
    assertThat(violations.get(0).getChecksum()).isEqualTo("ABCDE");
    verifyZeroInteractions(server);
  }

  @Test
  public void should_get_source_of_last_snapshot() {
    setupData("last_snapshot");
    ServerClient server = mock(ServerClient.class);

    LastSnapshots lastSnapshots = new LastSnapshots(new Settings(), getSession(), server);

    assertThat(lastSnapshots.getSource(newFile())).isEqualTo("this is bar");
    verifyZeroInteractions(server);
  }

  @Test
  public void should_return_empty_source_if_no_last_snapshot() {
    setupData("no_last_snapshot");
    ServerClient server = mock(ServerClient.class);

    LastSnapshots lastSnapshots = new LastSnapshots(new Settings(), getSession(), server);

    assertThat(lastSnapshots.getSource(newFile())).isEqualTo("");
    verifyZeroInteractions(server);
  }

  @Test
  public void should_download_source_from_ws_if_dry_run() {
    setupData("last_snapshot");
    ServerClient server = mock(ServerClient.class);
    when(server.request(anyString(), eq(false))).thenReturn("downloaded source of Bar.c");

    Settings settings = new Settings();
    settings.setProperty(CoreProperties.DRY_RUN, true);
    LastSnapshots lastSnapshots = new LastSnapshots(settings, getSession(), server);

    String source = lastSnapshots.getSource(newFile());
    assertThat(source).isEqualTo("downloaded source of Bar.c");
    verify(server).request("/api/sources?resource=myproject:org/foo/Bar.c&format=txt", false);
  }

  @Test
  public void should_fail_to_download_source_from_ws() throws URISyntaxException {
    setupData("last_snapshot");
    ServerClient server = mock(ServerClient.class);
    when(server.request(anyString(), eq(false))).thenThrow(new HttpDownloader.HttpException(new URI(""), 500));

    Settings settings = new Settings();
    settings.setProperty(CoreProperties.DRY_RUN, true);
    LastSnapshots lastSnapshots = new LastSnapshots(settings, getSession(), server);

    thrown.expect(HttpDownloader.HttpException.class);
    lastSnapshots.getSource(newFile());
  }

  @Test
  public void should_return_empty_source_if_dry_run_and_no_last_snapshot() throws URISyntaxException {
    setupData("last_snapshot");
    ServerClient server = mock(ServerClient.class);
    when(server.request(anyString(), eq(false))).thenThrow(new HttpDownloader.HttpException(new URI(""), 404));

    Settings settings = new Settings();
    settings.setProperty(CoreProperties.DRY_RUN, true);
    LastSnapshots lastSnapshots = new LastSnapshots(settings, getSession(), server);

    String source = lastSnapshots.getSource(newFile());
    assertThat(source).isEqualTo("");
    verify(server).request("/api/sources?resource=myproject:org/foo/Bar.c&format=txt", false);
  }

  @Test
  public void should_not_load_source_of_non_files() throws URISyntaxException {
    setupData("last_snapshot");
    ServerClient server = mock(ServerClient.class);

    LastSnapshots lastSnapshots = new LastSnapshots(new Settings(), getSession(), server);

    String source = lastSnapshots.getSource(new Project("my-project"));
    assertThat(source).isEqualTo("");
  }

  private File newFile() {
    File file = new File("org/foo", "Bar.c");
    file.setEffectiveKey("myproject:org/foo/Bar.c");
    return file;
  }
}
