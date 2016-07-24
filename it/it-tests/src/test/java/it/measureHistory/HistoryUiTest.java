/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package it.measureHistory;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.selenium.Selenese;
import it.Category1Suite;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import util.selenium.SeleneseTest;

import static util.ItUtils.projectDir;

public class HistoryUiTest {

  @ClassRule
  public static Orchestrator orchestrator = Category1Suite.ORCHESTRATOR;

  @BeforeClass
  public static void initialize() {
    orchestrator.resetData();
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/measureHistory/one-issue-per-line-profile.xml"));
    orchestrator.getServer().provisionProject("sample", "sample");
    orchestrator.getServer().associateProjectToQualityProfile("sample", "xoo", "one-issue-per-line");
    analyzeProject("shared/xoo-history-v1", "2014-10-19");
    analyzeProject("shared/xoo-history-v2", "2014-11-13");
  }

  private static void analyzeProject(String path, String date) {
    orchestrator.executeBuild(SonarScanner.create(projectDir(path))
      .setProperties("sonar.projectDate", date));
  }

  @Test
  public void test_timeline_widget() {
    new SeleneseTest(Selenese.builder().setHtmlTestsInClasspath("history-timeline-widget",
      "/measureHistory/HistoryUiTest/history-timeline-widget/timeline-widget.html",
      // SONAR-3561
      "/measureHistory/HistoryUiTest/history-timeline-widget/should-display-even-if-one-missing-metric.html"
      ).build()).runOn(orchestrator);
  }

  @Test
  public void test_timemachine_widget() {
    // Use old way to execute Selenium because 'waitForTextPresent' action is not supported by SeleneseTest
    orchestrator.executeSelenese(Selenese.builder().setHtmlTestsInClasspath("history-timemachine-widget",
      "/measureHistory/HistoryUiTest/history-timemachine-widget/time-machine-widget.html",
      // SONAR-3354 & SONAR-3353
      "/measureHistory/HistoryUiTest/history-timemachine-widget/should-display-empty-table-if-no-measure.html",
      // SONAR-3650
      "/measureHistory/HistoryUiTest/history-timemachine-widget/should-exclude-new-metrics.html"
      ).build());
  }

  /**
   * SONAR-2911
   */
  @Test
  public void test_comparison_page_between_project_versions() {
    new SeleneseTest(Selenese.builder().setHtmlTestsInClasspath("comparison-page",
      "/measureHistory/HistoryUiTest/comparison/should-compare-project-versions.html"
      ).build()).runOn(orchestrator);
  }
}
