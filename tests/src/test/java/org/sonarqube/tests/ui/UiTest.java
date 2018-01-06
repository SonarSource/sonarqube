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
package org.sonarqube.tests.ui;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import java.util.Map;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.qa.util.pageobjects.Navigation;
import org.sonarqube.tests.Category4Suite;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.WsResponse;
import util.ItUtils;

import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.WebDriverRunner.url;
import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.projectDir;

public class UiTest {

  @ClassRule
  public static final Orchestrator orchestrator = Category4Suite.ORCHESTRATOR;

  @Rule
  public Tester tester = new Tester(orchestrator).disableOrganizations();

  @Test
  public void footer_contains_information() {
    tester.openBrowser().getFooter()
      .should(text("Documentation"))
      .should(text("SonarSource SA"));
  }

  @Test
  public void footer_contains_version() {
    WsResponse status = tester.wsClient().wsConnector().call(new GetRequest("api/navigation/global"));
    Map<String, Object> statusMap = ItUtils.jsonToMap(status.content());

    tester.openBrowser().getFooter()
      .should(text((String) statusMap.get("version")));
  }

  @Test
  public void footer_doesnt_contains_version_on_login_page() {
    WsResponse status = tester.wsClient().wsConnector().call(new GetRequest("api/navigation/global"));
    Map<String, Object> statusMap = ItUtils.jsonToMap(status.content());

    Navigation navigation = tester.openBrowser();
    navigation.openLogin();
    navigation.getFooter().shouldNot(text((String) statusMap.get("version")));
  }

  @Test
  public void footer_doesnt_contains_about_when_not_logged_in() {
    tester.settings().setGlobalSettings("sonar.forceAuthentication", "true");
    Navigation navigation = tester.openBrowser();
    navigation.openLogin();
    navigation.getFooter()
      .shouldNot(text("About"))
      .shouldNot(text("Web API"));
  }

  @Test
  public void many_page_transitions() {
    analyzeXooSample();

    tester.openBrowser().open("/about");

    // on about page
    $(".about-page-projects-link")
      .shouldBe(visible)
      .shouldHave(text("1"))
      .click();

    // on projects page
    assertThat(url()).contains("/projects");
    $(".project-card-name")
      .shouldBe(visible)
      .shouldHave(text("Sample"))
      .find("a")
      .click();

    // on project dashboard
    assertThat(url()).contains("/dashboard?id=sample");
    $(".overview-quality-gate")
      .shouldBe(visible)
      .shouldHave(text("Passed"));
    $("a[href=\"/project/issues?id=sample&resolved=false&types=CODE_SMELL\"]")
      .shouldBe(visible)
      .shouldHave(text("0"))
      .click();

    // on project issues page
    assertThat(url()).contains("/project/issues?id=sample&resolved=false&types=CODE_SMELL");
    $$(".facet.active").shouldHaveSize(2).find(text("Code Smell")).should(exist);

    $("#global-navigation").find("a[href=\"/profiles\"]").click();

    // on quality profiles page
    assertThat(url()).contains("/profiles");
    $("table[data-language=xoo]").find("tr[data-name=Basic]").find(".quality-profiles-table-name")
      .shouldBe(visible)
      .shouldHave(text("Basic"))
      .find("a")
      .click();

    // on profile page
    assertThat(url()).contains("/profiles/show");
    $(".quality-profile-inheritance")
      .shouldBe(visible)
      .shouldHave(text("active rules"));
  }

  @Test
  public void markdown_help() {
    String tags[] = {"strong", "a", "ul", "ol", "h1", "code", "pre", "blockquote"};

    tester.openBrowser().open("/markdown/help");
    for (String tag : tags) {
      $(tag).shouldBe(visible);
    }
  }

  private static void analyzeXooSample() {
    orchestrator.executeBuild(SonarScanner.create(projectDir("shared/xoo-sample")));
  }
}
