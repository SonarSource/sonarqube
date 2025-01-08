/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.ce.task.projectexport.util;

import java.io.File;
import java.io.IOException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.ce.task.projectexport.taskprocessor.ProjectDescriptor;

import static org.assertj.core.api.Assertions.assertThat;

public class ProjectExportDumpFSImplTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private final MapSettings settings = new MapSettings();
  private final ProjectDescriptor projectDescriptor = new ProjectDescriptor("uuid", "project_key", "name");
  private final ProjectDescriptor descriptorWithUglyKey = new ProjectDescriptor("uuid", "  so:me à9ç& Key ", "name");

  private File dataDir;
  private ProjectExportDumpFSImpl underTest;

  @Before
  public void setUp() throws Exception {
    this.dataDir = temp.newFolder();
    settings.setProperty("sonar.path.data", dataDir.getAbsolutePath());
    this.underTest = new ProjectExportDumpFSImpl(settings.asConfig());
  }

  @Test
  public void start_creates_import_and_export_directories_including_missing_parents() throws IOException {
    dataDir = new File(temp.newFolder(), "data");
    File importDir = new File(dataDir, "governance/project_dumps/import");
    File exportDir = new File(dataDir, "governance/project_dumps/export");

    settings.setProperty("sonar.path.data", dataDir.getAbsolutePath());
    this.underTest = new ProjectExportDumpFSImpl(settings.asConfig());

    assertThat(dataDir).doesNotExist();
    assertThat(importDir).doesNotExist();
    assertThat(exportDir).doesNotExist();

    underTest.start();

    assertThat(dataDir).exists().isDirectory();
    assertThat(exportDir).exists().isDirectory();
  }

  @Test
  public void exportDumpOf_is_located_in_governance_project_dump_out() {
    assertThat(underTest.exportDumpOf(projectDescriptor)).isEqualTo(new File(dataDir, "governance/project_dumps/export/project_key.zip"));
  }

  @Test
  public void exportDumpOf_slugifies_project_key() {
    assertThat(underTest.exportDumpOf(descriptorWithUglyKey))
      .isEqualTo(new File(dataDir, "governance/project_dumps/export/so-me-a9c-key.zip"));
  }

}
