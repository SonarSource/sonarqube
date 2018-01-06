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

import com.sonar.orchestrator.Orchestrator;
import java.util.Date;
import org.apache.commons.lang.time.DateUtils;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.ws.Organizations;
import org.sonarqube.ws.Projects.CreateWsResponse;
import org.sonarqube.ws.Projects.SearchWsResponse;
import org.sonarqube.ws.Projects.SearchWsResponse.Component;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.projects.SearchRequest;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.formatDate;
import static util.ItUtils.runProjectAnalysis;

public class ProjectSearchTest {

  @ClassRule
  public static Orchestrator orchestrator = ProjectSuite.ORCHESTRATOR;

  @Rule
  public Tester tester = new Tester(orchestrator);

  @Test
  public void search_old_projects() {
    Organizations.Organization organization = tester.organizations().generate();
    CreateWsResponse.Project oldProject = tester.projects().provision(organization);
    CreateWsResponse.Project recentProject = tester.projects().provision(organization);
    Date now = new Date();
    Date oneYearAgo = DateUtils.addDays(now, -365);
    Date moreThanOneYearAgo = DateUtils.addDays(now, -366);

    analyzeProject(oldProject.getKey(), moreThanOneYearAgo, organization.getKey());
    analyzeProject(recentProject.getKey(), now, organization.getKey());

    SearchWsResponse result = tester.wsClient().projects().search(new SearchRequest()
      .setOrganization(organization.getKey())
      .setQualifiers(singletonList("TRK"))
      .setAnalyzedBefore(formatDate(oneYearAgo)));

    assertThat(result.getComponentsList()).extracting(Component::getKey).containsExactlyInAnyOrder(oldProject.getKey());
  }

  @Test
  public void search_on_key_query_partial_match_case_insensitive() {
    Organizations.Organization organization = tester.organizations().generate();
    CreateWsResponse.Project lowerCaseProject = tester.projects().provision(organization, p -> p.setProject("project-key"));
    CreateWsResponse.Project upperCaseProject = tester.projects().provision(organization, p -> p.setProject("PROJECT-KEY"));
    CreateWsResponse.Project anotherProject = tester.projects().provision(organization, p -> p.setProject("another-project"));

    analyzeProject(lowerCaseProject.getKey(), organization.getKey());
    analyzeProject(upperCaseProject.getKey(), organization.getKey());
    analyzeProject(anotherProject.getKey(), organization.getKey());

    SearchWsResponse result = tester.wsClient().projects().search(new SearchRequest()
      .setOrganization(organization.getKey())
      .setQualifiers(singletonList("TRK"))
      .setQ("JeCt-K"));

    assertThat(result.getComponentsList()).extracting(Component::getKey)
      .containsExactlyInAnyOrder(lowerCaseProject.getKey(), upperCaseProject.getKey())
      .doesNotContain(anotherProject.getKey());
  }

  @Test
  public void search_provisioned_projects() {
    Organizations.Organization organization = tester.organizations().generate();
    CreateWsResponse.Project firstProvisionedProject = tester.projects().provision(organization);
    CreateWsResponse.Project secondProvisionedProject = tester.projects().provision(organization);
    CreateWsResponse.Project analyzedProject = tester.projects().provision(organization);

    analyzeProject(analyzedProject.getKey(), organization.getKey());

    String result = tester.wsClient().wsConnector().call(new GetRequest("api/projects/provisioned")
      .setParam("organization", organization.getKey()))
      .failIfNotSuccessful().content();
   SearchWsResponse searchResult = tester.wsClient().projects().search(new SearchRequest()
     .setQualifiers(singletonList("TRK"))
     .setOrganization(organization.getKey())
     .setOnProvisionedOnly("true"));

    assertThat(result).contains(firstProvisionedProject.getKey(), secondProvisionedProject.getKey()).doesNotContain(analyzedProject.getKey());
    assertThat(searchResult.getComponentsList()).extracting(Component::getKey)
      .containsOnly(firstProvisionedProject.getKey(), secondProvisionedProject.getKey())
      .doesNotContain(analyzedProject.getKey());
  }

  private void analyzeProject(String projectKey, Date analysisDate, String organizationKey) {
    runProjectAnalysis(orchestrator, "shared/xoo-sample",
      "sonar.organization", organizationKey,
      "sonar.projectKey", projectKey,
      "sonar.projectDate", formatDate(analysisDate),
      "sonar.login", "admin",
      "sonar.password", "admin");
  }

  private void analyzeProject(String projectKey, String organizationKey) {
    analyzeProject(projectKey, new Date(), organizationKey);
  }
}
