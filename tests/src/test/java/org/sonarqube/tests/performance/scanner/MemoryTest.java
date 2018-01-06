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

import com.google.common.base.Strings;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.SonarScanner;
import com.sonar.orchestrator.http.HttpMethod;
import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonarqube.tests.performance.AbstractPerfTest;
import org.sonarqube.tests.performance.MavenLogs;
import org.sonarqube.tests.performance.PerfRule;

public class MemoryTest extends AbstractPerfTest {

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

  @Before
  public void cleanDatabase() {
    orchestrator.resetData();
  }

  int DEPTH = 4;

  @Test
  public void should_not_fail_with_limited_xmx_memory_and_no_coverage_per_test() {
    orchestrator.executeBuild(
      newScanner("-Xmx80m -server -XX:-HeapDumpOnOutOfMemoryError"));
  }

  // Property on root module is duplicated in each module so it may be big
  @Test
  public void analyzeProjectWithManyModulesAndBigProperties() throws IOException {

    File baseDir = temp.newFolder();

    prepareModule(baseDir, "moduleA", 1);
    prepareModule(baseDir, "moduleB", 1);
    prepareModule(baseDir, "moduleC", 1);

    FileUtils.write(new File(baseDir, "sonar-project.properties"), "sonar.modules=moduleA,moduleB,moduleC\n", true);
    FileUtils.write(new File(baseDir, "sonar-project.properties"), "sonar.myBigProp=" + Strings.repeat("A", 10000), true);

    SonarScanner scanner = SonarScanner.create()
      .setProperties(
        "sonar.projectKey", "big-module-tree",
        "sonar.projectName", "Big Module Tree",
        "sonar.projectVersion", "1.0",
        "sonar.sources", "",
        "sonar.showProfiling", "true");
    scanner.setEnvironmentVariable("SONAR_SCANNER_OPTS", "-Xmx512m -server")
      .setProjectDir(baseDir);

    BuildResult result = orchestrator.executeBuild(scanner);
    perfRule.assertDurationAround(MavenLogs.extractTotalTime(result.getLogs()), 8890);

    // Second execution with a property on server side
    orchestrator.getServer().newHttpCall("/api/settings/set")
      .setMethod(HttpMethod.POST)
      .setAdminCredentials()
      .setParam("key", "sonar.anotherBigProp")
      .setParam("value", Strings.repeat("B", 1000))
      .setParam("component", "big-module-tree")
      .execute();
    result = orchestrator.executeBuild(scanner);
    perfRule.assertDurationAround(MavenLogs.extractTotalTime(result.getLogs()), 8900);
  }

  private void prepareModule(File parentDir, String moduleName, int depth) throws IOException {
    File moduleDir = new File(parentDir, moduleName);
    moduleDir.mkdir();
    File projectProps = new File(moduleDir, "sonar-project.properties");
    FileUtils.write(projectProps, "sonar.moduleKey=" + moduleName + "\n", true);
    if (depth < DEPTH) {
      FileUtils.write(projectProps, "sonar.modules=" + moduleName + "A," + moduleName + "B," + moduleName + "C\n", true);
      prepareModule(moduleDir, moduleName + "A", depth + 1);
      prepareModule(moduleDir, moduleName + "B", depth + 1);
      prepareModule(moduleDir, moduleName + "C", depth + 1);
    }
  }

}
