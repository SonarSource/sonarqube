/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.api.utils.TempFolder;

public class ModuleFileSystemInitializerTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  PathResolver pathResolver = new PathResolver();

  @Test
  public void test_default_directories() throws Exception {
    File baseDir = temp.newFolder("base");
    File workDir = temp.newFolder("work");
    ProjectDefinition module = ProjectDefinition.create().setBaseDir(baseDir).setWorkDir(workDir);

    ModuleFileSystemInitializer initializer = new ModuleFileSystemInitializer(module, mock(TempFolder.class), pathResolver);

    assertThat(initializer.baseDir().getCanonicalPath()).isEqualTo(baseDir.getCanonicalPath());
    assertThat(initializer.workingDir().getCanonicalPath()).isEqualTo(workDir.getCanonicalPath());
    assertThat(initializer.sources()).isEmpty();
    assertThat(initializer.tests()).isEmpty();
  }

  @Test
  public void should_init_directories() throws IOException {
    File baseDir = temp.newFolder("base");
    File sourceDir = new File(baseDir, "src/main/java");
    FileUtils.forceMkdir(sourceDir);
    File testDir = new File(baseDir, "src/test/java");
    FileUtils.forceMkdir(testDir);
    File binaryDir = new File(baseDir, "target/classes");
    FileUtils.forceMkdir(binaryDir);

    ProjectDefinition project = ProjectDefinition.create()
      .setBaseDir(baseDir)
      .addSources("src/main/java", "src/main/unknown")
      .addTests("src/test/java", "src/test/unknown");

    ModuleFileSystemInitializer initializer = new ModuleFileSystemInitializer(project, mock(TempFolder.class), pathResolver);

    assertThat(initializer.baseDir().getCanonicalPath()).isEqualTo(baseDir.getCanonicalPath());
    assertThat(initializer.sources()).hasSize(1);
    assertThat(path(initializer.sources().get(0))).endsWith("src/main/java");
    assertThat(initializer.tests()).hasSize(1);
    assertThat(path(initializer.tests().get(0))).endsWith("src/test/java");
  }

  @Test
  public void supportFilenamesWithComma() throws IOException {
    File baseDir = temp.newFolder("base");
    File sourceFile = new File(baseDir, "my,File.cs");
    sourceFile.createNewFile();
    File testFile = new File(baseDir, "my,TestFile.cs");
    testFile.createNewFile();

    ProjectDefinition project = ProjectDefinition.create()
      .setBaseDir(baseDir)
      .addSources("\"my,File.cs\"")
      .addTests("\"my,TestFile.cs\"");

    ModuleFileSystemInitializer initializer = new ModuleFileSystemInitializer(project, mock(TempFolder.class), pathResolver);

    assertThat(initializer.baseDir().getCanonicalPath()).isEqualTo(baseDir.getCanonicalPath());
    assertThat(initializer.sources()).hasSize(1);
    assertThat(initializer.sources().get(0)).isEqualTo(sourceFile);
    assertThat(initializer.tests()).hasSize(1);
    assertThat(initializer.tests().get(0)).isEqualTo(testFile);
  }

  private String path(File f) throws IOException {
    return FilenameUtils.separatorsToUnix(f.getCanonicalPath());
  }

}
