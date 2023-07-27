/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.event.Level;
import org.sonar.api.batch.fs.IndexedFile;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultIndexedFile;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.notifications.AnalysisWarnings;
import org.sonar.api.testfixtures.log.LogTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.sonar.api.CoreProperties.GLOBAL_TEST_EXCLUSIONS_PROPERTY;
import static org.sonar.api.CoreProperties.PROJECT_TESTS_EXCLUSIONS_PROPERTY;
import static org.sonar.api.CoreProperties.PROJECT_TESTS_INCLUSIONS_PROPERTY;
import static org.sonar.api.CoreProperties.PROJECT_TEST_EXCLUSIONS_PROPERTY;
import static org.sonar.api.CoreProperties.PROJECT_TEST_INCLUSIONS_PROPERTY;

public class AbstractExclusionFiltersTest {

  @Rule
  public LogTester logTester = new LogTester();
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private AnalysisWarnings analysisWarnings;
  private Path moduleBaseDir;
  private MapSettings settings;

  @Before
  public void setUp() throws IOException {
    settings = new MapSettings();
    moduleBaseDir = temp.newFolder().toPath();
    this.analysisWarnings = mock(AnalysisWarnings.class);
  }

  @Test
  public void should_handleAliasForTestInclusionsProperty() {
    settings.setProperty(PROJECT_TESTS_INCLUSIONS_PROPERTY, "**/*Dao.java");
    AbstractExclusionFilters filter = new AbstractExclusionFilters(analysisWarnings, settings.asConfig()::getStringArray) {
    };

    IndexedFile indexedFile = new DefaultIndexedFile("foo", moduleBaseDir, "test/main/java/com/mycompany/FooDao.java", null);
    assertThat(filter.isIncluded(indexedFile.path(), Paths.get(indexedFile.relativePath()), InputFile.Type.TEST)).isTrue();

    indexedFile = new DefaultIndexedFile("foo", moduleBaseDir, "test/main/java/com/mycompany/Foo.java", null);
    assertThat(filter.isIncluded(indexedFile.path(), Paths.get(indexedFile.relativePath()), InputFile.Type.TEST)).isFalse();

    String expectedWarn = "Use of sonar.tests.inclusions detected. " +
      "While being taken into account, the only supported property is sonar.test.inclusions. Consider updating your configuration.";
    assertThat(logTester.logs(Level.WARN)).hasSize(1)
      .contains(expectedWarn);
    verify(analysisWarnings).addUnique(expectedWarn);
  }

  @Test
  public void should_handleAliasForTestExclusionsProperty() {
    settings.setProperty(PROJECT_TESTS_EXCLUSIONS_PROPERTY, "**/*Dao.java");
    AbstractExclusionFilters filter = new AbstractExclusionFilters(analysisWarnings, settings.asConfig()::getStringArray) {
    };

    IndexedFile indexedFile = new DefaultIndexedFile("foo", moduleBaseDir, "test/main/java/com/mycompany/FooDao.java", null);
    assertThat(filter.isExcluded(indexedFile.path(), Paths.get(indexedFile.relativePath()), InputFile.Type.TEST)).isTrue();

    indexedFile = new DefaultIndexedFile("foo", moduleBaseDir, "test/main/java/com/mycompany/Foo.java", null);
    assertThat(filter.isExcluded(indexedFile.path(), Paths.get(indexedFile.relativePath()), InputFile.Type.TEST)).isFalse();

    String expectedWarn = "Use of sonar.tests.exclusions detected. " +
      "While being taken into account, the only supported property is sonar.test.exclusions. Consider updating your configuration.";
    assertThat(logTester.logs(Level.WARN)).hasSize(1)
      .contains(expectedWarn);
    verify(analysisWarnings).addUnique(expectedWarn);
  }

  @Test
  public void AbstractExclusionFilters_whenUsedGlobalTestPropertyAndProjectTestLegacyProperty_shouldNotLogAliasWarning() {
    settings.setProperty(PROJECT_TEST_EXCLUSIONS_PROPERTY, "**/*Dao.java");
    settings.setProperty(GLOBAL_TEST_EXCLUSIONS_PROPERTY, "**/*Dao.java");
    settings.setProperty(PROJECT_TEST_INCLUSIONS_PROPERTY, "**/*Dao.java");
    new AbstractExclusionFilters(analysisWarnings, settings.asConfig()::getStringArray) {
    };

    assertThat(logTester.logs(Level.WARN)).isEmpty();
  }

  @Test
  public void should_keepLegacyValue_when_legacyAndAliasPropertiesAreUsedForTestInclusions() {
    settings.setProperty(PROJECT_TESTS_INCLUSIONS_PROPERTY, "**/*Dao.java");
    settings.setProperty(PROJECT_TEST_INCLUSIONS_PROPERTY, "**/*Dto.java");
    AbstractExclusionFilters filter = new AbstractExclusionFilters(analysisWarnings, settings.asConfig()::getStringArray) {
    };

    IndexedFile indexedFile = new DefaultIndexedFile("foo", moduleBaseDir, "test/main/java/com/mycompany/FooDao.java", null);
    assertThat(filter.isIncluded(indexedFile.path(), Paths.get(indexedFile.relativePath()), InputFile.Type.TEST)).isFalse();

    indexedFile = new DefaultIndexedFile("foo", moduleBaseDir, "test/main/java/com/mycompany/FooDto.java", null);
    assertThat(filter.isIncluded(indexedFile.path(), Paths.get(indexedFile.relativePath()), InputFile.Type.TEST)).isTrue();

    String expectedWarn = "Use of sonar.test.inclusions and sonar.tests.inclusions at the same time. sonar.test.inclusions is taken into account. Consider updating your configuration";
    assertThat(logTester.logs(Level.WARN)).hasSize(1)
      .contains(expectedWarn);
    verify(analysisWarnings).addUnique(expectedWarn);
  }

  @Test
  public void should_keepLegacyValue_when_legacyAndAliasPropertiesAreUsedForTestExclusions() {
    settings.setProperty(PROJECT_TESTS_EXCLUSIONS_PROPERTY, "**/*Dao.java");
    settings.setProperty(PROJECT_TEST_EXCLUSIONS_PROPERTY, "**/*Dto.java");
    AbstractExclusionFilters filter = new AbstractExclusionFilters(analysisWarnings, settings.asConfig()::getStringArray) {
    };

    IndexedFile indexedFile = new DefaultIndexedFile("foo", moduleBaseDir, "test/main/java/com/mycompany/FooDao.java", null);
    assertThat(filter.isExcluded(indexedFile.path(), Paths.get(indexedFile.relativePath()), InputFile.Type.TEST)).isFalse();

    indexedFile = new DefaultIndexedFile("foo", moduleBaseDir, "test/main/java/com/mycompany/FooDto.java", null);
    assertThat(filter.isExcluded(indexedFile.path(), Paths.get(indexedFile.relativePath()), InputFile.Type.TEST)).isTrue();

    String expectedWarn = "Use of sonar.test.exclusions and sonar.tests.exclusions at the same time. sonar.test.exclusions is taken into account. Consider updating your configuration";
    assertThat(logTester.logs(Level.WARN)).hasSize(1)
      .contains(expectedWarn);
    verify(analysisWarnings).addUnique(expectedWarn);
  }
}
