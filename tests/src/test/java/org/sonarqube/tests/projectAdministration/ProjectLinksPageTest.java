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
package org.sonarqube.tests.projectAdministration;

import com.codeborne.selenide.Condition;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import org.sonarqube.tests.Category1Suite;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.ws.WsProjectLinks.CreateWsResponse;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.projectlinks.CreateWsRequest;
import org.sonarqube.ws.client.projectlinks.DeleteWsRequest;
import org.sonarqube.pageobjects.Navigation;
import org.sonarqube.pageobjects.ProjectLinkItem;
import org.sonarqube.pageobjects.ProjectLinksPage;
import util.user.UserRule;

import static com.codeborne.selenide.Condition.hasText;
import static com.codeborne.selenide.Selenide.$;
import static util.ItUtils.newAdminWsClient;
import static util.ItUtils.projectDir;

public class ProjectLinksPageTest {

  @ClassRule
  public static Orchestrator ORCHESTRATOR = Category1Suite.ORCHESTRATOR;

  private Navigation nav = Navigation.create(ORCHESTRATOR);

  @Rule
  public UserRule userRule = UserRule.from(ORCHESTRATOR);

  private static WsClient wsClient;
  private long customLinkId;
  private String adminUser;

  @BeforeClass
  public static void setUp() {
    wsClient = newAdminWsClient(ORCHESTRATOR);

    ORCHESTRATOR.resetData();
    ORCHESTRATOR.executeBuild(
      SonarScanner.create(projectDir("shared/xoo-sample"))
        .setProperty("sonar.links.homepage", "http://example.com"));
  }

  @Before
  public void prepare() {
    customLinkId = Long.parseLong(createCustomLink().getLink().getId());
    adminUser = userRule.createAdminUser();
  }

  @After
  public void clean() {
    deleteLink(customLinkId);
  }

  @Test
  public void should_list_links() {
    ProjectLinksPage page = openPage();

    page.getLinks().shouldHaveSize(2);

    List<ProjectLinkItem> links = page.getLinksAsItems();
    ProjectLinkItem homepageLink = links.get(0);
    ProjectLinkItem customLink = links.get(1);

    homepageLink.getName().should(hasText("Home"));
    homepageLink.getType().should(hasText("sonar.links.homepage"));
    homepageLink.getUrl().should(hasText("http://example.com"));
    homepageLink.getDeleteButton().shouldNot(Condition.present);

    customLink.getName().should(hasText("Custom"));
    customLink.getType().shouldNot(Condition.present);
    customLink.getUrl().should(hasText("http://example.org/custom"));
    customLink.getDeleteButton().shouldBe(Condition.visible);
  }

  @Test
  public void should_create_link() {
    ProjectLinksPage page = openPage();

    page.getLinks().shouldHaveSize(2);

    $("#create-project-link").click();
    $("#create-link-name").setValue("Test");
    $("#create-link-url").setValue("http://example.com/test");
    $("#create-link-confirm").click();

    page.getLinks().shouldHaveSize(3);

    ProjectLinkItem testLink = page.getLinksAsItems().get(2);

    testLink.getName().should(hasText("Test"));
    testLink.getType().shouldNot(Condition.present);
    testLink.getUrl().should(hasText("http://example.com/test"));
    testLink.getDeleteButton().shouldBe(Condition.visible);
  }

  @Test
  public void should_delete_link() {
    ProjectLinksPage page = openPage();

    page.getLinks().shouldHaveSize(2);

    List<ProjectLinkItem> links = page.getLinksAsItems();
    ProjectLinkItem customLink = links.get(1);

    customLink.getDeleteButton().click();
    $("#delete-link-confirm").click();

    page.getLinks().shouldHaveSize(1);
  }

  private CreateWsResponse createCustomLink() {
    return wsClient.projectLinks().create(new CreateWsRequest()
      .setProjectKey("sample")
      .setName("Custom")
      .setUrl("http://example.org/custom"));
  }

  private void deleteLink(long id) {
    try {
      wsClient.projectLinks().delete(new DeleteWsRequest().setId(id));
    } catch (Exception e) {
      // fail silently
    }
  }

  private ProjectLinksPage openPage() {
    nav.logIn().submitCredentials(adminUser, adminUser);
    return nav.openProjectLinks("sample");
  }
}
