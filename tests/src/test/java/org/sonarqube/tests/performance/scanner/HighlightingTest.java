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
import org.sonarqube.tests.performance.MavenLogs;
import org.sonarqube.tests.performance.PerfRule;

public class HighlightingTest extends AbstractPerfTest {

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

  @BeforeClass
  public static void setUp() {
    // Execute a first analysis to prevent any side effects with cache of plugin JAR files
    orchestrator.executeBuild(newScanner("-Xmx512m -server", "sonar.profile", "one-xoo-issue-per-line"));
  }

  @Before
  public void cleanDatabase() {
    orchestrator.resetData();
  }

  @Test
  public void computeSyntaxHighlightingOnBigFiles() throws IOException {
    File baseDir = temp.newFolder();
    File srcDir = new File(baseDir, "src");
    srcDir.mkdir();

    int nbFiles = 100;
    int ruleCount = 100000;
    int nblines = 1000;
    int linesize = ruleCount / nblines;
    for (int nb = 1; nb <= nbFiles; nb++) {
      File xooFile = new File(srcDir, "sample" + nb + ".xoo");
      File xoohighlightingFile = new File(srcDir, "sample" + nb + ".xoo.highlighting");
      FileUtils.write(xooFile, StringUtils.repeat(StringUtils.repeat("a", linesize) + "\n", nblines));
      StringBuilder sb = new StringBuilder(16 * ruleCount);
      for (int i = 0; i < ruleCount; i++) {
        sb.append(i).append(":").append(i + 1).append(":s\n");
      }
      FileUtils.write(xoohighlightingFile, sb.toString());
    }

    SonarScanner scanner = SonarScanner.create()
      .setProperties(
        "sonar.projectKey", "highlighting",
        "sonar.projectName", "highlighting",
        "sonar.projectVersion", "1.0",
        "sonar.sources", "src",
        "sonar.showProfiling", "true");
    scanner.setEnvironmentVariable("SONAR_RUNNER_OPTS", "-Xmx512m -server")
      .setProjectDir(baseDir);

    BuildResult result = orchestrator.executeBuild(scanner);
    System.out.println("Total time: " + MavenLogs.extractTotalTime(result.getLogs()));
    perfRule.assertDurationAround(MavenLogs.extractTotalTime(result.getLogs()), 28000L);

    Properties prof = readProfiling(baseDir, "highlighting");
    perfRule.assertDurationAround(Long.valueOf(prof.getProperty("Xoo Highlighting Sensor")), 10000L);

  }
}
