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
package it.debt;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.selenium.Selenese;
import it.Category2Suite;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import util.selenium.SeleneseTest;

import static util.ItUtils.projectDir;

public class TechnicalDebtWidgetTest {

  @ClassRule
  public static Orchestrator orchestrator = Category2Suite.ORCHESTRATOR;

  @ClassRule
  public static DebtConfigurationRule debtConfiguration = DebtConfigurationRule.create(orchestrator);

  @BeforeClass
  public static void init() {
    orchestrator.resetData();

    // Set rating grid values to not depend from default value
    debtConfiguration.updateRatingGrid(0.1d, 0.2d, 0.5d, 1d);

    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/debt/with-many-rules.xml"));
    orchestrator.getServer().provisionProject("com.sonarsource.it.samples:multi-modules-sample", "com.sonarsource.it.samples:multi-modules-sample");
    orchestrator.getServer().associateProjectToQualityProfile("com.sonarsource.it.samples:multi-modules-sample", "xoo", "with-many-rules");

    // need to execute the build twice in order to have history widgets
    // we made some exclusions to have variations in diff mode
    scanProject("2011-06-01", "**/a2/**");
    scanProject("2012-02-01", "");
  }

  private static void scanProject(String date, String excludes) {
    SonarRunner scan = SonarRunner.create(projectDir("shared/xoo-multi-modules-sample"))
      .setProperties("sonar.projectDate", date, "sonar.exclusions", excludes);
    orchestrator.executeBuild(scan);
  }

  /**
   * SONAR-4717
   */
  @Test
  public void technical_debt_in_issues_widget() {
    new SeleneseTest(Selenese.builder()
      .setHtmlTestsInClasspath("technical-debt-in-issues-widget",
        "/debt/TechnicalDebtWidgetTest/technical-debt/should-have-correct-values.html",
        "/debt/TechnicalDebtWidgetTest/technical-debt/should-open-remediationcost-on-drilldown-service.html",
        "/debt/TechnicalDebtWidgetTest/technical-debt/display-differential-values.html",
        // SONAR-4717
        "/debt/TechnicalDebtWidgetTest/technical-debt/is-in-issues-widget.html"
      ).build()).runOn(orchestrator);
  }

  /**
   * SONAR-4718
   */
  @Test
  public void technical_debt_pyramid_widget() {
    new SeleneseTest(Selenese.builder()
      .setHtmlTestsInClasspath("technical-debt-pyramid-widget",
        "/debt/TechnicalDebtWidgetTest/technical-debt-pyramid/should-have-correct-values.html",
        "/debt/TechnicalDebtWidgetTest/technical-debt-pyramid/should-open-links-on-drilldown-service.html",
        "/debt/TechnicalDebtWidgetTest/technical-debt-pyramid/display-differential-values.html"
      ).build()).runOn(orchestrator);
  }

  /**
   * SONAR-5450
   */
  @Test
  public void debt_overview_widget() {
    new SeleneseTest(Selenese.builder()
      .setHtmlTestsInClasspath("debt-overview-widget",
        "/debt/TechnicalDebtWidgetTest/debt-overview/should-have-correct-values.html",
        "/debt/TechnicalDebtWidgetTest/debt-overview/should-open-links-on-drilldown-service.html",
        "/debt/TechnicalDebtWidgetTest/debt-overview/display-differential-values.html"
      ).build()).runOn(orchestrator);
  }

}
