/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonarqube.tests.qualityGate;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import org.sonarqube.tests.Category1Suite;
import java.util.Date;
import javax.annotation.Nullable;
import org.apache.commons.lang.time.DateFormatUtils;
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
import org.sonarqube.pageobjects.Navigation;
import org.sonarqube.pageobjects.ProjectActivityPage;

import static org.apache.commons.lang.time.DateUtils.addDays;
import static util.ItUtils.projectDir;
import static util.ItUtils.resetPeriod;
import static util.ItUtils.setServerProperty;
import static util.selenium.Selenese.runSelenese;

public class QualityGateUiTest {

  @ClassRule
  public static Orchestrator orchestrator = Category1Suite.ORCHESTRATOR;

  private static long DEFAULT_QUALITY_GATE;

  @BeforeClass
  public static void initPeriod() throws Exception {
    setServerProperty(orchestrator, "sonar.leak.period", "previous_analysis");
    DEFAULT_QUALITY_GATE = qgClient().list().defaultGate().id();
  }

  @AfterClass
  public static void resetData() throws Exception {
    resetPeriod(orchestrator);
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
  public void display_alerts_correctly_in_history_page() {
    QualityGateClient qgClient = qgClient();
    QualityGate qGate = qgClient.create("AlertsForHistory");
    qgClient.setDefault(qGate.id());

    String firstAnalysisDate = DateFormatUtils.ISO_DATE_FORMAT.format(addDays(new Date(), -2));
    String secondAnalysisDate = DateFormatUtils.ISO_DATE_FORMAT.format(addDays(new Date(), -1));

    // with this configuration, project should have an Orange alert
    QualityGateCondition lowThresholds = qgClient.createCondition(NewCondition.create(qGate.id()).metricKey("lines").operator("GT").warningThreshold("5").errorThreshold("50"));
    scanSampleWithDate(firstAnalysisDate);
    // with this configuration, project should have a Green alert
    qgClient.updateCondition(UpdateCondition.create(lowThresholds.id()).metricKey("lines").operator("GT").warningThreshold("5000").errorThreshold("5000"));
    scanSampleWithDate(secondAnalysisDate);

    Navigation nav = Navigation.create(orchestrator);
    ProjectActivityPage page = nav.openProjectActivity("sample");
    page
      .assertFirstAnalysisOfTheDayHasText(secondAnalysisDate, "Green (was Orange)")
      .assertFirstAnalysisOfTheDayHasText(firstAnalysisDate, "Orange");

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
