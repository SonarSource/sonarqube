/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.batch.scan.filesystem;

import com.google.common.base.Charsets;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.config.Settings;
import org.sonar.api.scan.filesystem.FileQuery;
import org.sonar.api.scan.filesystem.FileSystemFilter;
import org.sonar.api.scan.filesystem.ModuleFileSystem;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.batch.bootstrap.TempDirectories;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class ModuleFileSystemProviderTest {
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void test_provide() throws IOException {
    ModuleFileSystemProvider provider = new ModuleFileSystemProvider();
    File baseDir = temp.newFolder("base");
    File workDir = temp.newFolder("work");
    ProjectDefinition module = ProjectDefinition.create()
      .setBaseDir(baseDir)
      .setWorkDir(workDir);
    ModuleFileSystem fs = provider.provide(module, new PathResolver(), new TempDirectories(), mock(LanguageFilters.class),
      new Settings(), new FileSystemFilter[0]);

    assertThat(fs).isNotNull();
    assertThat(fs.baseDir().getCanonicalPath()).isEqualTo(baseDir.getCanonicalPath());
    assertThat(fs.workingDir().getCanonicalPath()).isEqualTo(workDir.getCanonicalPath());
    assertThat(fs.sourceDirs()).isEmpty();
    assertThat(fs.files(FileQuery.onSource())).isEmpty();
    assertThat(fs.testDirs()).isEmpty();
    assertThat(fs.files(FileQuery.onTest())).isEmpty();
  }

  @Test
  public void default_charset_is_platform_dependent() throws IOException {
    ModuleFileSystemProvider provider = new ModuleFileSystemProvider();

    ModuleFileSystem fs = provider.provide(newSimpleModule(), new PathResolver(), new TempDirectories(), mock(LanguageFilters.class),
      new Settings(), new FileSystemFilter[0]);

    assertThat(fs.sourceCharset()).isEqualTo(Charset.defaultCharset());
  }

  @Test
  public void set_charset() throws IOException {
    ModuleFileSystemProvider provider = new ModuleFileSystemProvider();
    ProjectDefinition module = newSimpleModule();
    Settings settings = new Settings();
    settings.setProperty(CoreProperties.ENCODING_PROPERTY, Charsets.ISO_8859_1.name());

    ModuleFileSystem fs = provider.provide(module, new PathResolver(), new TempDirectories(), mock(LanguageFilters.class),
      settings, new FileSystemFilter[0]);

    assertThat(fs.sourceCharset()).isEqualTo(Charsets.ISO_8859_1);
  }

  @Test
  public void test_directories() throws IOException {
    ModuleFileSystemProvider provider = new ModuleFileSystemProvider();

    File baseDir = temp.newFolder("base");
    File buildDir = temp.newFolder("build");
    File sourceDir = new File(baseDir, "src/main/java");
    FileUtils.forceMkdir(sourceDir);
    File testDir = new File(baseDir, "src/test/java");
    FileUtils.forceMkdir(testDir);
    File binaryDir = new File(baseDir, "target/classes");
    FileUtils.forceMkdir(binaryDir);

    ProjectDefinition project = ProjectDefinition.create()
      .setBaseDir(baseDir)
      .setBuildDir(buildDir)
      .addSourceDirs("src/main/java", "src/main/unknown")
      .addTestDirs("src/test/java", "src/test/unknown")
      .addBinaryDir("target/classes");

    ModuleFileSystem fs = provider.provide(project, new PathResolver(), new TempDirectories(), mock(LanguageFilters.class),
      new Settings(), new FileSystemFilter[0]);

    assertThat(fs.baseDir().getCanonicalPath()).isEqualTo(baseDir.getCanonicalPath());
    assertThat(fs.buildDir().getCanonicalPath()).isEqualTo(buildDir.getCanonicalPath());
    assertThat(fs.sourceDirs()).hasSize(1);
    assertThat(fs.sourceDirs().get(0).getCanonicalPath()).endsWith("src/main/java");
    assertThat(fs.testDirs()).hasSize(1);
    assertThat(fs.testDirs().get(0).getCanonicalPath()).endsWith("src/test/java");
    assertThat(fs.binaryDirs()).hasSize(1);
    assertThat(fs.binaryDirs().get(0).getCanonicalPath()).endsWith("target/classes");
  }

  private ProjectDefinition newSimpleModule() {
    return ProjectDefinition.create()
      .setBaseDir(temp.newFolder("base"));
  }
}
