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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.config.Settings;
import org.sonar.api.scan.filesystem.FileFilter;
import org.sonar.api.scan.filesystem.ModuleFileSystem;
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
    ModuleFileSystem fs = provider.provide(module, new PathResolver(), new TempDirectories(), mock(LanguageFileFilters.class),
      new Settings(), new FileFilter[0]);

    assertThat(fs).isNotNull();
    assertThat(fs.baseDir().getCanonicalPath()).isEqualTo(baseDir.getCanonicalPath());
    assertThat(fs.workingDir().getCanonicalPath()).isEqualTo(workDir.getCanonicalPath());
    assertThat(fs.sourceDirs()).isEmpty();
    assertThat(fs.sourceFiles()).isEmpty();
    assertThat(fs.testDirs()).isEmpty();
    assertThat(fs.testFiles()).isEmpty();
  }

  @Test
  public void default_charset_is_platform_dependent() throws IOException {
    ModuleFileSystemProvider provider = new ModuleFileSystemProvider();

    ModuleFileSystem fs = provider.provide(newSimpleModule(), new PathResolver(), new TempDirectories(), mock(LanguageFileFilters.class),
      new Settings(), new FileFilter[0]);

    assertThat(fs.sourceCharset()).isEqualTo(Charset.defaultCharset());
  }

  @Test
  public void set_charset() throws IOException {
    ModuleFileSystemProvider provider = new ModuleFileSystemProvider();
    ProjectDefinition module = newSimpleModule();
    Settings settings = new Settings();
    settings.setProperty(CoreProperties.ENCODING_PROPERTY, Charsets.ISO_8859_1.name());

    ModuleFileSystem fs = provider.provide(module, new PathResolver(), new TempDirectories(), mock(LanguageFileFilters.class),
      settings, new FileFilter[0]);

    assertThat(fs.sourceCharset()).isEqualTo(Charsets.ISO_8859_1);
  }

  private ProjectDefinition newSimpleModule() {
    return ProjectDefinition.create()
      .setBaseDir(temp.newFolder("base"));
  }
}
