/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.config.Settings;
import org.sonar.api.scan.filesystem.FileExclusions;

import java.io.IOException;

import static org.fest.assertions.Assertions.assertThat;

public class ExclusionFiltersTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void no_inclusions_nor_exclusions() throws IOException {
    ExclusionFilters filter = new ExclusionFilters(new FileExclusions(new Settings()));
    filter.prepare();

    java.io.File file = temp.newFile();
    DefaultInputFile inputFile = new DefaultInputFile("src/main/java/com/mycompany/FooDao.java").setFile(file);
    assertThat(filter.accept(inputFile, InputFile.Type.MAIN)).isTrue();
    assertThat(filter.accept(inputFile, InputFile.Type.TEST)).isTrue();
  }

  @Test
  public void match_inclusion() throws IOException {
    Settings settings = new Settings();
    settings.setProperty(CoreProperties.PROJECT_INCLUSIONS_PROPERTY, "**/*Dao.java");
    ExclusionFilters filter = new ExclusionFilters(new FileExclusions(settings));
    filter.prepare();

    java.io.File file = temp.newFile();
    DefaultInputFile inputFile = new DefaultInputFile("src/main/java/com/mycompany/FooDao.java").setFile(file);
    assertThat(filter.accept(inputFile, InputFile.Type.MAIN)).isTrue();

    inputFile = new DefaultInputFile("src/main/java/com/mycompany/Foo.java").setFile(file);
    assertThat(filter.accept(inputFile, InputFile.Type.MAIN)).isFalse();
  }


  @Test
  public void match_at_least_one_inclusion() throws IOException {
    Settings settings = new Settings();
    settings.setProperty(CoreProperties.PROJECT_INCLUSIONS_PROPERTY, "**/*Dao.java,**/*Dto.java");
    ExclusionFilters filter = new ExclusionFilters(new FileExclusions(settings));

    filter.prepare();

    java.io.File file = temp.newFile();

    DefaultInputFile inputFile = new DefaultInputFile("src/main/java/com/mycompany/Foo.java").setFile(file);
    assertThat(filter.accept(inputFile, InputFile.Type.MAIN)).isFalse();

    inputFile = new DefaultInputFile("src/main/java/com/mycompany/FooDto.java").setFile(file);
    assertThat(filter.accept(inputFile, InputFile.Type.MAIN)).isTrue();
  }

  @Test
  public void match_exclusions() throws IOException {
    Settings settings = new Settings();
    settings.setProperty(CoreProperties.PROJECT_INCLUSIONS_PROPERTY, "src/main/java/**/*");
    settings.setProperty(CoreProperties.PROJECT_TEST_INCLUSIONS_PROPERTY, "src/test/java/**/*");
    settings.setProperty(CoreProperties.PROJECT_EXCLUSIONS_PROPERTY, "**/*Dao.java");
    ExclusionFilters filter = new ExclusionFilters(new FileExclusions(settings));

    filter.prepare();

    java.io.File file = temp.newFile();
    DefaultInputFile inputFile = new DefaultInputFile("src/main/java/com/mycompany/FooDao.java").setFile(file);
    assertThat(filter.accept(inputFile, InputFile.Type.MAIN)).isFalse();

    inputFile = new DefaultInputFile("src/main/java/com/mycompany/Foo.java").setFile(file);
    assertThat(filter.accept(inputFile, InputFile.Type.MAIN)).isTrue();

    // source exclusions do not apply to tests
    inputFile = new DefaultInputFile("src/test/java/com/mycompany/FooDao.java").setFile(file);
    assertThat(filter.accept(inputFile, InputFile.Type.TEST)).isTrue();
  }

  @Test
  public void match_exclusion_by_absolute_path() throws IOException {
    java.io.File includedFile = temp.newFile("Foo.java");
    java.io.File excludedFile = temp.newFile("Bar.java");

    Settings settings = new Settings();
    settings.setProperty(CoreProperties.PROJECT_INCLUSIONS_PROPERTY, "src/main/java/**/*");
    settings.setProperty(CoreProperties.PROJECT_EXCLUSIONS_PROPERTY, "file:" + excludedFile.getCanonicalPath());
    ExclusionFilters filter = new ExclusionFilters(new FileExclusions(settings));

    filter.prepare();

    DefaultInputFile inputFile = new DefaultInputFile("src/main/java/org/bar/Foo.java").setFile(includedFile);
    assertThat(filter.accept(inputFile, InputFile.Type.MAIN)).isTrue();

    inputFile = new DefaultInputFile("src/main/java/org/bar/Bar.java").setFile(excludedFile);
    assertThat(filter.accept(inputFile, InputFile.Type.MAIN)).isFalse();
  }

  @Test
  public void trim_pattern() throws IOException {
    Settings settings = new Settings();
    settings.setProperty(CoreProperties.PROJECT_EXCLUSIONS_PROPERTY, "   **/*Dao.java   ");
    ExclusionFilters filter = new ExclusionFilters(new FileExclusions(settings));

    assertThat(filter.prepareMainExclusions()[0].toString()).isEqualTo("**/*Dao.java");
  }

}
