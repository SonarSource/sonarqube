/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.scanner.scan.filesystem;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import java.nio.file.Paths;
import java.util.Collections;
import org.junit.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.scanner.repository.FileData;
import org.sonar.scanner.repository.ProjectRepositories;
import org.sonar.scanner.scm.ScmChangedFiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class StatusDetectionTest {
  @Test
  public void detect_status() {
    ProjectRepositories ref = new ProjectRepositories(ImmutableTable.of(), createTable(), null);
    ScmChangedFiles changedFiles = new ScmChangedFiles(null);
    StatusDetection statusDetection = new StatusDetection(ref, changedFiles);

    assertThat(statusDetection.status("foo", createFile("src/Foo.java"), "ABCDE")).isEqualTo(InputFile.Status.SAME);
    assertThat(statusDetection.status("foo", createFile("src/Foo.java"), "XXXXX")).isEqualTo(InputFile.Status.CHANGED);
    assertThat(statusDetection.status("foo", createFile("src/Other.java"), "QWERT")).isEqualTo(InputFile.Status.ADDED);
  }

  @Test
  public void detect_status_branches_exclude() {
    ProjectRepositories ref = new ProjectRepositories(ImmutableTable.of(), createTable(), null);
    ScmChangedFiles changedFiles = new ScmChangedFiles(Collections.emptyList());
    StatusDetection statusDetection = new StatusDetection(ref, changedFiles);

    // normally changed
    assertThat(statusDetection.status("foo", createFile("src/Foo.java"), "XXXXX")).isEqualTo(InputFile.Status.SAME);

    // normally added
    assertThat(statusDetection.status("foo", createFile("src/Other.java"), "QWERT")).isEqualTo(InputFile.Status.SAME);
  }

  @Test
  public void detect_status_without_metadata() {
    DefaultInputFile mockedFile = mock(DefaultInputFile.class);
    when(mockedFile.relativePath()).thenReturn("module/src/Foo.java");
    when(mockedFile.path()).thenReturn(Paths.get("module", "src", "Foo.java"));

    ProjectRepositories ref = new ProjectRepositories(ImmutableTable.of(), createTable(), null);
    ScmChangedFiles changedFiles = new ScmChangedFiles(Collections.singletonList(Paths.get("module", "src", "Foo.java")));
    StatusDetection statusDetection = new StatusDetection(ref, changedFiles);

    assertThat(statusDetection.getStatusWithoutMetadata("foo", mockedFile)).isEqualTo(InputFile.Status.ADDED);

    verify(mockedFile).path();
    verify(mockedFile).relativePath();
    verifyNoMoreInteractions(mockedFile);
  }

  @Test
  public void detect_status_branches_confirm() {
    ProjectRepositories ref = new ProjectRepositories(ImmutableTable.of(), createTable(), null);
    ScmChangedFiles changedFiles = new ScmChangedFiles(Collections.singletonList(Paths.get("module", "src", "Foo.java")));
    StatusDetection statusDetection = new StatusDetection(ref, changedFiles);

    assertThat(statusDetection.status("foo", createFile("src/Foo.java"), "XXXXX")).isEqualTo(InputFile.Status.CHANGED);
  }

  private static Table<String, String, FileData> createTable() {
    Table<String, String, FileData> t = HashBasedTable.create();

    t.put("foo", "src/Foo.java", new FileData("ABCDE", "12345789"));
    t.put("foo", "src/Bar.java", new FileData("FGHIJ", "123456789"));

    return t;
  }

  private static DefaultInputFile createFile(String relativePath) {
    return new TestInputFileBuilder("module", relativePath).build();
  }
}
