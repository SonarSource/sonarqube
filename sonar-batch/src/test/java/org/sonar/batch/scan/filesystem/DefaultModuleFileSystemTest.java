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
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.scan.filesystem.FileQuery;
import org.sonar.api.scan.filesystem.InputFile;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class DefaultModuleFileSystemTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  InputFileCache fileCache = mock(InputFileCache.class);
  Settings settings = new Settings();
  FileIndexer fileIndexer = mock(FileIndexer.class);

  @Test
  public void test_equals_and_hashCode() throws Exception {
    DefaultModuleFileSystem foo1 = new DefaultModuleFileSystem("foo", settings, fileCache, fileIndexer);
    DefaultModuleFileSystem foo2 = new DefaultModuleFileSystem("foo", settings, fileCache, fileIndexer);
    DefaultModuleFileSystem bar = new DefaultModuleFileSystem("bar", settings, fileCache, fileIndexer);

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
    DefaultModuleFileSystem fs = new DefaultModuleFileSystem("foo", settings, fileCache, fileIndexer);

    assertThat(fs.sourceCharset()).isEqualTo(Charset.defaultCharset());
    assertThat(fs.isDefaultSourceCharset()).isTrue();
  }

  @Test
  public void source_encoding_is_set() {
    settings.setProperty(CoreProperties.ENCODING_PROPERTY, "Cp1124");
    DefaultModuleFileSystem fs = new DefaultModuleFileSystem("foo", settings, fileCache, fileIndexer);

    assertThat(fs.sourceCharset()).isEqualTo(Charset.forName("Cp1124"));

    // This test fails when default Java encoding is "IBM AIX Ukraine". Sorry for that.
    assertThat(fs.isDefaultSourceCharset()).isFalse();
  }

  @Test
  public void test_dirs() throws IOException {
    File basedir = temp.newFolder("base");
    File buildDir = temp.newFolder("build");
    File workingDir = temp.newFolder("work");

    DefaultModuleFileSystem fs = new DefaultModuleFileSystem("foo", settings, fileCache, fileIndexer);
    fs.setBaseDir(basedir);
    fs.setBuildDir(buildDir);
    fs.setWorkingDir(workingDir);
    fs.addBinaryDir(new File(basedir, "target/classes"));
    fs.addSourceDir(new File(basedir, "src/main/java"));
    fs.addSourceDir(new File(basedir, "src/main/groovy"));
    fs.addTestDir(new File(basedir, "src/test/java"));

    assertThat(fs.baseDir().getCanonicalPath()).isEqualTo(basedir.getCanonicalPath());
    assertThat(fs.workingDir().getCanonicalPath()).isEqualTo(workingDir.getCanonicalPath());
    assertThat(fs.buildDir().getCanonicalPath()).isEqualTo(buildDir.getCanonicalPath());
    assertThat(fs.sourceDirs()).hasSize(2);
    assertThat(fs.testDirs()).hasSize(1);
    assertThat(fs.binaryDirs()).hasSize(1);
  }

  @Test
  public void test_additional_source_files() throws IOException {
    DefaultModuleFileSystem fs = new DefaultModuleFileSystem("foo", settings, fileCache, fileIndexer);
    assertThat(fs.additionalSourceFiles()).isEmpty();
    assertThat(fs.additionalTestFiles()).isEmpty();

    File main = temp.newFile("Main.java");
    File test = temp.newFile("Test.java");
    fs.setAdditionalSourceFiles(Lists.newArrayList(main));
    fs.setAdditionalTestFiles(Lists.newArrayList(test));
    assertThat(fs.additionalSourceFiles()).containsOnly(main);
    assertThat(fs.additionalTestFiles()).containsOnly(test);
  }

  @Test
  public void should_reset_dirs() throws IOException {
    File basedir = temp.newFolder();
    DefaultModuleFileSystem fs = new DefaultModuleFileSystem("foo", settings, fileCache, fileIndexer);
    fs.setBaseDir(basedir);
    fs.setWorkingDir(basedir);
    fs.addSourceDir(new File(basedir, "src/main/java"));

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
    verify(fileIndexer).index(fs);
  }

  @Test
  public void should_search_input_files() throws Exception {
    DefaultModuleFileSystem fs = new DefaultModuleFileSystem("foo", settings, fileCache, fileIndexer);

    File mainFile = temp.newFile();
    InputFile mainInput = InputFile.create(mainFile, "Main.java", ImmutableMap.of(InputFile.ATTRIBUTE_TYPE, InputFile.TYPE_SOURCE));
    InputFile testInput = InputFile.create(temp.newFile(), "Test.java", ImmutableMap.of(InputFile.ATTRIBUTE_TYPE, InputFile.TYPE_TEST));

    when(fileCache.byModule("foo")).thenReturn(Lists.newArrayList(mainInput, testInput));

    Iterable<InputFile> inputFiles = fs.inputFiles(FileQuery.onSource());
    assertThat(inputFiles).containsOnly(mainInput);

    List<File> files = fs.files(FileQuery.onSource());
    assertThat(files).containsOnly(mainFile);
  }

  @Test
  public void should_index() throws Exception {
    DefaultModuleFileSystem fs = new DefaultModuleFileSystem("foo", settings, fileCache, fileIndexer);

    verifyZeroInteractions(fileIndexer);

    fs.index();
    verify(fileIndexer).index(fs);
  }

  //
//
//
//  @Test
//  public void should_exclude_dirs_starting_with_dot() throws IOException {
//    File basedir = new File(resourcesDir(), "exclude_dir_starting_with_dot");
//    DefaultModuleFileSystem fileSystem = new DefaultModuleFileSystem(mock(RemoteFileHashes.class))
//      .setBaseDir(basedir)
//      .setWorkingDir(temp.newFolder())
//      .addSourceDir(new File(basedir, "src"))
//      .setSettings(new Settings());
//
//    List<File> files = fileSystem.files(FileQuery.onSource());
//    assertThat(files).hasSize(1);
//    assertThat(files.get(0).getName()).isEqualTo("Included.java");
//  }
//
//  @Test
//  public void should_load_source_files_by_language() throws IOException {
//    File basedir = new File(resourcesDir(), "main_and_test_files");
//    DefaultModuleFileSystem fileSystem = new DefaultModuleFileSystem(mock(RemoteFileHashes.class))
//      .setBaseDir(basedir)
//      .setWorkingDir(temp.newFolder())
//      .addSourceDir(new File(basedir, "src/main/java"))
//      .addTestDir(new File(basedir, "src/test/java"))
//      .setSettings(new Settings());
//
//    List<File> files = fileSystem.files(FileQuery.onSource().onLanguage("java"));
//    assertThat(files).hasSize(2);
//    for (File sourceFiles : files) {
//      assertThat(sourceFiles).exists().isFile();
//      assertThat(sourceFiles.getName()).isIn("Hello.java", "Foo.java");
//    }
//    assertThat(fileSystem.files(FileQuery.onSource().onLanguage("php"))).isEmpty();
//  }
//
//  @Test
//  public void should_load_test_files() throws IOException {
//    File basedir = new File(resourcesDir(), "main_and_test_files");
//    DefaultModuleFileSystem fileSystem = new DefaultModuleFileSystem(mock(RemoteFileHashes.class))
//      .setBaseDir(basedir)
//      .setWorkingDir(temp.newFolder())
//      .addSourceDir(new File(basedir, "src/main/java"))
//      .addTestDir(new File(basedir, "src/test/java"))
//      .setSettings(new Settings());
//
//    assertThat(fileSystem.testDirs()).hasSize(1);
//    List<File> testFiles = fileSystem.files(FileQuery.onTest());
//    assertThat(testFiles).hasSize(2);
//    for (File testFile : testFiles) {
//      assertThat(testFile).exists().isFile();
//      assertThat(testFile.getName()).endsWith("Test.java");
//    }
//  }
//
//  @Test
//  public void should_load_test_files_by_language() throws IOException {
//    File basedir = new File(resourcesDir(), "main_and_test_files");
//    DefaultModuleFileSystem fileSystem = new DefaultModuleFileSystem(mock(RemoteFileHashes.class))
//      .setBaseDir(basedir)
//      .setWorkingDir(temp.newFolder())
//      .addSourceDir(new File(basedir, "src/main/java"))
//      .addTestDir(new File(basedir, "src/test/java"))
//      .setSettings(new Settings());
//
//    List<File> testFiles = fileSystem.files(FileQuery.onTest().onLanguage("java"));
//    assertThat(testFiles).hasSize(2);
//    for (File testFile : testFiles) {
//      assertThat(testFile).exists().isFile();
//      assertThat(testFile.getName()).endsWith("Test.java");
//    }
//    assertThat(fileSystem.files(FileQuery.onTest().onLanguage("php"))).isEmpty();
//  }
//
//  private File resourcesDir() {
//    File dir = new File("test-resources/DefaultModuleFileSystemTest");
//    if (!dir.exists()) {
//      dir = new File("sonar-batch/test-resources/DefaultModuleFileSystemTest");
//    }
//    return dir;
//  }
//
//  @Test
//  public void should_apply_file_filters() throws IOException {
//    File basedir = new File(resourcesDir(), "main_and_test_files");
//    DefaultModuleFileSystem fileSystem = new DefaultModuleFileSystem(mock(RemoteFileHashes.class))
//      .setBaseDir(basedir)
//      .setWorkingDir(temp.newFolder())
//      .addSourceDir(new File(basedir, "src/main/java"))
//      .addFilters(new FileFilterWrapper(FileFilterUtils.nameFileFilter("Foo.java")))
//      .setSettings(new Settings());
//
//    List<File> files = fileSystem.files(FileQuery.onSource());
//    assertThat(files).hasSize(1);
//    assertThat(files.get(0).getName()).isEqualTo("Foo.java");
//  }
//
//  static class Php extends AbstractLanguage {
//    public Php() {
//      super("php");
//    }
//
//    public String[] getFileSuffixes() {
//      return new String[] {"php"};
//    }
//  }
//
//  static class Java extends AbstractLanguage {
//    public Java() {
//      super("java");
//    }
//
//    public String[] getFileSuffixes() {
//      return new String[] {"java", "jav"};
//    }
//  }
//
//
//
//  @Test
//  public void should_throw_if_incremental_mode_and_not_in_dryrun() throws Exception {
//    File basedir = temp.newFolder();
//    Settings settings = new Settings();
//    DefaultModuleFileSystem fileSystem = new DefaultModuleFileSystem(mock(RemoteFileHashes.class))
//      .setBaseDir(basedir)
//      .setWorkingDir(temp.newFolder())
//      .addSourceDir(new File(basedir, "src/main/java"))
//      .setSettings(settings);
//
//    settings.setProperty(CoreProperties.INCREMENTAL_PREVIEW, true);
//
//    thrown.expect(SonarException.class);
//    thrown.expectMessage("Incremental preview is only supported with preview mode");
//    fileSystem.files(FileQuery.onSource());
//  }
//
//  @Test
//  public void should_filter_changed_files() throws Exception {
//    File basedir = new File(resourcesDir(), "main_and_test_files");
//    Settings settings = new Settings();
//    settings.setProperty(CoreProperties.ENCODING_PROPERTY, "UTF-8");
//    File mainDir = new File(basedir, "src/main/java");
//    File testDir = new File(basedir, "src/test/java");
//    File foo = new File(mainDir, "Foo.java");
//    File hello = new File(mainDir, "Hello.java");
//    File fooTest = new File(testDir, "FooTest.java");
//    File helloTest = new File(testDir, "HelloTest.java");
//
//    RemoteFileHashes remoteFileHashes = mock(RemoteFileHashes.class);
//    when(remoteFileHashes.getPreviousHash(foo)).thenReturn("oldfoohash");
//    when(remoteFileHashes.getCurrentHash(foo, Charsets.UTF_8)).thenReturn("foohash");
//    when(remoteFileHashes.getPreviousHash(hello)).thenReturn("oldhellohash");
//    when(remoteFileHashes.getCurrentHash(hello, Charsets.UTF_8)).thenReturn("oldhellohash");
//    when(remoteFileHashes.getPreviousHash(fooTest)).thenReturn("oldfooTesthash");
//    when(remoteFileHashes.getCurrentHash(fooTest, Charsets.UTF_8)).thenReturn("fooTesthash");
//    when(remoteFileHashes.getPreviousHash(helloTest)).thenReturn("oldhelloTesthash");
//    when(remoteFileHashes.getCurrentHash(helloTest, Charsets.UTF_8)).thenReturn("oldhelloTesthash");
//
//    DefaultModuleFileSystem fileSystem = new DefaultModuleFileSystem(remoteFileHashes)
//      .setBaseDir(basedir)
//      .setWorkingDir(temp.newFolder())
//      .addSourceDir(mainDir)
//      .addTestDir(testDir)
//      .setSettings(settings);
//
//    assertThat(fileSystem.files(FileQuery.onSource())).containsOnly(foo, hello);
//    assertThat(fileSystem.files(FileQuery.onTest())).containsOnly(fooTest, helloTest);
//
//    assertThat(fileSystem.changedFiles(FileQuery.onSource())).containsExactly(foo);
//    assertThat(fileSystem.changedFiles(FileQuery.onTest())).containsExactly(fooTest);
//
//    settings.setProperty(CoreProperties.INCREMENTAL_PREVIEW, true);
//    settings.setProperty(CoreProperties.DRY_RUN, true);
//
//    assertThat(fileSystem.files(FileQuery.onSource())).containsExactly(foo);
//    assertThat(fileSystem.files(FileQuery.onTest())).containsExactly(fooTest);
//  }

}
