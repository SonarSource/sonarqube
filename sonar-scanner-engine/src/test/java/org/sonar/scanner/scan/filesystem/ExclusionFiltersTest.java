/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
package org.sonar.scanner.scan.filesystem;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.fs.IndexedFile;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultIndexedFile;
import org.sonar.api.config.MapSettings;
import org.sonar.api.config.Settings;
import org.sonar.api.scan.filesystem.FileExclusions;

import static org.assertj.core.api.Assertions.assertThat;

public class ExclusionFiltersTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  private Path moduleBaseDir;

  @Before
  public void setUp() throws IOException {
    moduleBaseDir = temp.newFolder().toPath();
  }

  @Test
  public void no_inclusions_nor_exclusions() throws IOException {
    ExclusionFilters filter = new ExclusionFilters(new FileExclusions(new MapSettings()));
    filter.prepare();

    IndexedFile indexedFile = new DefaultIndexedFile("foo", moduleBaseDir, "src/main/java/com/mycompany/FooDao.java");
    assertThat(filter.accept(indexedFile, InputFile.Type.MAIN)).isTrue();
    assertThat(filter.accept(indexedFile, InputFile.Type.TEST)).isTrue();
  }

  @Test
  public void match_inclusion() throws IOException {
    Settings settings = new MapSettings();
    settings.setProperty(CoreProperties.PROJECT_INCLUSIONS_PROPERTY, "**/*Dao.java");
    ExclusionFilters filter = new ExclusionFilters(new FileExclusions(settings));
    filter.prepare();

    IndexedFile indexedFile = new DefaultIndexedFile("foo", moduleBaseDir, "src/main/java/com/mycompany/FooDao.java");
    assertThat(filter.accept(indexedFile, InputFile.Type.MAIN)).isTrue();

    indexedFile = new DefaultIndexedFile("foo", moduleBaseDir, "src/main/java/com/mycompany/Foo.java");
    assertThat(filter.accept(indexedFile, InputFile.Type.MAIN)).isFalse();
  }

  @Test
  public void match_at_least_one_inclusion() throws IOException {
    Settings settings = new MapSettings();
    settings.setProperty(CoreProperties.PROJECT_INCLUSIONS_PROPERTY, "**/*Dao.java,**/*Dto.java");
    ExclusionFilters filter = new ExclusionFilters(new FileExclusions(settings));

    filter.prepare();

    IndexedFile indexedFile = new DefaultIndexedFile("foo", moduleBaseDir, "src/main/java/com/mycompany/Foo.java");
    assertThat(filter.accept(indexedFile, InputFile.Type.MAIN)).isFalse();

    indexedFile = new DefaultIndexedFile("foo", moduleBaseDir, "src/main/java/com/mycompany/FooDto.java");
    assertThat(filter.accept(indexedFile, InputFile.Type.MAIN)).isTrue();
  }

  @Test
  public void match_exclusions() throws IOException {
    Settings settings = new MapSettings();
    settings.setProperty(CoreProperties.PROJECT_INCLUSIONS_PROPERTY, "src/main/java/**/*");
    settings.setProperty(CoreProperties.PROJECT_TEST_INCLUSIONS_PROPERTY, "src/test/java/**/*");
    settings.setProperty(CoreProperties.PROJECT_EXCLUSIONS_PROPERTY, "**/*Dao.java");
    ExclusionFilters filter = new ExclusionFilters(new FileExclusions(settings));

    filter.prepare();

    IndexedFile indexedFile = new DefaultIndexedFile("foo", moduleBaseDir, "src/main/java/com/mycompany/FooDao.java");
    assertThat(filter.accept(indexedFile, InputFile.Type.MAIN)).isFalse();

    indexedFile = new DefaultIndexedFile("foo", moduleBaseDir, "src/main/java/com/mycompany/Foo.java");
    assertThat(filter.accept(indexedFile, InputFile.Type.MAIN)).isTrue();

    // source exclusions do not apply to tests
    indexedFile = new DefaultIndexedFile("foo", moduleBaseDir, "src/test/java/com/mycompany/FooDao.java");
    assertThat(filter.accept(indexedFile, InputFile.Type.TEST)).isTrue();
  }

  @Test
  public void match_exclusion_by_absolute_path() throws IOException {
    File excludedFile = new File(moduleBaseDir.toString(), "src/main/java/org/bar/Bar.java");

    Settings settings = new MapSettings();
    settings.setProperty(CoreProperties.PROJECT_INCLUSIONS_PROPERTY, "src/main/java/**/*");
    settings.setProperty(CoreProperties.PROJECT_EXCLUSIONS_PROPERTY, "file:" + excludedFile.getAbsolutePath());
    ExclusionFilters filter = new ExclusionFilters(new FileExclusions(settings));

    filter.prepare();

    IndexedFile indexedFile = new DefaultIndexedFile("foo", moduleBaseDir, "src/main/java/org/bar/Foo.java");
    assertThat(filter.accept(indexedFile, InputFile.Type.MAIN)).isTrue();

    indexedFile = new DefaultIndexedFile("foo", moduleBaseDir, "src/main/java/org/bar/Bar.java");
    assertThat(filter.accept(indexedFile, InputFile.Type.MAIN)).isFalse();
  }

  @Test
  public void trim_pattern() {
    Settings settings = new MapSettings();
    settings.setProperty(CoreProperties.PROJECT_EXCLUSIONS_PROPERTY, "   **/*Dao.java   ");
    ExclusionFilters filter = new ExclusionFilters(new FileExclusions(settings));

    assertThat(filter.prepareMainExclusions()[0].toString()).isEqualTo("**/*Dao.java");
  }

}
