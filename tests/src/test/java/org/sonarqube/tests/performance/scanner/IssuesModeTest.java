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
import com.sonar.orchestrator.build.SonarScanner;
import java.io.File;
import java.io.IOException;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonarqube.tests.performance.AbstractPerfTest;
import org.sonarqube.tests.performance.PerfRule;

public class IssuesModeTest extends AbstractPerfTest {

  @ClassRule
  public static Orchestrator orchestrator = ScannerPerformanceSuite.ORCHESTRATOR;

  @ClassRule
  public static TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public PerfRule perfRule = new PerfRule(4) {
    @Override
    protected void beforeEachRun() {
      orchestrator.resetData();
    }
  };

  @Before
  public void cleanDatabase() {
    orchestrator.resetData();
  }

  @Test
  public void issues_mode_scan_xoo_project() throws IOException {
    File userHome = temp.newFolder();
    orchestrator.getServer().provisionProject("sample", "xoo-sample");
    orchestrator.getServer().associateProjectToQualityProfile("sample", "xoo", "one-xoo-issue-per-line");
    SonarScanner runner = newScanner(
      "-Xmx512m -server",
      "sonar.analysis.mode", "issues",
      "sonar.userHome", userHome.getAbsolutePath(),
      "sonar.showProfiling", "true");
    long start = System.currentTimeMillis();
    orchestrator.executeBuild(runner, false);
    long duration = System.currentTimeMillis() - start;
    System.out.println("Issues analysis: " + duration + "ms");

    perfRule.assertDurationAround(duration, 4650L);
  }
}
