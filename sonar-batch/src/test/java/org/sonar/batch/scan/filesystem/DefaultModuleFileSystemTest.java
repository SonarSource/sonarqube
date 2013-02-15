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
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.resources.AbstractLanguage;
import org.sonar.api.resources.Languages;
import org.sonar.api.scan.filesystem.FileQuery;
import org.sonar.api.scan.filesystem.FileSystemFilter;
import org.sonar.api.scan.filesystem.PathResolver;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class DefaultModuleFileSystemTest {
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void test_builder() throws IOException {
    File basedir = temp.newFolder("base");
    File workingDir = temp.newFolder("work");
    PathResolver pathResolver = mock(PathResolver.class);
    LanguageFilters languageFilters = mock(LanguageFilters.class);
    FileSystemFilter fileFilter = mock(FileSystemFilter.class);

    DefaultModuleFileSystem fileSystem = new DefaultModuleFileSystem.Builder()
      .baseDir(basedir)
      .workingDir(workingDir)
      .addBinaryDir(new File(basedir, "target/classes"))
      .addSourceDir(new File(basedir, "src/main/java"))
      .addSourceDir(new File(basedir, "src/main/groovy"))
      .addTestDir(new File(basedir, "src/test/java"))
      .addFsFilter(fileFilter)
      .sourceCharset(Charsets.UTF_8)
      .languageFilters(languageFilters)
      .build();

    assertThat(fileSystem).isNotNull();
    assertThat(fileSystem.baseDir().getCanonicalPath()).isEqualTo(basedir.getCanonicalPath());
    assertThat(fileSystem.workingDir().getCanonicalPath()).isEqualTo(workingDir.getCanonicalPath());
    assertThat(fileSystem.sourceDirs()).hasSize(2);
    assertThat(fileSystem.testDirs()).hasSize(1);
    assertThat(fileSystem.binaryDirs()).hasSize(1);
    assertThat(fileSystem.sourceCharset().name()).isEqualTo("UTF-8");
    assertThat(fileSystem.fsFilters()).containsOnly(fileFilter);
    assertThat(fileSystem.languageFilters()).isSameAs(languageFilters);
  }

  @Test
  public void should_exclude_dirs_starting_with_dot() throws IOException {
    File basedir = new File("test-resources/DefaultModuleFileSystemTest/exclude_dir_starting_with_dot");
    DefaultModuleFileSystem fileSystem = new DefaultModuleFileSystem.Builder()
      .baseDir(basedir)
      .sourceCharset(Charsets.UTF_8)
      .workingDir(temp.newFolder())
      .addSourceDir(new File(basedir, "src"))
      .build();

    List<File> files = fileSystem.files(FileQuery.onSource());
    assertThat(files).hasSize(1);
    assertThat(files.get(0).getName()).isEqualTo("Included.java");
  }

  @Test
  public void should_load_source_files_by_language() throws IOException {
    File basedir = new File("test-resources/DefaultModuleFileSystemTest/main_and_test_files");
    DefaultModuleFileSystem fileSystem = new DefaultModuleFileSystem.Builder()
      .baseDir(basedir)
      .sourceCharset(Charsets.UTF_8)
      .workingDir(temp.newFolder())
      .addSourceDir(new File(basedir, "src/main/java"))
      .addTestDir(new File(basedir, "src/test/java"))
      .languageFilters(new LanguageFilters(new Languages(new Java(), new Php())))
      .build();

    List<File> files = fileSystem.files(FileQuery.onSource().onLanguage("java"));
    assertThat(files).hasSize(2);
    for (File sourceFiles : files) {
      assertThat(sourceFiles).exists().isFile();
      assertThat(sourceFiles.getName()).isIn("Hello.java", "Foo.java");
    }
    assertThat(fileSystem.files(FileQuery.onSource().onLanguage("php"))).isEmpty();
  }

  @Test
  public void should_load_test_files() throws IOException {
    File basedir = new File("test-resources/DefaultModuleFileSystemTest/main_and_test_files");
    DefaultModuleFileSystem fileSystem = new DefaultModuleFileSystem.Builder()
      .baseDir(basedir)
      .sourceCharset(Charsets.UTF_8)
      .workingDir(temp.newFolder())
      .addSourceDir(new File(basedir, "src/main/java"))
      .addTestDir(new File(basedir, "src/test/java"))
      .build();

    assertThat(fileSystem.testDirs()).hasSize(1);
    List<File> testFiles = fileSystem.files(FileQuery.onTest());
    assertThat(testFiles).hasSize(2);
    for (File testFile : testFiles) {
      assertThat(testFile).exists().isFile();
      assertThat(testFile.getName()).endsWith("Test.java");
    }
  }

  @Test
  public void should_load_test_files_by_language() throws IOException {
    File basedir = new File("test-resources/DefaultModuleFileSystemTest/main_and_test_files");
    DefaultModuleFileSystem fileSystem = new DefaultModuleFileSystem.Builder()
      .baseDir(basedir)
      .sourceCharset(Charsets.UTF_8)
      .workingDir(temp.newFolder())
      .addSourceDir(new File(basedir, "src/main/java"))
      .addTestDir(new File(basedir, "src/test/java"))
      .languageFilters(new LanguageFilters(new Languages(new Java(), new Php())))
      .build();

    List<File> testFiles = fileSystem.files(FileQuery.onTest().onLanguage("java"));
    assertThat(testFiles).hasSize(2);
    for (File testFile : testFiles) {
      assertThat(testFile).exists().isFile();
      assertThat(testFile.getName()).endsWith("Test.java");
    }
    assertThat(fileSystem.files(FileQuery.onTest().onLanguage("php"))).isEmpty();
  }

  @Test
  public void should_apply_file_filters() throws IOException {
    File basedir = new File("test-resources/DefaultModuleFileSystemTest/main_and_test_files");
    DefaultModuleFileSystem fileSystem = new DefaultModuleFileSystem.Builder()
      .baseDir(basedir)
      .sourceCharset(Charsets.UTF_8)
      .workingDir(temp.newFolder())
      .addSourceDir(new File(basedir, "src/main/java"))
      .addFsFilter(new FileFilterWrapper(FileFilterUtils.nameFileFilter("Foo.java")))
      .build();

    List<File> files = fileSystem.files(FileQuery.onSource());
    assertThat(files).hasSize(1);
    assertThat(files.get(0).getName()).isEqualTo("Foo.java");
  }

  static class Php extends AbstractLanguage {
    public Php() {
      super("php");
    }

    public String[] getFileSuffixes() {
      return new String[]{"php"};
    }
  }

  static class Java extends AbstractLanguage {
    public Java() {
      super("java");
    }

    public String[] getFileSuffixes() {
      return new String[]{"java", "jav"};
    }
  }

  @Test
  public void test_reset_dirs() throws IOException {
    File basedir = temp.newFolder();
    DefaultModuleFileSystem fileSystem = new DefaultModuleFileSystem.Builder()
      .baseDir(basedir)
      .sourceCharset(Charsets.UTF_8)
      .workingDir(basedir)
      .addSourceDir(new File(basedir, "src/main/java"))
      .addFsFilter(new FileFilterWrapper(FileFilterUtils.nameFileFilter("Foo.java")))
      .build();

    File existingDir = temp.newFolder("new_folder");
    File notExistingDir = new File(existingDir, "not_exist");

    fileSystem.resetDirs(existingDir, existingDir,
      Arrays.asList(existingDir, notExistingDir), Arrays.asList(existingDir, notExistingDir), Arrays.asList(existingDir, notExistingDir));

    assertThat(fileSystem.baseDir().getCanonicalPath()).isEqualTo(existingDir.getCanonicalPath());
    assertThat(fileSystem.buildDir().getCanonicalPath()).isEqualTo(existingDir.getCanonicalPath());
    assertThat(fileSystem.sourceDirs()).hasSize(1);
    assertThat(fileSystem.sourceDirs().get(0).getCanonicalPath()).isEqualTo(existingDir.getCanonicalPath());
    assertThat(fileSystem.testDirs()).hasSize(1);
    assertThat(fileSystem.testDirs().get(0).getCanonicalPath()).isEqualTo(existingDir.getCanonicalPath());
    assertThat(fileSystem.binaryDirs()).hasSize(1);
    assertThat(fileSystem.binaryDirs().get(0).getCanonicalPath()).isEqualTo(existingDir.getCanonicalPath());
  }
}
