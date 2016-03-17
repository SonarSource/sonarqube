/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.batch.scan.filesystem;

import org.sonar.api.batch.fs.InputFile.Status;

import org.junit.Before;
import org.sonar.batch.analysis.DefaultAnalysisMode;
import com.google.common.collect.Lists;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Project;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class DefaultModuleFileSystemTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private Settings settings;
  private FileIndexer fileIndexer;
  private ModuleFileSystemInitializer initializer;
  private ComponentIndexer componentIndexer;
  private ModuleInputFileCache moduleInputFileCache;
  private DefaultAnalysisMode mode;

  @Before
  public void setUp() {
    settings = new Settings();
    fileIndexer = mock(FileIndexer.class);
    initializer = mock(ModuleFileSystemInitializer.class, Mockito.RETURNS_DEEP_STUBS);
    componentIndexer = mock(ComponentIndexer.class);
    moduleInputFileCache = mock(ModuleInputFileCache.class);
    mode = mock(DefaultAnalysisMode.class);
  }

  @Test
  public void test_equals_and_hashCode() throws Exception {
    DefaultModuleFileSystem foo1 = new DefaultModuleFileSystem(moduleInputFileCache,
      new Project("foo"), settings, fileIndexer, initializer, componentIndexer, mode);
    DefaultModuleFileSystem foo2 = new DefaultModuleFileSystem(moduleInputFileCache,
      new Project("foo"), settings, fileIndexer, initializer, componentIndexer, mode);
    DefaultModuleFileSystem bar = new DefaultModuleFileSystem(moduleInputFileCache,
      new Project("bar"), settings, fileIndexer, initializer, componentIndexer, mode);
    DefaultModuleFileSystem branch = new DefaultModuleFileSystem(moduleInputFileCache,
      new Project("bar", "branch", "My project"), settings, fileIndexer, initializer, componentIndexer, mode);

    assertThat(foo1.moduleKey()).isEqualTo("foo");
    assertThat(branch.moduleKey()).isEqualTo("bar:branch");
    assertThat(foo1.equals(foo1)).isTrue();
    assertThat(foo1.equals(foo2)).isTrue();
    assertThat(foo1.equals(bar)).isFalse();
    assertThat(foo1.equals("foo")).isFalse();
    assertThat(foo1.hashCode()).isEqualTo(foo1.hashCode());
    assertThat(foo1.hashCode()).isEqualTo(foo2.hashCode());
  }

  @Test
  public void default_source_encoding() {
    DefaultModuleFileSystem fs = new DefaultModuleFileSystem(moduleInputFileCache,
      new Project("foo"), settings, fileIndexer, initializer, componentIndexer, mode);

    assertThat(fs.sourceCharset()).isEqualTo(Charset.defaultCharset());
    assertThat(fs.isDefaultJvmEncoding()).isTrue();
  }

  @Test
  public void source_encoding_is_set() {
    settings.setProperty(CoreProperties.ENCODING_PROPERTY, "Cp1124");
    DefaultModuleFileSystem fs = new DefaultModuleFileSystem(moduleInputFileCache,
      new Project("foo"), settings, fileIndexer, initializer, componentIndexer, mode);

    assertThat(fs.encoding()).isEqualTo(Charset.forName("Cp1124"));
    assertThat(fs.sourceCharset()).isEqualTo(Charset.forName("Cp1124"));

    // This test fails when default Java encoding is "IBM AIX Ukraine". Sorry for that.
    assertThat(fs.isDefaultJvmEncoding()).isFalse();
  }

  @Test
  public void default_predicate_scan_only_changed() throws IOException {
    when(mode.scanAllFiles()).thenReturn(false);

    DefaultModuleFileSystem fs = new DefaultModuleFileSystem(moduleInputFileCache,
      new Project("foo"), settings, fileIndexer, initializer, componentIndexer, mode);

    File baseDir = temp.newFile();
    InputFile mainInput = new DefaultInputFile("foo", "Main.java").setModuleBaseDir(baseDir.toPath()).setType(InputFile.Type.MAIN);
    InputFile testInput = new DefaultInputFile("foo", "Test.java").setModuleBaseDir(baseDir.toPath()).setType(InputFile.Type.TEST);
    InputFile mainSameInput = new DefaultInputFile("foo", "MainSame.java").setModuleBaseDir(baseDir.toPath())
      .setType(InputFile.Type.TEST).setStatus(Status.SAME);
    when(moduleInputFileCache.inputFiles()).thenReturn(Lists.newArrayList(mainInput, testInput, mainSameInput));

    fs.index();
    Iterable<InputFile> inputFiles = fs.inputFiles(fs.predicates().all());
    assertThat(inputFiles).containsOnly(mainInput, testInput);

    Iterable<InputFile> allInputFiles = fs.inputFiles();
    assertThat(allInputFiles).containsOnly(mainInput, mainSameInput, testInput);
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
    File javaSrc = new File(basedir, "src/main/java");
    javaSrc.mkdirs();
    File groovySrc = new File(basedir, "src/main/groovy");
    groovySrc.mkdirs();
    when(initializer.sources()).thenReturn(Arrays.asList(javaSrc, groovySrc, additionalFile));
    File javaTest = new File(basedir, "src/test/java");
    javaTest.mkdirs();
    when(initializer.tests()).thenReturn(Arrays.asList(javaTest, additionalTest));

    DefaultModuleFileSystem fs = new DefaultModuleFileSystem(moduleInputFileCache,
      new Project("foo"), settings, fileIndexer, initializer, componentIndexer, mode);

    assertThat(fs.baseDir().getCanonicalPath()).isEqualTo(basedir.getCanonicalPath());
    assertThat(fs.workDir().getCanonicalPath()).isEqualTo(workingDir.getCanonicalPath());
    assertThat(fs.buildDir().getCanonicalPath()).isEqualTo(buildDir.getCanonicalPath());
    assertThat(fs.sourceDirs()).hasSize(2);
    assertThat(fs.testDirs()).hasSize(1);
    assertThat(fs.binaryDirs()).hasSize(1);
  }

  @Test
  public void should_search_input_files() throws Exception {
    DefaultModuleFileSystem fs = new DefaultModuleFileSystem(moduleInputFileCache,
      new Project("foo"), settings, fileIndexer, initializer, componentIndexer, mode);

    File baseDir = temp.newFile();
    InputFile mainInput = new DefaultInputFile("foo", "Main.java").setModuleBaseDir(baseDir.toPath()).setType(InputFile.Type.MAIN);
    InputFile testInput = new DefaultInputFile("foo", "Test.java").setModuleBaseDir(baseDir.toPath()).setType(InputFile.Type.TEST);
    when(moduleInputFileCache.inputFiles()).thenReturn(Lists.newArrayList(mainInput, testInput));

    fs.index();
    Iterable<InputFile> inputFiles = fs.inputFiles(fs.predicates().hasType(InputFile.Type.MAIN));
    assertThat(inputFiles).containsOnly(mainInput);

    Iterable<File> files = fs.files(fs.predicates().hasType(InputFile.Type.MAIN));
    assertThat(files).containsOnly(new File(baseDir, "Main.java"));
  }

  @Test
  public void should_index() {
    DefaultModuleFileSystem fs = new DefaultModuleFileSystem(moduleInputFileCache,
      new Project("foo"), settings, fileIndexer, initializer, componentIndexer, mode);

    verifyZeroInteractions(fileIndexer);

    fs.index();
    verify(fileIndexer).index(fs);
    verify(componentIndexer).execute(fs);
  }

}
