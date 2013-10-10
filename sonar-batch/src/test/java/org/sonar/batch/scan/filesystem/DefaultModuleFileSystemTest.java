/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.batch.scan.filesystem;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.scan.filesystem.FileQuery;
import org.sonar.api.scan.filesystem.InputFile;
import org.sonar.api.scan.filesystem.internal.DefaultInputFile;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class DefaultModuleFileSystemTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  Settings settings = new Settings();
  FileIndex fileIndex = mock(FileIndex.class);
  ModuleFileSystemInitializer initializer = mock(ModuleFileSystemInitializer.class, Mockito.RETURNS_DEEP_STUBS);

  @Test
  public void test_equals_and_hashCode() throws Exception {
    DefaultModuleFileSystem foo1 = new DefaultModuleFileSystem("foo", settings, fileIndex, initializer);
    DefaultModuleFileSystem foo2 = new DefaultModuleFileSystem("foo", settings, fileIndex, initializer);
    DefaultModuleFileSystem bar = new DefaultModuleFileSystem("bar", settings, fileIndex, initializer);

    assertThat(foo1.moduleKey()).isEqualTo("foo");
    assertThat(foo1.equals(foo1)).isTrue();
    assertThat(foo1.equals(foo2)).isTrue();
    assertThat(foo1.equals(bar)).isFalse();
    assertThat(foo1.equals("foo")).isFalse();
    assertThat(foo1.hashCode()).isEqualTo(foo1.hashCode());
    assertThat(foo1.hashCode()).isEqualTo(foo2.hashCode());
  }

  @Test
  public void default_source_encoding() {
    DefaultModuleFileSystem fs = new DefaultModuleFileSystem("foo", settings, fileIndex, initializer);

    assertThat(fs.sourceCharset()).isEqualTo(Charset.defaultCharset());
    assertThat(fs.isDefaultSourceCharset()).isTrue();
  }

  @Test
  public void source_encoding_is_set() {
    settings.setProperty(CoreProperties.ENCODING_PROPERTY, "Cp1124");
    DefaultModuleFileSystem fs = new DefaultModuleFileSystem("foo", settings, fileIndex, initializer);

    assertThat(fs.sourceCharset()).isEqualTo(Charset.forName("Cp1124"));

    // This test fails when default Java encoding is "IBM AIX Ukraine". Sorry for that.
    assertThat(fs.isDefaultSourceCharset()).isFalse();
  }

  @Test
  public void test_dirs() throws IOException {
    File basedir = temp.newFolder("base");
    File buildDir = temp.newFolder("build");
    File workingDir = temp.newFolder("work");
    File additionalFile = temp.newFile("Main.java");
    File additionalTest = temp.newFile("Test.java");
    when(initializer.baseDir()).thenReturn(basedir);
    when(initializer.buildDir()).thenReturn(buildDir);
    when(initializer.workingDir()).thenReturn(workingDir);
    when(initializer.binaryDirs()).thenReturn(Arrays.asList(new File(basedir, "target/classes")));
    when(initializer.sourceDirs()).thenReturn(Arrays.asList(new File(basedir, "src/main/java"), new File(basedir, "src/main/groovy")));
    when(initializer.testDirs()).thenReturn(Arrays.asList(new File(basedir, "src/test/java")));
    when(initializer.additionalSourceFiles()).thenReturn(Arrays.asList(additionalFile));
    when(initializer.additionalTestFiles()).thenReturn(Arrays.asList(additionalTest));


    DefaultModuleFileSystem fs = new DefaultModuleFileSystem("foo", settings, fileIndex, initializer);

    assertThat(fs.baseDir().getCanonicalPath()).isEqualTo(basedir.getCanonicalPath());
    assertThat(fs.workingDir().getCanonicalPath()).isEqualTo(workingDir.getCanonicalPath());
    assertThat(fs.buildDir().getCanonicalPath()).isEqualTo(buildDir.getCanonicalPath());
    assertThat(fs.sourceDirs()).hasSize(2);
    assertThat(fs.testDirs()).hasSize(1);
    assertThat(fs.binaryDirs()).hasSize(1);
    assertThat(fs.additionalSourceFiles()).containsOnly(additionalFile);
    assertThat(fs.additionalTestFiles()).containsOnly(additionalTest);
  }

  @Test
  public void should_reset_dirs() throws IOException {
    File basedir = temp.newFolder();
    when(initializer.baseDir()).thenReturn(basedir);
    when(initializer.workingDir()).thenReturn(basedir);
    when(initializer.sourceDirs()).thenReturn(Arrays.asList(new File(basedir, "src/main/java")));

    DefaultModuleFileSystem fs = new DefaultModuleFileSystem("foo", settings, fileIndex, initializer);

    File existingDir = temp.newFolder("new_folder");
    File notExistingDir = new File(existingDir, "not_exist");

    fs.resetDirs(existingDir, existingDir,
      Lists.newArrayList(existingDir, notExistingDir), Lists.newArrayList(existingDir, notExistingDir), Lists.newArrayList(existingDir, notExistingDir));

    assertThat(fs.baseDir().getCanonicalPath()).isEqualTo(existingDir.getCanonicalPath());
    assertThat(fs.buildDir().getCanonicalPath()).isEqualTo(existingDir.getCanonicalPath());
    assertThat(fs.sourceDirs()).hasSize(1);
    assertThat(fs.sourceDirs().get(0).getCanonicalPath()).isEqualTo(existingDir.getCanonicalPath());
    assertThat(fs.testDirs()).hasSize(1);
    assertThat(fs.testDirs().get(0).getCanonicalPath()).isEqualTo(existingDir.getCanonicalPath());
    assertThat(fs.binaryDirs()).hasSize(1);
    assertThat(fs.binaryDirs().get(0).getCanonicalPath()).isEqualTo(existingDir.getCanonicalPath());
    verify(fileIndex).index(fs);
  }

  @Test
  public void should_search_input_files() throws Exception {
    DefaultModuleFileSystem fs = new DefaultModuleFileSystem("foo", settings, fileIndex, initializer);

    File mainFile = temp.newFile();
    InputFile mainInput = DefaultInputFile.create(mainFile, "Main.java", ImmutableMap.of(InputFile.ATTRIBUTE_TYPE, InputFile.TYPE_SOURCE));
    InputFile testInput = DefaultInputFile.create(temp.newFile(), "Test.java", ImmutableMap.of(InputFile.ATTRIBUTE_TYPE, InputFile.TYPE_TEST));

    when(fileIndex.inputFiles("foo")).thenReturn(Lists.newArrayList(mainInput, testInput));

    Iterable<InputFile> inputFiles = fs.inputFiles(FileQuery.onSource());
    assertThat(inputFiles).containsOnly(mainInput);

    List<File> files = fs.files(FileQuery.onSource());
    assertThat(files).containsOnly(mainFile);
  }

  @Test
  public void should_index() throws Exception {
    DefaultModuleFileSystem fs = new DefaultModuleFileSystem("foo", settings, fileIndex, initializer);

    verifyZeroInteractions(fileIndex);

    fs.start();
    verify(fileIndex).index(fs);
    fs.stop();
  }

}
