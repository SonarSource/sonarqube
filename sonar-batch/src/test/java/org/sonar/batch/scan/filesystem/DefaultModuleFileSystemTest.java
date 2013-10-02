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

import org.apache.commons.io.filefilter.FileFilterUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.AbstractLanguage;
import org.sonar.api.resources.Languages;
import org.sonar.api.scan.filesystem.FileQuery;
import org.sonar.api.scan.filesystem.FileSystemFilter;
import org.sonar.api.utils.SonarException;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultModuleFileSystemTest {
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void test_new_file_system() throws IOException {
    File basedir = temp.newFolder("base");
    File workingDir = temp.newFolder("work");
    LanguageFilters languageFilters = mock(LanguageFilters.class);
    FileSystemFilter fileFilter = mock(FileSystemFilter.class);

    DefaultModuleFileSystem fileSystem = new DefaultModuleFileSystem(mock(FileHashCache.class))
      .setBaseDir(basedir)
      .setWorkingDir(workingDir)
      .addBinaryDir(new File(basedir, "target/classes"))
      .addSourceDir(new File(basedir, "src/main/java"))
      .addSourceDir(new File(basedir, "src/main/groovy"))
      .addTestDir(new File(basedir, "src/test/java"))
      .addFilters(fileFilter)
      .setLanguageFilters(languageFilters);

    assertThat(fileSystem).isNotNull();
    assertThat(fileSystem.baseDir().getCanonicalPath()).isEqualTo(basedir.getCanonicalPath());
    assertThat(fileSystem.workingDir().getCanonicalPath()).isEqualTo(workingDir.getCanonicalPath());
    assertThat(fileSystem.sourceDirs()).hasSize(2);
    assertThat(fileSystem.testDirs()).hasSize(1);
    assertThat(fileSystem.binaryDirs()).hasSize(1);
    assertThat(fileSystem.filters()).containsOnly(fileFilter);
    assertThat(fileSystem.languageFilters()).isSameAs(languageFilters);
  }

  @Test
  public void default_source_encoding() {
    File basedir = temp.newFolder("base");
    DefaultModuleFileSystem fileSystem = new DefaultModuleFileSystem(mock(FileHashCache.class))
      .setBaseDir(basedir)
      .setSettings(new Settings());

    assertThat(fileSystem.sourceCharset()).isEqualTo(Charset.defaultCharset());
    assertThat(fileSystem.isDefaultSourceCharset()).isTrue();
  }

  @Test
  public void source_encoding_is_set() {
    File basedir = temp.newFolder("base");
    Settings settings = new Settings();
    settings.setProperty(CoreProperties.ENCODING_PROPERTY, "UTF-8");
    DefaultModuleFileSystem fileSystem = new DefaultModuleFileSystem(mock(FileHashCache.class))
      .setBaseDir(basedir)
      .setSettings(settings);

    assertThat(fileSystem.sourceCharset()).isEqualTo(Charset.forName("UTF-8"));
    assertThat(fileSystem.isDefaultSourceCharset()).isFalse();
  }

  @Test
  public void should_exclude_dirs_starting_with_dot() throws IOException {
    File basedir = new File(resourcesDir(), "exclude_dir_starting_with_dot");
    DefaultModuleFileSystem fileSystem = new DefaultModuleFileSystem(mock(FileHashCache.class))
      .setBaseDir(basedir)
      .setWorkingDir(temp.newFolder())
      .addSourceDir(new File(basedir, "src"))
      .setSettings(new Settings());

    List<File> files = fileSystem.files(FileQuery.onSource());
    assertThat(files).hasSize(1);
    assertThat(files.get(0).getName()).isEqualTo("Included.java");
  }

  @Test
  public void should_load_source_files_by_language() throws IOException {
    File basedir = new File(resourcesDir(), "main_and_test_files");
    DefaultModuleFileSystem fileSystem = new DefaultModuleFileSystem(mock(FileHashCache.class))
      .setBaseDir(basedir)
      .setWorkingDir(temp.newFolder())
      .addSourceDir(new File(basedir, "src/main/java"))
      .addTestDir(new File(basedir, "src/test/java"))
      .setLanguageFilters(new LanguageFilters(new Languages(new Java(), new Php())))
      .setSettings(new Settings());

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
    File basedir = new File(resourcesDir(), "main_and_test_files");
    DefaultModuleFileSystem fileSystem = new DefaultModuleFileSystem(mock(FileHashCache.class))
      .setBaseDir(basedir)
      .setWorkingDir(temp.newFolder())
      .addSourceDir(new File(basedir, "src/main/java"))
      .addTestDir(new File(basedir, "src/test/java"))
      .setSettings(new Settings());

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
    File basedir = new File(resourcesDir(), "main_and_test_files");
    DefaultModuleFileSystem fileSystem = new DefaultModuleFileSystem(mock(FileHashCache.class))
      .setBaseDir(basedir)
      .setWorkingDir(temp.newFolder())
      .addSourceDir(new File(basedir, "src/main/java"))
      .addTestDir(new File(basedir, "src/test/java"))
      .setLanguageFilters(new LanguageFilters(new Languages(new Java(), new Php())))
      .setSettings(new Settings());

    List<File> testFiles = fileSystem.files(FileQuery.onTest().onLanguage("java"));
    assertThat(testFiles).hasSize(2);
    for (File testFile : testFiles) {
      assertThat(testFile).exists().isFile();
      assertThat(testFile.getName()).endsWith("Test.java");
    }
    assertThat(fileSystem.files(FileQuery.onTest().onLanguage("php"))).isEmpty();
  }

  private File resourcesDir() {
    File dir = new File("test-resources/DefaultModuleFileSystemTest");
    if (!dir.exists()) {
      dir = new File("sonar-batch/test-resources/DefaultModuleFileSystemTest");
    }
    return dir;
  }

  @Test
  public void should_apply_file_filters() throws IOException {
    File basedir = new File(resourcesDir(), "main_and_test_files");
    DefaultModuleFileSystem fileSystem = new DefaultModuleFileSystem(mock(FileHashCache.class))
      .setBaseDir(basedir)
      .setWorkingDir(temp.newFolder())
      .addSourceDir(new File(basedir, "src/main/java"))
      .addFilters(new FileFilterWrapper(FileFilterUtils.nameFileFilter("Foo.java")))
      .setSettings(new Settings());

    List<File> files = fileSystem.files(FileQuery.onSource());
    assertThat(files).hasSize(1);
    assertThat(files.get(0).getName()).isEqualTo("Foo.java");
  }

  static class Php extends AbstractLanguage {
    public Php() {
      super("php");
    }

    public String[] getFileSuffixes() {
      return new String[] {"php"};
    }
  }

  static class Java extends AbstractLanguage {
    public Java() {
      super("java");
    }

    public String[] getFileSuffixes() {
      return new String[] {"java", "jav"};
    }
  }

  @Test
  public void test_reset_dirs() throws IOException {
    File basedir = temp.newFolder();
    DefaultModuleFileSystem fileSystem = new DefaultModuleFileSystem(mock(FileHashCache.class))
      .setBaseDir(basedir)
      .setWorkingDir(basedir)
      .addSourceDir(new File(basedir, "src/main/java"))
      .addFilters(new FileFilterWrapper(FileFilterUtils.nameFileFilter("Foo.java")));

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

  @Test
  public void should_throw_if_incremental_mode_and_not_in_dryrun() throws Exception {
    File basedir = temp.newFolder();
    Settings settings = new Settings();
    DefaultModuleFileSystem fileSystem = new DefaultModuleFileSystem(mock(FileHashCache.class))
      .setBaseDir(basedir)
      .setWorkingDir(temp.newFolder())
      .addSourceDir(new File(basedir, "src/main/java"))
      .setSettings(settings);

    settings.setProperty(CoreProperties.INCREMENTAL_PREVIEW, true);

    thrown.expect(SonarException.class);
    thrown.expectMessage("Incremental preview is only supported with preview mode");
    fileSystem.files(FileQuery.onSource());
  }

  @Test
  public void should_filter_changed_files() throws Exception {
    File basedir = new File(resourcesDir(), "main_and_test_files");
    Settings settings = new Settings();
    File mainDir = new File(basedir, "src/main/java");
    File testDir = new File(basedir, "src/test/java");
    File foo = new File(mainDir, "Foo.java");
    File hello = new File(mainDir, "Hello.java");
    File fooTest = new File(testDir, "FooTest.java");
    File helloTest = new File(testDir, "HelloTest.java");

    FileHashCache fileHashCache = mock(FileHashCache.class);
    when(fileHashCache.getPreviousHash(foo)).thenReturn("oldfoohash");
    when(fileHashCache.getCurrentHash(foo)).thenReturn("foohash");
    when(fileHashCache.getPreviousHash(hello)).thenReturn("oldhellohash");
    when(fileHashCache.getCurrentHash(hello)).thenReturn("oldhellohash");
    when(fileHashCache.getPreviousHash(fooTest)).thenReturn("oldfooTesthash");
    when(fileHashCache.getCurrentHash(fooTest)).thenReturn("fooTesthash");
    when(fileHashCache.getPreviousHash(helloTest)).thenReturn("oldhelloTesthash");
    when(fileHashCache.getCurrentHash(helloTest)).thenReturn("oldhelloTesthash");

    DefaultModuleFileSystem fileSystem = new DefaultModuleFileSystem(fileHashCache)
      .setBaseDir(basedir)
      .setWorkingDir(temp.newFolder())
      .addSourceDir(mainDir)
      .addTestDir(testDir)
      .setSettings(settings);

    assertThat(fileSystem.files(FileQuery.onSource())).containsExactly(foo, hello);
    assertThat(fileSystem.files(FileQuery.onTest())).containsExactly(fooTest, helloTest);

    assertThat(fileSystem.changedFiles(FileQuery.onSource())).containsExactly(foo);
    assertThat(fileSystem.changedFiles(FileQuery.onTest())).containsExactly(fooTest);

    settings.setProperty(CoreProperties.INCREMENTAL_PREVIEW, true);
    settings.setProperty(CoreProperties.DRY_RUN, true);

    assertThat(fileSystem.files(FileQuery.onSource())).containsExactly(foo);
    assertThat(fileSystem.files(FileQuery.onTest())).containsExactly(fooTest);
  }

}
