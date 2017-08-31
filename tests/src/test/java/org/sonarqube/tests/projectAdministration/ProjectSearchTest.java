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

import com.sonar.orchestrator.Orchestrator;
import java.util.Date;
import org.apache.commons.lang.time.DateUtils;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.tests.Category6Suite;
import org.sonarqube.tests.Tester;
import org.sonarqube.ws.Organizations;
import org.sonarqube.ws.WsProjects.CreateWsResponse;
import org.sonarqube.ws.WsProjects.SearchWsResponse;
import org.sonarqube.ws.client.project.SearchWsRequest;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.formatDate;
import static util.ItUtils.runProjectAnalysis;

public class ProjectSearchTest {

  @ClassRule
  public static Orchestrator orchestrator = Category6Suite.ORCHESTRATOR;
  @Rule
  public Tester tester = new Tester(orchestrator);

  @Test
  public void search_old_projects() {
    Organizations.Organization organization = tester.organizations().generate();
    CreateWsResponse.Project oldProject = tester.projects().generate(organization);
    CreateWsResponse.Project recentProject = tester.projects().generate(organization);
    Date now = new Date();
    Date oneYearAgo = DateUtils.addDays(now, -365);
    Date moreThanOneYearAgo = DateUtils.addDays(now, -366);

    analyzeProject(oldProject.getKey(), moreThanOneYearAgo, organization.getKey());
    analyzeProject(recentProject.getKey(), now, organization.getKey());

    SearchWsResponse result = tester.wsClient().projects().search(SearchWsRequest.builder()
      .setOrganization(organization.getKey())
      .setQualifiers(singletonList("TRK"))
      .setAnalyzedBefore(formatDate(oneYearAgo)).build());

    assertThat(result.getComponentsList()).extracting(SearchWsResponse.Component::getKey).containsExactlyInAnyOrder(oldProject.getKey());
  }

  @Test
  public void search_on_key_partial_match_case_insensitive() {
    Organizations.Organization organization = tester.organizations().generate();
    CreateWsResponse.Project lowerCaseProject = tester.projects().generate(organization, p -> p.setKey("project-key"));
    CreateWsResponse.Project upperCaseProject = tester.projects().generate(organization, p -> p.setKey("PROJECT-KEY"));
    CreateWsResponse.Project anotherProject = tester.projects().generate(organization, p -> p.setKey("another-project"));

    analyzeProject(lowerCaseProject.getKey(), organization.getKey());
    analyzeProject(upperCaseProject.getKey(), organization.getKey());
    analyzeProject(anotherProject.getKey(), organization.getKey());

    SearchWsResponse result = tester.wsClient().projects().search(SearchWsRequest.builder()
      .setOrganization(organization.getKey())
      .setQualifiers(singletonList("TRK"))
      .setQuery("JeCt-K")
      .build());

    assertThat(result.getComponentsList()).extracting(SearchWsResponse.Component::getKey)
      .containsExactlyInAnyOrder(lowerCaseProject.getKey(), upperCaseProject.getKey())
      .doesNotContain(anotherProject.getKey());
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
