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
package it.qualityGate;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.selenium.Selenese;
import it.Category1Suite;
import javax.annotation.Nullable;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.qualitygate.NewCondition;
import org.sonar.wsclient.qualitygate.QualityGate;
import org.sonar.wsclient.qualitygate.QualityGateClient;
import org.sonar.wsclient.qualitygate.QualityGateCondition;
import org.sonar.wsclient.qualitygate.UpdateCondition;
import util.ItUtils;
import util.selenium.SeleneseTest;

import static util.ItUtils.projectDir;
import static util.ItUtils.setServerProperty;

public class QualityGateUiTest {

  @ClassRule
  public static Orchestrator orchestrator = Category1Suite.ORCHESTRATOR;

  @BeforeClass
  public static void initPeriods() throws Exception {
    setServerProperty(orchestrator, "sonar.timemachine.period1", "previous_analysis");
    setServerProperty(orchestrator, "sonar.timemachine.period2", "30");
    setServerProperty(orchestrator, "sonar.timemachine.period3", "previous_version");
  }

  @AfterClass
  public static void resetPeriods() throws Exception {
    ItUtils.resetPeriods(orchestrator);
  }

  @Before
  public void cleanUp() {
    orchestrator.resetData();
  }

  /**
   * SONAR-3326
   */
  @Test
  public void display_alerts_correctly_in_history_page() {
    QualityGateClient qgClient = orchestrator.getServer().adminWsClient().qualityGateClient();
    QualityGate qGate = qgClient.create("AlertsForHistory");
    qgClient.setDefault(qGate.id());

    // with this configuration, project should have an Orange alert
    QualityGateCondition lowThresholds = qgClient.createCondition(NewCondition.create(qGate.id()).metricKey("lines").operator("GT").warningThreshold("5").errorThreshold("50"));
    scanSampleWithDate("2012-01-01");
    // with this configuration, project should have a Green alert
    qgClient.updateCondition(UpdateCondition.create(lowThresholds.id()).metricKey("lines").operator("GT").warningThreshold("5000").errorThreshold("5000"));
    scanSampleWithDate("2012-01-02");

    new SeleneseTest(Selenese.builder()
      .setHtmlTestsInClasspath("display-alerts-history-page",
        "/qualityGate/QualityGateUiTest/should-display-alerts-correctly-history-page.html"
      ).build()).runOn(orchestrator);

    qgClient.unsetDefault();
    qgClient.destroy(qGate.id());
  }

  /**
   * SONAR-1352
   */
  @Test
  public void display_period_alert_on_project_dashboard() {
    QualityGateClient qgClient = orchestrator.getServer().adminWsClient().qualityGateClient();
    QualityGate qGate = qgClient.create("AlertsForDashboard");
    qgClient.createCondition(NewCondition.create(qGate.id()).metricKey("lines").operator("LT").warningThreshold("0").errorThreshold("10")
      .period(1));
    qgClient.setDefault(qGate.id());

    // No alert
    scanSampleWithDate("2012-01-01");

    // Red alert because lines number has not changed since previous analysis
    scanSample();

    new SeleneseTest(Selenese.builder()
      .setHtmlTestsInClasspath("display-period-alerts",
        "/qualityGate/QualityGateUiTest/should-display-period-alerts-correctly.html"
      ).build()).runOn(orchestrator);

    qgClient.unsetDefault();
    qgClient.destroy(qGate.id());
  }

  @Test
  public void should_display_quality_gates_page() {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("should_display_quality_gates_page",
        "/qualityGate/QualityGateUiTest/should_display_quality_gates_page.html"
    ).build();
    new SeleneseTest(selenese).runOn(orchestrator);
  }

  private void scanSample() {
    scanSample(null, null);
  }

  private void scanSampleWithDate(String date) {
    scanSample(date, null);
  }

  private void scanSample(@Nullable String date, @Nullable String profile) {
    SonarRunner scan = SonarRunner.create(projectDir("shared/xoo-sample"))
      .setProperties("sonar.cpd.skip", "true");
    if (date != null) {
      scan.setProperty("sonar.projectDate", date);
    }
    if (profile != null) {
      scan.setProfile(profile);
    }
    orchestrator.executeBuild(scan);
  }

}
