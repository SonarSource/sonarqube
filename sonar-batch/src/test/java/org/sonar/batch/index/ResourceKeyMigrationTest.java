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

import org.apache.commons.configuration.PropertiesConfiguration;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.resources.Project;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

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
      newInputFile(javaModule, "src/main/java/org/foo/Bar.java", "org.foo.Bar", false),
      newInputFile(javaModule, "src/main/java/RootBar.java", "[default].RootBar", false),
      newInputFile(javaModule, "src/test/java/org/foo/BarTest.java", "org.foo.BarTest", true));

    phpInputFiles = (Iterable) Arrays.asList(
      newInputFile(phpModule, "org/foo/Bar.php", "org/foo/Bar.php", false),
      newInputFile(phpModule, "RootBar.php", "RootBar.php", false),
      newInputFile(phpModule, "test/org/foo/BarTest.php", "org/foo/BarTest.php", true));

  }

  private DefaultInputFile newInputFile(Project module, String path, String deprecatedKey, boolean isTest) {
    File file = new File(baseDir, path);
    String effectiveKey = module.getKey() + ":" + path;
    String deprecatedEffectiveKey = module.getKey() + ":" + deprecatedKey;
    return new DefaultInputFile(path).setFile(file)
      .setKey(effectiveKey)
      .setDeprecatedKey(deprecatedEffectiveKey)
      .setType(isTest ? InputFile.Type.TEST : InputFile.Type.MAIN);
  }

  @Test
  public void shouldMigrateResourceKeys() {
    setupData("shouldMigrateResourceKeys");

    Logger logger = mock(Logger.class);
    ResourceKeyMigration migration = new ResourceKeyMigration(getSession(), logger);
    migration.checkIfMigrationNeeded(multiModuleProject);
    
    migration.migrateIfNeeded(javaModule, javaInputFiles);
    migration.migrateIfNeeded(phpModule, phpInputFiles);

    verify(logger).info("Component {} changed to {}", "b:org.foo.Bar", "b:src/main/java/org/foo/Bar.java");
    verify(logger).warn("Directory with key b:org/foo matches both b:src/main/java/org/foo and b:src/test/java/org/foo. First match is arbitrary chosen.");
    verify(logger).info("Component {} changed to {}", "b:org.foo.BarTest", "b:src/test/java/org/foo/BarTest.java");
    verify(logger).info("Component {} changed to {}", "b:[default].RootBar", "b:src/main/java/RootBar.java");
    verify(logger).info("Component {} changed to {}", "b:org/foo", "b:src/main/java/org/foo");
    verify(logger).info("Component {} changed to {}", "b:[root]", "b:src/main/java");

    checkTables("shouldMigrateResourceKeys", new String[]{"build_date", "created_at"}, "projects");
  }

  private static Project newProject(String key, String language) {
    PropertiesConfiguration configuration = new PropertiesConfiguration();
    configuration.setProperty("sonar.language", language);
    return new Project(key).setConfiguration(configuration).setAnalysisType(Project.AnalysisType.DYNAMIC);
  }

}
