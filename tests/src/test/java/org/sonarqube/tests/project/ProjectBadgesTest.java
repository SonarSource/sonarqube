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
package org.sonarqube.tests.project;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.ws.Organizations.Organization;
import org.sonarqube.ws.Users.CreateWsResponse.User;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.projects.UpdateVisibilityRequest;

import static com.codeborne.selenide.Selenide.$;
import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.projectDir;

public class ProjectBadgesTest {

  private static final String PROJECT_KEY = "sample";
  private static final String WS_MEASURE_BADGES_ON_QUALITY_GATE = "api/project_badges/measure?project=" + PROJECT_KEY + "&metric=alert_status";
  private static final String WS_MEASURE_BADGES_ON_BUGS = "api/project_badges/measure?project=" + PROJECT_KEY + "&metric=bugs";
  private static final String WS_QUALITY_GATE_BADGE = "api/project_badges/quality_gate?project=" + PROJECT_KEY;
  private static final String SONAR_CLOUD_ORANGE_BADGE = "images/project_badges/sonarcloud-orange.svg";

  @ClassRule
  public static Orchestrator orchestrator = SonarCloudProjectSuite.ORCHESTRATOR;

  @Rule
  public Tester tester = new Tester(orchestrator);

  @Test
  public void public_project_badges() {
    orchestrator.executeBuild(SonarScanner.create(projectDir("shared/xoo-sample")));
    tester.openBrowser("/projects").openProjectDashboard(PROJECT_KEY);

    SelenideElement badgesModal = openBadgesModal();
    ElementsCollection badgeButtons = badgesModal.$$(".badge-button").shouldHaveSize(3);

    // Check quality gate badge
    shouldHaveUrl(badgesModal, WS_MEASURE_BADGES_ON_QUALITY_GATE);

    // Check bugs badge
    selectOption("Bugs");
    shouldHaveUrl(badgesModal, WS_MEASURE_BADGES_ON_BUGS);

    // Check marketing quality gate badge
    badgeButtons.get(1).click();
    shouldHaveUrl(badgesModal, WS_QUALITY_GATE_BADGE);

    // Check scanned on SonarCloud badge
    badgeButtons.get(2).click();
    selectOption("Orange");
    shouldHaveUrl(badgesModal, SONAR_CLOUD_ORANGE_BADGE);
  }

  @Test
  public void private_project_do_not_have_badges() {
    Organization org = tester.organizations().generate();
    User user = tester.users().generateAdministrator(org);
    orchestrator.executeBuild(
      SonarScanner
        .create(projectDir("shared/xoo-sample"))
        .setProperties("sonar.organization", org.getKey(), "sonar.login", user.getLogin(), "sonar.password", user.getLogin())
    );
    tester.wsClient().projects().updateVisibility(new UpdateVisibilityRequest().setProject("sample").setVisibility("private"));
    tester.openBrowser("/projects").logIn().submitCredentials(user.getLogin()).openProjectDashboard(PROJECT_KEY);
    shouldNotHaveBadges();
  }

  @Test
  public void project_badges_ws() {
    orchestrator.executeBuild(SonarScanner.create(projectDir("shared/xoo-sample")));
    assertThat(tester.wsClient().wsConnector().call(new GetRequest(WS_MEASURE_BADGES_ON_QUALITY_GATE)).failIfNotSuccessful().contentType()).isEqualTo("image/svg+xml");
    assertThat(tester.wsClient().wsConnector().call(new GetRequest(WS_MEASURE_BADGES_ON_BUGS)).failIfNotSuccessful().contentType()).isEqualTo("image/svg+xml");
    assertThat(tester.wsClient().wsConnector().call(new GetRequest(WS_QUALITY_GATE_BADGE)).failIfNotSuccessful().contentType()).isEqualTo("image/svg+xml");
    assertThat(tester.wsClient().wsConnector().call(new GetRequest(SONAR_CLOUD_ORANGE_BADGE)).failIfNotSuccessful().contentType()).isEqualTo("image/svg+xml");
  }

  private void shouldNotHaveBadges() {
    $(".js-project-badges").shouldNot(Condition.exist);
  }

  private SelenideElement openBadgesModal() {
    $(".js-project-badges").shouldBe(Condition.visible).click();
    return $(".modal-body").shouldBe(Condition.visible);
  }

  private void selectOption(String option) {
    SelenideElement select = $(".Select").should(Condition.visible);
    select.click();
    select.$$(".Select-option").find(Condition.text(option)).should(Condition.exist).click();
  }

  private void shouldHaveUrl(SelenideElement badgesModal, String url) {
    badgesModal.$(".badge-snippet pre")
      .shouldBe(Condition.visible)
      .shouldHave(Condition.text(url));
  }
}
