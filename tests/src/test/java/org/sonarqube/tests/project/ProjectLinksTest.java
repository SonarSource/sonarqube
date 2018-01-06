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
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.qa.util.pageobjects.ProjectLinkItem;
import org.sonarqube.qa.util.pageobjects.ProjectLinksPage;
import org.sonarqube.ws.ProjectLinks.CreateWsResponse;
import org.sonarqube.ws.client.projectlinks.CreateRequest;
import org.sonarqube.ws.client.projectlinks.DeleteRequest;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static util.ItUtils.projectDir;

public class ProjectLinksTest {

  @ClassRule
  public static Orchestrator orchestrator = ProjectSuite.ORCHESTRATOR;

  private static Tester tester = new Tester(orchestrator);

  @ClassRule
  public static RuleChain ruleChain = RuleChain.outerRule(orchestrator).around(tester);

  private long customLinkId;
  private String adminUser;

  @BeforeClass
  public static void setUp() {
    orchestrator.executeBuild(
      SonarScanner.create(projectDir("shared/xoo-sample"))
        .setProperty("sonar.links.homepage", "http://example.com"));
  }

  @Before
  public void prepare() {
    customLinkId = Long.parseLong(createCustomLink().getLink().getId());
    adminUser = tester.users().generateAdministratorOnDefaultOrganization().getLogin();
  }

  @After
  public void clean() {
    deleteLink(customLinkId);
  }

  @Test
  public void should_list_links() {
    ProjectLinksPage page = openLinksPage();

    page.getLinks().shouldHaveSize(2);

    List<ProjectLinkItem> links = page.getLinksAsItems();
    ProjectLinkItem homepageLink = links.get(0);
    ProjectLinkItem customLink = links.get(1);

    homepageLink.getName().should(text("Home"));
    homepageLink.getType().should(text("sonar.links.homepage"));
    homepageLink.getUrl().should(text("http://example.com"));
    homepageLink.getDeleteButton().shouldNot(Condition.exist);

    customLink.getName().should(text("Custom"));
    customLink.getType().shouldNot(Condition.exist);
    customLink.getUrl().should(text("http://example.org/custom"));
    customLink.getDeleteButton().shouldBe(Condition.visible);
  }

  @Test
  public void should_create_link() {
    ProjectLinksPage page = openLinksPage();

    page.getLinks().shouldHaveSize(2);

    $("#create-project-link").click();
    $("#create-link-name").setValue("Test");
    $("#create-link-url").setValue("http://example.com/test");
    $("#create-link-confirm").click();

    page.getLinks().shouldHaveSize(3);

    ProjectLinkItem testLink = page.getLinksAsItems().get(2);

    testLink.getName().should(text("Test"));
    testLink.getType().shouldNot(Condition.exist);
    testLink.getUrl().should(text("http://example.com/test"));
    testLink.getDeleteButton().shouldBe(Condition.visible);
  }

  @Test
  public void should_delete_link() {
    ProjectLinksPage page = openLinksPage();

    page.getLinks().shouldHaveSize(2);

    List<ProjectLinkItem> links = page.getLinksAsItems();
    ProjectLinkItem customLink = links.get(1);

    customLink.getDeleteButton().click();
    $("#delete-link-confirm")
      .shouldBe(Condition.visible)
      .click();

    page.getLinks().shouldHaveSize(1);
  }

  private CreateWsResponse createCustomLink() {
    return tester.wsClient().projectLinks().create(new CreateRequest()
      .setProjectKey("sample")
      .setName("Custom")
      .setUrl("http://example.org/custom"));
  }

  private void deleteLink(long id) {
    try {
      tester.wsClient().projectLinks().delete(new DeleteRequest().setId("" + id));
    } catch (Exception e) {
      // fail silently
    }
  }

  private ProjectLinksPage openLinksPage() {
    return tester
      .openBrowser()
      .logIn()
      .submitCredentials(adminUser, adminUser)
      .openProjectLinks("sample");
  }
}
