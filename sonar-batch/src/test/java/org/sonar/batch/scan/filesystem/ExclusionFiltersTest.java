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

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.scan.filesystem.FileExclusions;
import org.sonar.api.scan.filesystem.InputFile;
import org.sonar.api.scan.filesystem.ModuleFileSystem;
import org.sonar.api.scan.filesystem.internal.DefaultInputFile;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ExclusionFiltersTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  private ModuleFileSystem fs;
  private File basedir;

  @Before
  public void prepare() throws IOException {
    basedir = temp.newFolder();
    fs = mock(ModuleFileSystem.class);
    when(fs.baseDir()).thenReturn(basedir);
  }

  @Test
  public void should_match_source_inclusion() throws IOException {
    Settings settings = new Settings();
    settings.setProperty(CoreProperties.PROJECT_INCLUSIONS_PROPERTY, "**/*Dao.java");
    ExclusionFilters filter = new ExclusionFilters(new FileExclusions(settings));

    java.io.File file = temp.newFile();
    InputFile inputFile = DefaultInputFile.create(file, Charsets.UTF_8, "src/main/java/com/mycompany/Foo.java", ImmutableMap.of(
      InputFile.ATTRIBUTE_TYPE, InputFile.TYPE_MAIN
      ));

    assertThat(filter.accept(inputFile, fs)).isFalse();

    inputFile = DefaultInputFile.create(file, Charsets.UTF_8, "src/main/java/com/mycompany/FooDao.java", ImmutableMap.of(
      InputFile.ATTRIBUTE_TYPE, InputFile.TYPE_MAIN));
    assertThat(filter.accept(inputFile, fs)).isTrue();

    // test are excluded by default if no sonar.tests nor sonar.test.inclusions
    inputFile = DefaultInputFile.create(file, Charsets.UTF_8, "src/main/java/com/mycompany/Foo.java", ImmutableMap.of(
      InputFile.ATTRIBUTE_TYPE, InputFile.TYPE_TEST));
    assertThat(filter.accept(inputFile, fs)).isFalse();
  }

  @Test
  public void should_include_source_folders_by_default() throws IOException {
    Settings settings = new Settings();
    ExclusionFilters filter = new ExclusionFilters(new FileExclusions(settings));
    when(fs.sourceDirs()).thenReturn(Arrays.asList(new File(basedir, "src/main/java")));

    filter.logConfiguration(fs);

    java.io.File file = temp.newFile();
    InputFile inputFile = DefaultInputFile.create(file, Charsets.UTF_8, "src/main/java/com/mycompany/Foo.java", ImmutableMap.of(
      InputFile.ATTRIBUTE_TYPE, InputFile.TYPE_MAIN
      ));

    assertThat(filter.accept(inputFile, fs)).isTrue();

    inputFile = DefaultInputFile.create(file, Charsets.UTF_8, "src/main/java2/com/mycompany/FooDao.java", ImmutableMap.of(
      InputFile.ATTRIBUTE_TYPE, InputFile.TYPE_MAIN));
    assertThat(filter.accept(inputFile, fs)).isFalse();

    // source inclusions do not apply to tests
    inputFile = DefaultInputFile.create(file, Charsets.UTF_8, "src/main/java/com/mycompany/Foo.java", ImmutableMap.of(
      InputFile.ATTRIBUTE_TYPE, InputFile.TYPE_TEST));
    assertThat(filter.accept(inputFile, fs)).isFalse();
  }

  @Test
  public void should_include_source_and_test_folders_by_default() throws IOException {
    Settings settings = new Settings();
    ExclusionFilters filter = new ExclusionFilters(new FileExclusions(settings));
    when(fs.sourceDirs()).thenReturn(Arrays.asList(new File(basedir, "src/main/java")));
    when(fs.testDirs()).thenReturn(Arrays.asList(new File(basedir, "src/test/java")));

    filter.logConfiguration(fs);

    java.io.File file = temp.newFile();
    InputFile inputFile = DefaultInputFile.create(file, Charsets.UTF_8, "src/main/java/com/mycompany/Foo.java", ImmutableMap.of(
      InputFile.ATTRIBUTE_TYPE, InputFile.TYPE_MAIN
      ));

    assertThat(filter.accept(inputFile, fs)).isTrue();

    inputFile = DefaultInputFile.create(file, Charsets.UTF_8, "src/main/java/com/mycompany/Foo.java", ImmutableMap.of(
      InputFile.ATTRIBUTE_TYPE, InputFile.TYPE_TEST));
    assertThat(filter.accept(inputFile, fs)).isFalse();

    inputFile = DefaultInputFile.create(file, Charsets.UTF_8, "src/test/java/com/mycompany/Foo.java", ImmutableMap.of(
      InputFile.ATTRIBUTE_TYPE, InputFile.TYPE_TEST));
    assertThat(filter.accept(inputFile, fs)).isTrue();
  }

  @Test
  public void should_ignore_source_folders_if_inclusion_defined() throws IOException {
    Settings settings = new Settings();
    settings.setProperty(CoreProperties.PROJECT_INCLUSIONS_PROPERTY, "src/main/java2/**/*");
    ExclusionFilters filter = new ExclusionFilters(new FileExclusions(settings));
    when(fs.sourceDirs()).thenReturn(Arrays.asList(new File(basedir, "src/main/java")));

    filter.logConfiguration(fs);

    java.io.File file = temp.newFile();
    InputFile inputFile = DefaultInputFile.create(file, Charsets.UTF_8, "src/main/java/com/mycompany/Foo.java", ImmutableMap.of(
      InputFile.ATTRIBUTE_TYPE, InputFile.TYPE_MAIN
      ));

    assertThat(filter.accept(inputFile, fs)).isFalse();

    inputFile = DefaultInputFile.create(file, Charsets.UTF_8, "src/main/java2/com/mycompany/FooDao.java", ImmutableMap.of(
      InputFile.ATTRIBUTE_TYPE, InputFile.TYPE_MAIN));
    assertThat(filter.accept(inputFile, fs)).isTrue();

    // source inclusions do not apply to tests
    inputFile = DefaultInputFile.create(file, Charsets.UTF_8, "src/main/java/com/mycompany/Foo.java", ImmutableMap.of(
      InputFile.ATTRIBUTE_TYPE, InputFile.TYPE_TEST));
    assertThat(filter.accept(inputFile, fs)).isFalse();
  }

  @Test
  public void should_include_test_folders_by_default() throws IOException {
    Settings settings = new Settings();
    ExclusionFilters filter = new ExclusionFilters(new FileExclusions(settings));
    when(fs.testDirs()).thenReturn(Arrays.asList(new File(basedir, "src/test/java")));

    filter.logConfiguration(fs);

    java.io.File file = temp.newFile();
    InputFile inputFile = DefaultInputFile.create(file, Charsets.UTF_8, "src/test/java/com/mycompany/Foo.java", ImmutableMap.of(
      InputFile.ATTRIBUTE_TYPE, InputFile.TYPE_MAIN
      ));

    // test inclusions do not apply to main code
    assertThat(filter.accept(inputFile, fs)).isFalse();

    inputFile = DefaultInputFile.create(file, Charsets.UTF_8, "src/test2/java/com/mycompany/FooTest.java", ImmutableMap.of(
      InputFile.ATTRIBUTE_TYPE, InputFile.TYPE_TEST));
    assertThat(filter.accept(inputFile, fs)).isFalse();

    inputFile = DefaultInputFile.create(file, Charsets.UTF_8, "src/test/java/com/mycompany/Foo.java", ImmutableMap.of(
      InputFile.ATTRIBUTE_TYPE, InputFile.TYPE_TEST));
    assertThat(filter.accept(inputFile, fs)).isTrue();
  }

  @Test
  public void should_match_at_least_one_source_inclusion() throws IOException {
    Settings settings = new Settings();
    settings.setProperty(CoreProperties.PROJECT_INCLUSIONS_PROPERTY, "**/*Dao.java,**/*Dto.java");
    ExclusionFilters filter = new ExclusionFilters(new FileExclusions(settings));

    java.io.File file = temp.newFile();
    InputFile inputFile = DefaultInputFile.create(file, Charsets.UTF_8, "src/main/java/com/mycompany/Foo.java", ImmutableMap.of(
      InputFile.ATTRIBUTE_TYPE, InputFile.TYPE_MAIN
      ));

    assertThat(filter.accept(inputFile, fs)).isFalse();

    inputFile = DefaultInputFile.create(file, Charsets.UTF_8, "src/main/java/com/mycompany/FooDto.java", ImmutableMap.of(
      InputFile.ATTRIBUTE_TYPE, InputFile.TYPE_MAIN
      ));
    assertThat(filter.accept(inputFile, fs)).isTrue();
  }

  @Test
  public void should_match_source_exclusions() throws IOException {
    Settings settings = new Settings();
    settings.setProperty(CoreProperties.PROJECT_INCLUSIONS_PROPERTY, "src/main/java/**/*");
    settings.setProperty(CoreProperties.PROJECT_TEST_INCLUSIONS_PROPERTY, "src/test/java/**/*");
    settings.setProperty(CoreProperties.PROJECT_EXCLUSIONS_PROPERTY, "**/*Dao.java");
    ExclusionFilters filter = new ExclusionFilters(new FileExclusions(settings));

    java.io.File file = temp.newFile();
    InputFile inputFile = DefaultInputFile.create(file, Charsets.UTF_8, "src/main/java/com/mycompany/FooDao.java", ImmutableMap.of(
      InputFile.ATTRIBUTE_TYPE, InputFile.TYPE_MAIN
      ));
    assertThat(filter.accept(inputFile, fs)).isFalse();

    inputFile = DefaultInputFile.create(file, Charsets.UTF_8, "src/main/java/com/mycompany/Foo.java", ImmutableMap.of(
      InputFile.ATTRIBUTE_TYPE, InputFile.TYPE_MAIN));
    assertThat(filter.accept(inputFile, fs)).isTrue();

    // source exclusions do not apply to tests
    inputFile = DefaultInputFile.create(file, Charsets.UTF_8, "src/test/java/com/mycompany/FooDao.java", ImmutableMap.of(
      InputFile.ATTRIBUTE_TYPE, InputFile.TYPE_TEST));
    assertThat(filter.accept(inputFile, fs)).isTrue();
  }

  @Test
  public void should_match_source_exclusion_by_absolute_path() throws IOException {
    java.io.File includedFile = temp.newFile("Foo.java");
    java.io.File excludedFile = temp.newFile("Bar.java");

    Settings settings = new Settings();
    settings.setProperty(CoreProperties.PROJECT_INCLUSIONS_PROPERTY, "src/main/java/**/*");
    settings.setProperty(CoreProperties.PROJECT_EXCLUSIONS_PROPERTY, "file:" + excludedFile.getCanonicalPath());
    ExclusionFilters filter = new ExclusionFilters(new FileExclusions(settings));

    InputFile includedInput = DefaultInputFile.create(includedFile, Charsets.UTF_8, "src/main/java/org/bar/Foo.java", ImmutableMap.of(
      InputFile.ATTRIBUTE_TYPE, InputFile.TYPE_MAIN
      ));
    assertThat(filter.accept(includedInput, fs)).isTrue();

    InputFile excludedInput = DefaultInputFile.create(excludedFile, Charsets.UTF_8, "src/main/java/org/bar/Bar.java", ImmutableMap.of(
      InputFile.ATTRIBUTE_TYPE, InputFile.TYPE_MAIN
      ));
    assertThat(filter.accept(excludedInput, fs)).isFalse();
  }

  @Test
  public void should_trim_pattern() throws IOException {
    Settings settings = new Settings();
    settings.setProperty(CoreProperties.PROJECT_EXCLUSIONS_PROPERTY, "   **/*Dao.java   ");
    ExclusionFilters filter = new ExclusionFilters(new FileExclusions(settings));

    assertThat(filter.sourceExclusions()[0].toString()).isEqualTo("**/*Dao.java");
  }

}
