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

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;

import static org.assertj.core.api.Assertions.assertThat;

public class ModuleFileSystemInitializerTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public LogTester logTester = new LogTester();

  @Test
  public void test_default_directories() throws Exception {
    File baseDir = temp.newFolder("base");
    File workDir = temp.newFolder("work");
    ProjectDefinition module = ProjectDefinition.create().setBaseDir(baseDir).setWorkDir(workDir);

    ModuleFileSystemInitializer initializer = new ModuleFileSystemInitializer(new DefaultInputModule(module));

    assertThat(logTester.logs(LoggerLevel.INFO)).contains("Base dir: " + baseDir.toPath().toAbsolutePath().toString());
    assertThat(logTester.logs(LoggerLevel.INFO)).contains("Working dir: " + workDir.toPath().toAbsolutePath().toString());
    assertThat(initializer.sources()).isEmpty();
    assertThat(initializer.tests()).isEmpty();
  }

  @Test
  public void should_init_directories() throws IOException {
    File baseDir = temp.newFolder("base");
    File workDir = temp.newFolder("work");
    File sourceDir = new File(baseDir, "src/main/java");
    FileUtils.forceMkdir(sourceDir);
    File testDir = new File(baseDir, "src/test/java");
    FileUtils.forceMkdir(testDir);
    File binaryDir = new File(baseDir, "target/classes");
    FileUtils.forceMkdir(binaryDir);

    ProjectDefinition project = ProjectDefinition.create()
      .setBaseDir(baseDir)
      .setWorkDir(workDir)
      .addSources("src/main/java", "src/main/unknown")
      .addTests("src/test/java", "src/test/unknown");

    ModuleFileSystemInitializer initializer = new ModuleFileSystemInitializer(new DefaultInputModule(project));

    assertThat(initializer.sources()).hasSize(1);
    assertThat(path(initializer.sources().get(0))).endsWith("src/main/java");
    assertThat(initializer.tests()).hasSize(1);
    assertThat(path(initializer.tests().get(0))).endsWith("src/test/java");
  }

  @Test
  public void supportFilenamesWithComma() throws IOException {
    File baseDir = temp.newFolder("base");
    File workDir = temp.newFolder("work");
    File sourceFile = new File(baseDir, "my,File.cs");
    sourceFile.createNewFile();
    File testFile = new File(baseDir, "my,TestFile.cs");
    testFile.createNewFile();

    ProjectDefinition project = ProjectDefinition.create()
      .setBaseDir(baseDir)
      .setWorkDir(workDir)
      .addSources("\"my,File.cs\"")
      .addTests("\"my,TestFile.cs\"");

    ModuleFileSystemInitializer initializer = new ModuleFileSystemInitializer(new DefaultInputModule(project));

    assertThat(initializer.sources()).hasSize(1);
    assertThat(initializer.sources().get(0)).isEqualTo(sourceFile.toPath());
    assertThat(initializer.tests()).hasSize(1);
    assertThat(initializer.tests().get(0)).isEqualTo(testFile.toPath());
  }

  private String path(Path f) throws IOException {
    return FilenameUtils.separatorsToUnix(f.toString());
  }

}
