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
package org.sonar.scanner.protocol.output;

import java.io.File;
import java.nio.charset.Charset;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileStructureTest {

  @TempDir
  public File temp;

  @Test
  void fail_if_dir_does_not_exist() {
    File dir = temp;
    FileUtils.deleteQuietly(dir);

    assertThatThrownBy(() -> new FileStructure(dir))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Directory of analysis report does not exist");
  }

  @Test
  void fail_if_invalid_dir() {
    // not a dir but a file
    File dir = new File(temp, "newFile");

    assertThatThrownBy(() -> new FileStructure(dir))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Directory of analysis report does not exist");
  }

  @Test
  void locate_files() throws Exception {
    File dir = temp;
    FileUtils.write(new File(dir, "metadata.pb"), "metadata content", Charset.defaultCharset());
    FileUtils.write(new File(dir, "issues-3.pb"), "external issues of component 3", Charset.defaultCharset());
    FileUtils.write(new File(dir, "external-issues-3.pb"), "issues of component 3", Charset.defaultCharset());
    FileUtils.write(new File(dir, "component-42.pb"), "details of component 42", Charset.defaultCharset());

    FileStructure structure = new FileStructure(dir);
    assertThat(structure.metadataFile()).exists().isFile();
    assertThat(structure.fileFor(FileStructure.Domain.COMPONENT, 42)).exists().isFile();
    assertThat(structure.fileFor(FileStructure.Domain.ISSUES, 3)).exists().isFile();
    assertThat(structure.fileFor(FileStructure.Domain.ISSUES, 42)).doesNotExist();
    assertThat(structure.fileFor(FileStructure.Domain.EXTERNAL_ISSUES, 3)).exists().isFile();
    assertThat(structure.fileFor(FileStructure.Domain.EXTERNAL_ISSUES, 42)).doesNotExist();
  }

  @Test
  void contextProperties_file() throws Exception {
    File dir = temp;
    File file = new File(dir, "context-props.pb");
    FileUtils.write(file, "content", Charset.defaultCharset());

    FileStructure structure = new FileStructure(dir);
    assertThat(structure.contextProperties()).exists().isFile().isEqualTo(file);
  }

  @Test
  void telemetryFile_hasTheCorrectName() throws Exception {
    File dir = temp;
    File file = new File(dir, "telemetry-entries.pb");
    FileUtils.write(file, "content", Charset.defaultCharset());

    FileStructure structure = new FileStructure(dir);
    assertThat(structure.telemetryEntries()).exists().isFile().isEqualTo(file);
  }

  @Test
  void scaDir_shouldExist() {
    File sca = new File(temp, "sca");

    FileStructure structure = new FileStructure(temp);
    assertThat(structure.scaDir()).exists().isDirectory().isEqualTo(sca);
  }
}
