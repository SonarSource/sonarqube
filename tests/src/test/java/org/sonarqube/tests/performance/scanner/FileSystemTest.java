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
import com.sonar.orchestrator.build.SonarRunner;
import java.io.File;
import java.io.IOException;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonarqube.tests.performance.AbstractPerfTest;
import org.sonarqube.tests.performance.PerfRule;

public class FileSystemTest extends AbstractPerfTest {

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

  private static File baseDir;

  @BeforeClass
  public static void setUp() throws IOException {
    // Execute a first analysis to prevent any side effects with cache of plugin JAR files
    orchestrator.executeBuild(newScanner("-Xmx512m -server", "sonar.profile", "one-xoo-issue-per-line"));
    baseDir = prepareProject();
  }

  @Before
  public void cleanDatabase() {
    orchestrator.resetData();
  }

  @Test
  public void indexProjectWith1000BigFilesXmx128() throws IOException {
    run(128, 30000L);
  }

  private void run(int xmx, long expectedDuration) throws IOException {
    SonarRunner runner = SonarRunner.create()
      .setProperties(
        "sonar.projectKey", "filesystemXmx" + xmx,
        "sonar.projectName", "filesystem xmx" + xmx,
        "sonar.projectVersion", "1.0",
        "sonar.sources", "src",
        "sonar.analysis.mode", "issues",
        "sonar.preloadFileMetadata", "true",
        "sonar.showProfiling", "true")
      .setEnvironmentVariable("SONAR_RUNNER_OPTS", "-Xmx" + xmx + "m -server")
      .setProjectDir(baseDir);

    orchestrator.executeBuild(runner);

    Properties prof = readProfiling(baseDir, "filesystemXmx" + xmx);
    perfRule.assertDurationAround(Long.valueOf(prof.getProperty("Index filesystem")), expectedDuration);
  }

  private static File prepareProject() throws IOException {
    File baseDir = temp.newFolder();
    File srcDir = new File(baseDir, "src");
    srcDir.mkdir();

    int nbFiles = 1000;
    int lines = 10000;
    for (int nb = 1; nb <= nbFiles; nb++) {
      File xooFile = new File(srcDir, "sample" + nb + ".xoo");
      FileUtils.write(xooFile, StringUtils.repeat(StringUtils.repeat("a", 100) + "\n", lines));
    }
    return baseDir;
  }

}
