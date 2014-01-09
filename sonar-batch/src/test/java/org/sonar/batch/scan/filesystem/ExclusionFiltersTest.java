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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.File;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.Project;
import org.sonar.api.scan.filesystem.FileExclusions;
import org.sonar.api.scan.filesystem.internal.DefaultInputFile;
import org.sonar.api.scan.filesystem.internal.InputFile;

import java.io.IOException;

import static org.fest.assertions.Assertions.assertThat;

public class ExclusionFiltersTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void should_match_source_inclusion() throws IOException {
    Settings settings = new Settings();
    settings.setProperty(CoreProperties.PROJECT_INCLUSIONS_PROPERTY, "**/*Dao.java");
    ExclusionFilters filter = new ExclusionFilters(new FileExclusions(settings));

    java.io.File file = temp.newFile();
    InputFile inputFile = DefaultInputFile.create(file, Charsets.UTF_8, "src/main/java/com/mycompany/Foo.java", ImmutableMap.of(
      InputFile.ATTRIBUTE_TYPE, InputFile.TYPE_SOURCE,
      InputFile.ATTRIBUTE_SOURCE_RELATIVE_PATH, "com/mycompany/Foo.java"
      ));

    assertThat(filter.accept(inputFile)).isFalse();

    inputFile = DefaultInputFile.create(file, Charsets.UTF_8, "src/main/java/com/mycompany/FooDao.java", ImmutableMap.of(
      InputFile.ATTRIBUTE_TYPE, InputFile.TYPE_SOURCE,
      InputFile.ATTRIBUTE_SOURCE_RELATIVE_PATH, "com/mycompany/FooDao.java"
      ));
    assertThat(filter.accept(inputFile)).isTrue();

    // source inclusions do not apply to tests
    inputFile = DefaultInputFile.create(file, Charsets.UTF_8, "src/main/java/com/mycompany/Foo.java", ImmutableMap.of(
      InputFile.ATTRIBUTE_TYPE, InputFile.TYPE_TEST,
      InputFile.ATTRIBUTE_SOURCE_RELATIVE_PATH, "com/mycompany/Foo.java"
      ));
    assertThat(filter.accept(inputFile)).isTrue();
  }

  @Test
  public void should_match_at_least_one_source_inclusion() throws IOException {
    Settings settings = new Settings();
    settings.setProperty(CoreProperties.PROJECT_INCLUSIONS_PROPERTY, "**/*Dao.java,**/*Dto.java");
    ExclusionFilters filter = new ExclusionFilters(new FileExclusions(settings));

    java.io.File file = temp.newFile();
    InputFile inputFile = DefaultInputFile.create(file, Charsets.UTF_8, "src/main/java/com/mycompany/Foo.java", ImmutableMap.of(
      InputFile.ATTRIBUTE_TYPE, InputFile.TYPE_SOURCE,
      InputFile.ATTRIBUTE_SOURCE_RELATIVE_PATH, "com/mycompany/Foo.java"
      ));

    assertThat(filter.accept(inputFile)).isFalse();

    inputFile = DefaultInputFile.create(file, Charsets.UTF_8, "src/main/java/com/mycompany/FooDto.java", ImmutableMap.of(
      InputFile.ATTRIBUTE_TYPE, InputFile.TYPE_SOURCE,
      InputFile.ATTRIBUTE_SOURCE_RELATIVE_PATH, "com/mycompany/FooDto.java"
      ));
    assertThat(filter.accept(inputFile)).isTrue();
  }

  @Test
  public void should_match_source_exclusions() throws IOException {
    Settings settings = new Settings();
    settings.setProperty(CoreProperties.PROJECT_EXCLUSIONS_PROPERTY, "**/*Dao.java");
    ExclusionFilters filter = new ExclusionFilters(new FileExclusions(settings));

    java.io.File file = temp.newFile();
    InputFile inputFile = DefaultInputFile.create(file, Charsets.UTF_8, "src/main/java/com/mycompany/FooDao.java", ImmutableMap.of(
      InputFile.ATTRIBUTE_TYPE, InputFile.TYPE_SOURCE,
      InputFile.ATTRIBUTE_SOURCE_RELATIVE_PATH, "com/mycompany/FooDao.java"
      ));
    assertThat(filter.accept(inputFile)).isFalse();

    inputFile = DefaultInputFile.create(file, Charsets.UTF_8, "src/main/java/com/mycompany/Foo.java", ImmutableMap.of(
      InputFile.ATTRIBUTE_TYPE, InputFile.TYPE_SOURCE,
      InputFile.ATTRIBUTE_SOURCE_RELATIVE_PATH, "com/mycompany/Foo.java"
      ));
    assertThat(filter.accept(inputFile)).isTrue();

    // source exclusions do not apply to tests
    inputFile = DefaultInputFile.create(file, Charsets.UTF_8, "src/main/java/com/mycompany/FooDao.java", ImmutableMap.of(
      InputFile.ATTRIBUTE_TYPE, InputFile.TYPE_TEST,
      InputFile.ATTRIBUTE_SOURCE_RELATIVE_PATH, "com/mycompany/FooDao.java"
      ));
    assertThat(filter.accept(inputFile)).isTrue();
  }

  @Test
  public void should_match_source_exclusion_by_absolute_path() throws IOException {
    java.io.File includedFile = temp.newFile("Foo.java");
    java.io.File excludedFile = temp.newFile("Bar.java");

    Settings settings = new Settings();
    settings.setProperty(CoreProperties.PROJECT_EXCLUSIONS_PROPERTY, "file:" + excludedFile.getCanonicalPath());
    ExclusionFilters filter = new ExclusionFilters(new FileExclusions(settings));

    InputFile includedInput = DefaultInputFile.create(includedFile, Charsets.UTF_8, "src/main/java/org/bar/Foo.java", ImmutableMap.of(
      InputFile.ATTRIBUTE_TYPE, InputFile.TYPE_SOURCE
      ));
    assertThat(filter.accept(includedInput)).isTrue();

    InputFile excludedInput = DefaultInputFile.create(excludedFile, Charsets.UTF_8, "src/main/java/org/bar/Bar.java", ImmutableMap.of(
      InputFile.ATTRIBUTE_TYPE, InputFile.TYPE_SOURCE
      ));
    assertThat(filter.accept(excludedInput)).isFalse();
  }

  @Test
  public void should_match_resource_inclusions() throws IOException {
    Settings settings = new Settings();
    settings.setProperty(CoreProperties.PROJECT_INCLUSIONS_PROPERTY, "**/*Dao.c");
    ExclusionFilters filter = new ExclusionFilters(new FileExclusions(settings));

    assertThat(filter.isIgnored(new File("org/sonar", "FooDao.c"))).isFalse();
    assertThat(filter.isIgnored(new File("org/sonar", "Foo.c"))).isTrue();
  }

  @Test
  public void should_match_resource_exclusions() throws IOException {
    Settings settings = new Settings();
    settings.setProperty(CoreProperties.PROJECT_EXCLUSIONS_PROPERTY, "**/*Dao.c");
    ExclusionFilters filter = new ExclusionFilters(new FileExclusions(settings));

    assertThat(filter.isIgnored(new File("org/sonar", "FooDao.c"))).isTrue();
    assertThat(filter.isIgnored(new File("org/sonar", "Foo.c"))).isFalse();
  }

  @Test
  public void should_ignore_resource_exclusions_by_absolute_path() throws IOException {
    Settings settings = new Settings();
    settings.setProperty(CoreProperties.PROJECT_EXCLUSIONS_PROPERTY, "file:**/*Dao.c");
    ExclusionFilters filter = new ExclusionFilters(new FileExclusions(settings));

    assertThat(filter.isIgnored(new File("org/sonar", "FooDao.c"))).isFalse();
    assertThat(filter.isIgnored(new File("org/sonar", "Foo.c"))).isFalse();
  }

  @Test
  public void should_ignore_resource_inclusions_by_absolute_path() throws IOException {
    Settings settings = new Settings();
    settings.setProperty(CoreProperties.PROJECT_INCLUSIONS_PROPERTY, "file:**/*Dao.c");
    ExclusionFilters filter = new ExclusionFilters(new FileExclusions(settings));

    assertThat(filter.isIgnored(new File("org/sonar", "FooDao.c"))).isFalse();
    assertThat(filter.isIgnored(new File("org/sonar", "Foo.c"))).isFalse();
  }

  /**
   * JavaFile will be deprecated
   */
  @Test
  public void should_match_java_resource_inclusions() throws IOException {
    Settings settings = new Settings();
    settings.setProperty(CoreProperties.PROJECT_INCLUSIONS_PROPERTY, "**/*Dao.java");
    ExclusionFilters filter = new ExclusionFilters(new FileExclusions(settings));

    assertThat(filter.isIgnored(new JavaFile("org.sonar", "FooDao"))).isFalse();
    assertThat(filter.isIgnored(new JavaFile("org.sonar", "Foo"))).isTrue();
  }

  /**
   * JavaFile will be deprecated
   */
  @Test
  public void should_match_java_resource_exclusions() throws IOException {
    Settings settings = new Settings();
    settings.setProperty(CoreProperties.PROJECT_EXCLUSIONS_PROPERTY, "**/*Dao.java");
    ExclusionFilters filter = new ExclusionFilters(new FileExclusions(settings));

    assertThat(filter.isIgnored(new JavaFile("org.sonar", "FooDao"))).isTrue();
    assertThat(filter.isIgnored(new JavaFile("org.sonar", "Foo"))).isFalse();
  }

  @Test
  public void should_not_check_exclusions_on_non_file_resources() throws IOException {
    Settings settings = new Settings();
    settings.setProperty(CoreProperties.PROJECT_EXCLUSIONS_PROPERTY, "*");
    ExclusionFilters filter = new ExclusionFilters(new FileExclusions(settings));

    assertThat(filter.isIgnored(new Project("MyProject"))).isFalse();
  }

  @Test
  public void test_settings() throws IOException {
    Settings settings = new Settings();
    settings.setProperty(CoreProperties.PROJECT_INCLUSIONS_PROPERTY, "source/inclusions");
    settings.setProperty(CoreProperties.PROJECT_EXCLUSIONS_PROPERTY, "source/exclusions");
    settings.setProperty(CoreProperties.PROJECT_TEST_INCLUSIONS_PROPERTY, "test/inclusions");
    settings.setProperty(CoreProperties.PROJECT_TEST_EXCLUSIONS_PROPERTY, "test/exclusions");
    ExclusionFilters filter = new ExclusionFilters(new FileExclusions(settings));

    assertThat(filter.sourceInclusions()[0].toString()).isEqualTo("source/inclusions");
    assertThat(filter.testInclusions()[0].toString()).isEqualTo("test/inclusions");
    assertThat(filter.sourceExclusions()[0].toString()).isEqualTo("source/exclusions");
    assertThat(filter.testExclusions()[0].toString()).isEqualTo("test/exclusions");
  }

  @Test
  public void should_trim_pattern() throws IOException {
    Settings settings = new Settings();
    settings.setProperty(CoreProperties.PROJECT_EXCLUSIONS_PROPERTY, "   **/*Dao.java   ");
    ExclusionFilters filter = new ExclusionFilters(new FileExclusions(settings));

    assertThat(filter.sourceExclusions()[0].toString()).isEqualTo("**/*Dao.java");
  }

}
