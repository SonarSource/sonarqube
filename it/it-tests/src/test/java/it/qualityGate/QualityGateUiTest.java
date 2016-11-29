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
package it.qualityGate;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import it.Category1Suite;
import javax.annotation.Nullable;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.sonar.wsclient.qualitygate.NewCondition;
import org.sonar.wsclient.qualitygate.QualityGate;
import org.sonar.wsclient.qualitygate.QualityGateClient;
import org.sonar.wsclient.qualitygate.QualityGateCondition;
import org.sonar.wsclient.qualitygate.UpdateCondition;
import util.ItUtils;

import static util.ItUtils.projectDir;
import static util.ItUtils.setServerProperty;
import static util.selenium.Selenese.runSelenese;

public class QualityGateUiTest {

  @ClassRule
  public static Orchestrator orchestrator = Category1Suite.ORCHESTRATOR;

  private static long DEFAULT_QUALITY_GATE;

  @BeforeClass
  public static void initPeriods() throws Exception {
    setServerProperty(orchestrator, "sonar.timemachine.period1", "previous_analysis");
    setServerProperty(orchestrator, "sonar.timemachine.period2", "30");
    setServerProperty(orchestrator, "sonar.timemachine.period3", "previous_version");
    DEFAULT_QUALITY_GATE = qgClient().list().defaultGate().id();
  }

  @AfterClass
  public static void resetData() throws Exception {
    ItUtils.resetPeriods(orchestrator);
    qgClient().setDefault(DEFAULT_QUALITY_GATE);
  }

  @Before
  public void cleanUp() {
    orchestrator.resetData();
  }

  /**
   * SONAR-3326
   */
  @Test
  @Ignore("history page is not available yet")
  public void display_alerts_correctly_in_history_page() {
    QualityGateClient qgClient = qgClient();
    QualityGate qGate = qgClient.create("AlertsForHistory");
    qgClient.setDefault(qGate.id());

    // with this configuration, project should have an Orange alert
    QualityGateCondition lowThresholds = qgClient.createCondition(NewCondition.create(qGate.id()).metricKey("lines").operator("GT").warningThreshold("5").errorThreshold("50"));
    scanSampleWithDate("2012-01-01");
    // with this configuration, project should have a Green alert
    qgClient.updateCondition(UpdateCondition.create(lowThresholds.id()).metricKey("lines").operator("GT").warningThreshold("5000").errorThreshold("5000"));
    scanSampleWithDate("2012-01-02");

    runSelenese(orchestrator, "/qualityGate/QualityGateUiTest/should-display-alerts-correctly-history-page.html");

    qgClient.unsetDefault();
    qgClient.destroy(qGate.id());
  }

  @Test
  public void should_display_quality_gates_page() {
    runSelenese(orchestrator, "/qualityGate/QualityGateUiTest/should_display_quality_gates_page.html");
  }

  private void scanSampleWithDate(String date) {
    scanSample(date, null);
  }

  private void scanSample(@Nullable String date, @Nullable String profile) {
    SonarScanner scan = SonarScanner.create(projectDir("shared/xoo-sample"))
      .setProperty("sonar.cpd.exclusions", "**/*");
    if (date != null) {
      scan.setProperty("sonar.projectDate", date);
    }
    if (profile != null) {
      scan.setProfile(profile);
    }
    orchestrator.executeBuild(scan);
  }

  private static QualityGateClient qgClient() {
    return orchestrator.getServer().adminWsClient().qualityGateClient();
  }

}
