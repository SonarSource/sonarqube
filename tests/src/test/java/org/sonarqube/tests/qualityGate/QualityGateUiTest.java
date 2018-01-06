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
package org.sonarqube.tests.qualityGate;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import java.util.Date;
import javax.annotation.Nullable;
import org.apache.commons.lang.time.DateFormatUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.By;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.qa.util.pageobjects.Navigation;
import org.sonarqube.qa.util.pageobjects.ProjectActivityPage;
import org.sonarqube.ws.Projects.CreateWsResponse.Project;
import org.sonarqube.ws.Qualitygates;
import org.sonarqube.ws.client.qualitygates.CreateConditionRequest;
import org.sonarqube.ws.client.qualitygates.UpdateConditionRequest;

import static com.codeborne.selenide.Selenide.$;
import static org.apache.commons.lang.time.DateUtils.addDays;
import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.projectDir;
import static util.selenium.Selenese.runSelenese;

public class QualityGateUiTest {

  @ClassRule
  public static Orchestrator orchestrator = QualityGateSuite.ORCHESTRATOR;

  @Rule
  public Tester tester = new Tester(orchestrator)
    // all the tests of QualityGateSuite must disable organizations
    .disableOrganizations();

  @Before
  public void initPeriod() {
    tester.settings().setGlobalSettings("sonar.leak.period", "previous_version");
  }

  /**
   * SONAR-3326
   */
  @Test
  public void display_alerts_correctly_in_history_page() {
    Project project = tester.projects().provision();
    Qualitygates.CreateResponse qGate = tester.qGates().generate();
    tester.qGates().associateProject(qGate, project);

    String firstAnalysisDate = DateFormatUtils.ISO_DATE_FORMAT.format(addDays(new Date(), -2));
    String secondAnalysisDate = DateFormatUtils.ISO_DATE_FORMAT.format(addDays(new Date(), -1));

    // with this configuration, project should have an Orange alert
    Qualitygates.CreateConditionResponse lowThresholds = tester.qGates().service()
      .createCondition(new CreateConditionRequest().setGateId(String.valueOf(qGate.getId())).setMetric("lines").setOp("GT").setWarning("5").setError("50"));
    scanSampleWithDate(project, firstAnalysisDate);
    // with this configuration, project should have a Green alert
    tester.qGates().service().updateCondition(new UpdateConditionRequest().setId(String.valueOf(lowThresholds.getId())).setMetric("lines").setOp("GT").setWarning("5000").setError("5000"));
    scanSampleWithDate(project, secondAnalysisDate);

    Navigation nav = Navigation.create(orchestrator);
    ProjectActivityPage page = nav.openProjectActivity(project.getKey());
    page
      .assertFirstAnalysisOfTheDayHasText(secondAnalysisDate, "Green (was Orange)")
      .assertFirstAnalysisOfTheDayHasText(firstAnalysisDate, "Orange");
  }

  @Test
  public void should_display_quality_gates_page() {
    runSelenese(orchestrator, "/qualityGate/QualityGateUiTest/should_display_quality_gates_page.html");
  }

  @Test
  public void should_have_a_global_link_to_quality_gates() {
    String login = tester.users().generate().getLogin();
    tester.openBrowser()
      .logIn().submitCredentials(login)
      .openQualityGates();

    SelenideElement element = $(".navbar-global .global-navbar-menu")
      .find(By.linkText("Quality Gates"))
      .should(Condition.exist);
    assertThat(element.attr("href")).endsWith("/quality_gates");
  }

  @Test
  public void should_not_allow_random_user_to_create() {
    String login = tester.users().generate().getLogin();
    String admin = tester.users().generateAdministrator().getLogin();
    tester.openBrowser()
      .logIn().submitCredentials(login)
      .openQualityGates()
      .canNotCreateQG()
      .displayQualityGateDetail("Sonar way");
    tester.openBrowser()
      .logIn().submitCredentials(admin)
      .openQualityGates()
      .canCreateQG()
      .displayIntro();
  }

  private void scanSampleWithDate(Project project, String date) {
    scanSample(project, date, null);
  }

  private void scanSample(Project project, @Nullable String date, @Nullable String profile) {
    SonarScanner scan = SonarScanner.create(projectDir("shared/xoo-sample")).setProperty("sonar.projectKey", project.getKey())
      .setProperty("sonar.cpd.exclusions", "**/*");
    if (date != null) {
      scan.setProperty("sonar.projectDate", date);
    }
    if (profile != null) {
      scan.setProfile(profile);
    }
    orchestrator.executeBuild(scan);
  }

}
