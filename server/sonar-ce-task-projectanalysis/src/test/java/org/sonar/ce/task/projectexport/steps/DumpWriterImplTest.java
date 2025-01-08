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
package org.sonar.ce.task.projectexport.steps;

import com.google.common.collect.Iterables;
import com.sonarsource.governance.projectdump.protobuf.ProjectDump;
import java.io.File;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.event.Level;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.api.utils.TempFolder;
import org.sonar.ce.task.projectexport.taskprocessor.ProjectDescriptor;
import org.sonar.ce.task.projectexport.util.ProjectExportDumpFS;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.ce.task.projectexport.steps.DumpElement.COMPONENTS;

public class DumpWriterImplTest {


  @Rule
  public TemporaryFolder junitTemp = new TemporaryFolder();

  @Rule
  public LogTester logTester = new LogTester();

  MutableDumpReaderImpl dumpReader = new MutableDumpReaderImpl();
  ProjectExportDumpFS projectexportDumpFS = mock(ProjectExportDumpFS.class);
  TempFolder temp = mock(TempFolder.class);
  ProjectDescriptor descriptor = mock(ProjectDescriptor.class);
  File rootDir;
  File zipFile;
  File targetZipFile;
  DumpWriter underTest;

  @Before
  public void setUp() throws Exception {
    rootDir = junitTemp.newFolder();
    dumpReader.setTempRootDir(rootDir);
    zipFile = junitTemp.newFile();
    targetZipFile = junitTemp.newFile();
    when(temp.newDir()).thenReturn(rootDir);
    when(temp.newFile()).thenReturn(zipFile);
    when(projectexportDumpFS.exportDumpOf(descriptor)).thenReturn(targetZipFile);
    underTest = new DumpWriterImpl(descriptor, projectexportDumpFS, temp);
  }

  @Test
  public void writeMetadata_writes_to_file() {
    underTest.write(newMetadata());

    assertThat(dumpReader.metadata().getProjectKey()).isEqualTo("foo");
  }

  @Test
  public void writeMetadata_fails_if_called_twice() {
    underTest.write(newMetadata());

    assertThatThrownBy(() -> underTest.write(newMetadata()))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Metadata has already been written");
  }

  @Test
  public void publish_zips_directory_then_deletes_the_temp_directory() {
    underTest.write(newMetadata());
    underTest.publish();

    assertThat(rootDir).doesNotExist();
    assertThat(targetZipFile).isFile().exists();
    assertThat(logTester.logs(Level.INFO).get(0))
      .contains("Dump file published", "size=", "path=" + targetZipFile.getAbsolutePath());
  }

  @Test
  public void publish_fails_if_called_twice() {
    underTest.write(newMetadata());
    underTest.publish();

    assertThatThrownBy(() -> underTest.publish())
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Dump is already published");
  }

  @Test
  public void publish_fails_if_metadata_is_missing() {
    assertThatThrownBy(() -> underTest.publish())
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Metadata is missing");
  }

  @Test
  public void ensure_written_fields_can_be_read() {
    try (StreamWriter<ProjectDump.Component> writer = underTest.newStreamWriter(COMPONENTS)) {
      writer.write(ProjectDump.Component.newBuilder()
        .setKey("abc")
        .setScope("FIL")
        .setQualifier("FIL")
        .setLanguage("java")
        .build());
    }
    try (MessageStream<ProjectDump.Component> reader = dumpReader.stream(COMPONENTS)) {
      ProjectDump.Component component = Iterables.getOnlyElement(reader);
      assertThat(component.getKey()).isEqualTo("abc");
      assertThat(component.getScope()).isEqualTo("FIL");
      assertThat(component.getQualifier()).isEqualTo("FIL");
      assertThat(component.getLanguage()).isEqualTo("java");
    }
  }

  @Test
  public void create_empty_file_if_stream_is_empty() {
    try (StreamWriter<ProjectDump.Component> writer = underTest.newStreamWriter(COMPONENTS)) {
      // no components
    }
    assertThat(new File(rootDir, COMPONENTS.filename())).isFile().exists();
    try (MessageStream<ProjectDump.Component> reader = dumpReader.stream(COMPONENTS)) {
      assertThat(reader).isEmpty();
    }
  }

  private static ProjectDump.Metadata newMetadata() {
    return ProjectDump.Metadata.newBuilder()
      .setProjectKey("foo")
      .build();
  }
}
