/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.scanner.repository.FileData;
import org.sonar.scanner.repository.ProjectRepositories;
import org.sonar.scanner.repository.SingleProjectRepository;
import org.sonar.scanner.scm.ScmChangedFiles;
import org.sonar.scm.git.ChangedFile;

import static org.assertj.core.api.Assertions.assertThat;

public class StatusDetectionTest {
  private final ProjectRepositories projectRepositories = new SingleProjectRepository(createFileDataPerPathMap());

  @Test
  public void detect_status() {
    ScmChangedFiles changedFiles = new ScmChangedFiles(null);
    StatusDetection statusDetection = new StatusDetection(projectRepositories, changedFiles);

    assertThat(statusDetection.status("foo", createFile("src/Foo.java"), "ABCDE")).isEqualTo(InputFile.Status.SAME);
    assertThat(statusDetection.status("foo", createFile("src/Foo.java"), "XXXXX")).isEqualTo(InputFile.Status.CHANGED);
    assertThat(statusDetection.status("foo", createFile("src/Other.java"), "QWERT")).isEqualTo(InputFile.Status.ADDED);
  }

  @Test
  public void detect_status_branches_exclude() {
    ScmChangedFiles changedFiles = new ScmChangedFiles(Set.of());
    StatusDetection statusDetection = new StatusDetection(projectRepositories, changedFiles);

    // normally changed
    assertThat(statusDetection.status("foo", createFile("src/Foo.java"), "XXXXX")).isEqualTo(InputFile.Status.SAME);

    // normally added
    assertThat(statusDetection.status("foo", createFile("src/Other.java"), "QWERT")).isEqualTo(InputFile.Status.SAME);
  }

  @Test
  public void detect_status_branches_confirm() {
    Path filePath = Paths.get("module", "src", "Foo.java");
    ScmChangedFiles changedFiles = new ScmChangedFiles(Set.of(ChangedFile.of(filePath)));

    StatusDetection statusDetection = new StatusDetection(projectRepositories, changedFiles);
    assertThat(statusDetection.status("foo", createFile("src/Foo.java"), "XXXXX")).isEqualTo(InputFile.Status.CHANGED);
  }

  private static Map<String, FileData> createFileDataPerPathMap() {
    Map<String, FileData> t = new HashMap<>();

    t.put("src/Foo.java", new FileData("ABCDE", "12345789"));
    t.put("src/Bar.java", new FileData("FGHIJ", "123456789"));

    return t;
  }

  private static DefaultInputFile createFile(String relativePath) {
    return new TestInputFileBuilder("module", relativePath).build();
  }
}
