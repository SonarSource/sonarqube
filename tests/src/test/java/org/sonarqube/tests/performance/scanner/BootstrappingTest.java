/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonarqube.tests.performance.scanner;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.SonarScanner;
import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonarqube.tests.performance.AbstractPerfTest;
import org.sonarqube.tests.performance.MavenLogs;
import org.sonarqube.tests.performance.PerfRule;

public class BootstrappingTest extends AbstractPerfTest {

  @Rule
  public PerfRule perfRule = new PerfRule(4) {
    @Override
    protected void beforeEachRun() {
      orchestrator.resetData();
    }
  };

  @ClassRule
  public static TemporaryFolder temp = new TemporaryFolder();

  @ClassRule
  public static Orchestrator orchestrator = ScannerPerformanceSuite.ORCHESTRATOR;

  private static File manyFlatModulesBaseDir;
  private static File manyNestedModulesBaseDir;

  @BeforeClass
  public static void setUp() throws Exception {
    // Execute a first analysis to prevent any side effects with cache of plugin JAR files
    orchestrator.executeBuild(newScanner("-Xmx512m -server", "sonar.profile", "one-xoo-issue-per-line"));

    manyFlatModulesBaseDir = prepareProjectWithManyFlatModules(100);
    manyNestedModulesBaseDir = prepareProjectWithManyNestedModules(50);
  }

  @Test
  public void analyzeProjectWith100FlatModules() {

    SonarScanner scanner = SonarScanner.create()
      .setProperties(
        "sonar.projectKey", "many-flat-modules",
        "sonar.projectName", "Many Flat Modules",
        "sonar.projectVersion", "1.0",
        "sonar.sources", "",
        "sonar.showProfiling", "true");
    scanner
      .setEnvironmentVariable("SONAR_RUNNER_OPTS", "-Xmx512m -server")
      .setProjectDir(manyFlatModulesBaseDir);

    BuildResult result = orchestrator.executeBuild(scanner);
    // First analysis
    perfRule.assertDurationAround(MavenLogs.extractTotalTime(result.getLogs()), 7900L);

    result = orchestrator.executeBuild(scanner);
    // Second analysis is longer since we load project referential
    perfRule.assertDurationAround(MavenLogs.extractTotalTime(result.getLogs()), 8400L);
  }

  private static File prepareProjectWithManyFlatModules(int SIZE) throws IOException {
    File baseDir = temp.newFolder();
    File projectProps = new File(baseDir, "sonar-project.properties");

    StringBuilder moduleListBuilder = new StringBuilder(SIZE * ("module".length() + 2));

    for (int i = 1; i <= SIZE; i++) {
      moduleListBuilder.append("module").append(i);
      File moduleDir = new File(baseDir, "module" + i);
      moduleDir.mkdir();
      if (i != SIZE) {
        moduleListBuilder.append(",");
      }
    }

    FileUtils.write(projectProps, "sonar.modules=", true);
    FileUtils.write(projectProps, moduleListBuilder.toString(), true);
    FileUtils.write(projectProps, "\n", true);
    return baseDir;
  }

  @Test
  public void analyzeProjectWith50NestedModules() {
    SonarScanner scanner = SonarScanner.create()
      .setProperties(
        "sonar.projectKey", "many-nested-modules",
        "sonar.projectName", "Many Nested Modules",
        "sonar.projectVersion", "1.0",
        "sonar.sources", "",
        "sonar.showProfiling", "true");
    scanner.setEnvironmentVariable("SONAR_RUNNER_OPTS", "-Xmx512m -server");
    scanner.setProjectDir(manyNestedModulesBaseDir);

    BuildResult result = orchestrator.executeBuild(scanner);
    // First analysis
    perfRule.assertDurationAround(MavenLogs.extractTotalTime(result.getLogs()), 6300L);

    result = orchestrator.executeBuild(scanner);
    // Second analysis
    perfRule.assertDurationAround(MavenLogs.extractTotalTime(result.getLogs()), 6300L);
  }

  private static File prepareProjectWithManyNestedModules(int SIZE) throws IOException {
    File baseDir = temp.newFolder();
    File currentDir = baseDir;

    for (int i = 1; i <= SIZE; i++) {
      File projectProps = new File(currentDir, "sonar-project.properties");
      FileUtils.write(projectProps, "sonar.modules=module" + i + "\n", true);
      if (i >= 1) {
        FileUtils.write(projectProps, "sonar.moduleKey=module" + (i - 1), true);
      }
      File moduleDir = new File(currentDir, "module" + i);
      moduleDir.mkdir();
      currentDir = moduleDir;
    }
    FileUtils.write(new File(currentDir, "sonar-project.properties"), "sonar.moduleKey=module" + SIZE, true);
    return baseDir;
  }

}
