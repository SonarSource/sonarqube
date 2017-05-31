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
package it.projectSearch;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import it.Category6Suite;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.organization.CreateWsRequest;
import pageobjects.Navigation;
import pageobjects.projects.ProjectsPage;
import util.ItUtils;

import static com.codeborne.selenide.WebDriverRunner.url;
import static it.Category6Suite.enableOrganizationsSupport;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.newAdminWsClient;
import static util.ItUtils.newProjectKey;
import static util.ItUtils.projectDir;
import static util.ItUtils.restoreProfile;
import static util.ItUtils.setServerProperty;

public class LeakProjectsPageTest {

  @ClassRule
  public static Orchestrator orchestrator = Category6Suite.ORCHESTRATOR;

  @Rule
  public Navigation nav = Navigation.get(orchestrator);

  private static WsClient wsClient;

  private String organizationKey;

  @BeforeClass
  public static void beforeClass() throws Exception {
    enableOrganizationsSupport();
    setServerProperty(orchestrator, "sonar.leak.period", "previous_analysis");
    wsClient = newAdminWsClient(orchestrator);
  }

  @Before
  public void setUp() throws Exception {
    organizationKey = ItUtils.newOrganizationKey();
    wsClient.organizations().create(CreateWsRequest.builder()
      .setKey(organizationKey)
      .setName(organizationKey)
      .build());
    restoreProfile(orchestrator, SearchProjectsTest.class.getResource("/projectSearch/SearchProjectsTest/with-many-rules.xml"), organizationKey);
  }

  @After
  public void tearDown() {
    wsClient.organizations().delete(organizationKey);
  }

  @Test
  public void should_display_leak_information() {
    // This project has 0% duplication on new code
    String projectKey2 = newProjectKey();
    analyzeProject(projectKey2, "projectSearch/xoo-history-v1", "2016-12-31");
    analyzeProject(projectKey2, "projectSearch/xoo-history-v2", null);

    // This project has no duplication on new code
    String projectKey1 = newProjectKey();
    analyzeProject(projectKey1, "shared/xoo-sample", "2016-12-31");
    analyzeProject(projectKey1, "shared/xoo-sample", null);

    // Check the facets and project cards
    ProjectsPage page = nav.logIn().asAdmin().openProjects(organizationKey);
    page.changePerspective("Leak");
    assertThat(url()).endsWith("/projects?view=leak");
    page.shouldHaveTotal(2);
    page.getProjectByKey(projectKey2)
      .shouldHaveMeasure("new_reliability_rating", "0A")
      .shouldHaveMeasure("new_security_rating", "0A")
      .shouldHaveMeasure("new_maintainability_rating", "17A")
      .shouldHaveMeasure("new_coverage", "–")
      .shouldHaveMeasure("new_duplicated_lines_density", "0.0%")
      .shouldHaveMeasure("new_lines", "17");
    page.getFacetByProperty("new_duplications")
      .shouldHaveValue("1", "1")
      .shouldHaveValue("2", "0")
      .shouldHaveValue("3", "0")
      .shouldHaveValue("4", "0")
      .shouldHaveValue("5", "0")
      .shouldHaveValue("6", "1");
  }

  private void analyzeProject(String projectKey, String relativePath, @Nullable String analysisDate) {
    List<String> keyValueProperties = new ArrayList<>(asList(
      "sonar.projectKey", projectKey,
      "sonar.organization", organizationKey,
      "sonar.profile", "with-many-rules",
      "sonar.login", "admin", "sonar.password", "admin",
      "sonar.scm.disabled", "false"));
    if (analysisDate != null) {
      keyValueProperties.add("sonar.projectDate");
      keyValueProperties.add(analysisDate);
    }
    orchestrator.executeBuild(SonarScanner.create(projectDir(relativePath), keyValueProperties.toArray(new String[0])));
  }
}
