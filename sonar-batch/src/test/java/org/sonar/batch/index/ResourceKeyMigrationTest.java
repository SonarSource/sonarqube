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
package org.sonar.batch.index;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.DeprecatedDefaultInputFile;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Project;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.batch.scan.filesystem.DefaultModuleFileSystem;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ResourceKeyMigrationTest extends AbstractDbUnitTestCase {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  Project multiModuleProject, phpModule, javaModule;
  Iterable<InputFile> javaInputFiles;
  Iterable<InputFile> phpInputFiles;
  File baseDir;

  @Before
  public void before() throws Exception {
    SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy");

    multiModuleProject = newProject("root", "java");
    multiModuleProject.setName("Root").setAnalysisDate(format.parse("25/12/2010"));

    phpModule = newProject("a", "php");
    phpModule.setName("A").setAnalysisDate(format.parse("25/12/2010"));
    phpModule.setParent(multiModuleProject);
    phpModule.setPath("/moduleA");

    javaModule = newProject("b", "java");
    javaModule.setName("B").setAnalysisDate(format.parse("25/12/2010"));
    javaModule.setParent(multiModuleProject);
    javaModule.setPath("/moduleB");

    baseDir = temp.newFolder();

    javaInputFiles = (Iterable) Arrays.asList(
      newInputFile(javaModule, "src/main/java/org/foo/Bar.java", false, "java"),
      newInputFile(javaModule, "src/main/java/RootBar.java", false, "java"),
      newInputFile(javaModule, "src/test/java/org/foo/BarTest.java", true, "java"));

    phpInputFiles = (Iterable) Arrays.asList(
      newInputFile(phpModule, "org/foo/Bar.php", false, "php"),
      newInputFile(phpModule, "RootBar.php", false, "php"),
      newInputFile(phpModule, "test/org/foo/BarTest.php", true, "php"));

  }

  private DefaultInputFile newInputFile(Project module, String path, boolean isTest, String language) {
    return new DeprecatedDefaultInputFile(module.getKey(), path)
      .setModuleBaseDir(baseDir.toPath())
      .setLanguage(language)
      .setType(isTest ? InputFile.Type.TEST : InputFile.Type.MAIN);
  }

  @Test
  public void shouldMigrateResourceKeys() {
    setupData("shouldMigrateResourceKeys");

    Logger logger = mock(Logger.class);
    ResourceKeyMigration migration = new ResourceKeyMigration(getSession(), new PathResolver(), logger);
    migration.checkIfMigrationNeeded(multiModuleProject);

    DefaultModuleFileSystem fs = mock(DefaultModuleFileSystem.class);
    when(fs.sourceDirs()).thenReturn(Arrays.asList(new File(baseDir, "src/main/java")));
    when(fs.testDirs()).thenReturn(Arrays.asList(new File(baseDir, "src/test/java")));
    migration.migrateIfNeeded(javaModule, javaInputFiles, fs);

    when(fs.sourceDirs()).thenReturn(Arrays.asList(new File(baseDir, ".")));
    when(fs.testDirs()).thenReturn(Arrays.asList(new File(baseDir, "test")));
    migration.migrateIfNeeded(phpModule, phpInputFiles, fs);

    verify(logger).info("Component {} changed to {}", "b:org.foo.Bar", "b:src/main/java/org/foo/Bar.java");
    verify(logger).warn("Directory with key b:org/foo matches both b:src/main/java/org/foo and b:src/test/java/org/foo. First match is arbitrary chosen.");
    verify(logger).info("Component {} changed to {}", "b:org.foo.BarTest", "b:src/test/java/org/foo/BarTest.java");
    verify(logger).info("Component {} changed to {}", "b:[default].RootBar", "b:src/main/java/RootBar.java");
    verify(logger).info("Component {} changed to {}", "b:org/foo", "b:src/main/java/org/foo");
    verify(logger).info("Component {} changed to {}", "b:[root]", "b:src/main/java");

    checkTables("shouldMigrateResourceKeys", new String[] {"build_date", "created_at", "authorization_updated_at"}, "projects");
  }

  private static Project newProject(String key, String language) {
    Settings settings = new Settings();
    settings.setProperty(CoreProperties.PROJECT_LANGUAGE_PROPERTY, language);
    return new Project(key).setSettings(settings).setAnalysisType(Project.AnalysisType.DYNAMIC);
  }

}
