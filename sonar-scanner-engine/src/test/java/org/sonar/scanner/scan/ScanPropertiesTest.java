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
package org.sonar.scanner.scan;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.internal.DefaultInputProject;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.utils.MessageException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ScanPropertiesTest {
  private MapSettings settings = new MapSettings();
  private DefaultInputProject project = mock(DefaultInputProject.class);
  private ScanProperties underTest = new ScanProperties(settings.asConfig(), project);

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Before
  public void setUp() throws IOException {
    when(project.getBaseDir()).thenReturn(temp.newFolder().toPath());
    when(project.getWorkDir()).thenReturn(temp.newFolder().toPath());
  }

  @Test
  public void defaults_if_no_setting_defined() {
    assertThat(underTest.branch()).isEmpty();
    assertThat(underTest.organizationKey()).isEmpty();
    assertThat(underTest.preloadFileMetadata()).isFalse();
    assertThat(underTest.shouldKeepReport()).isFalse();
    assertThat(underTest.metadataFilePath()).isEqualTo(project.getWorkDir().resolve("report-task.txt"));
    underTest.validate();
  }

  @Test
  public void should_define_organization_key() {
    settings.setProperty("sonar.organization", "org");
    assertThat(underTest.organizationKey()).isEqualTo(Optional.of("org"));
  }

  @Test
  public void should_define_branch_name() {
    settings.setProperty("sonar.branch.name", "name");
    assertThat(underTest.branch()).isEqualTo(Optional.of("name"));
  }

  @Test
  public void should_define_preload_file_metadata() {
    settings.setProperty("sonar.preloadFileMetadata", "true");
    assertThat(underTest.preloadFileMetadata()).isTrue();
  }

  @Test
  public void should_define_keep_report() {
    settings.setProperty("sonar.scanner.keepReport", "true");
    assertThat(underTest.shouldKeepReport()).isTrue();
  }

  @Test
  public void should_define_metadata_file_path() throws IOException {
    Path path = temp.newFolder().toPath().resolve("report");
    settings.setProperty("sonar.scanner.metadataFilePath", path.toString());
    assertThat(underTest.metadataFilePath()).isEqualTo(path);
  }

  @Test
  public void validate_fails_if_metadata_file_location_is_not_absolute() {
    settings.setProperty("sonar.scanner.metadataFilePath", "relative");

    exception.expect(MessageException.class);
    exception.expectMessage("Property 'sonar.scanner.metadataFilePath' must point to an absolute path: relative");
    underTest.validate();

  }
}
