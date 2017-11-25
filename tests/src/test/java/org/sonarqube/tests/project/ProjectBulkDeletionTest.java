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

package org.sonarqube.tests.project;

import com.sonar.orchestrator.Orchestrator;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.ws.Organizations;
import org.sonarqube.ws.Projects.CreateWsResponse;
import org.sonarqube.ws.Projects.SearchWsResponse.Component;
import org.sonarqube.ws.client.project.SearchRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.runProjectAnalysis;

public class ProjectBulkDeletionTest {

  @ClassRule
  public static Orchestrator orchestrator = ProjectSuite.ORCHESTRATOR;

  @Rule
  public Tester tester = new Tester(orchestrator);

  @Test
  public void delete_projects() {
    Organizations.Organization organization = tester.organizations().generate();
    CreateWsResponse.Project firstProvisionedProject = tester.projects().provision(organization, p -> p.setKey("first-provisioned-project"));
    CreateWsResponse.Project secondProvisionedProject = tester.projects().provision(organization, p -> p.setKey("second-provisioned-project"));
    CreateWsResponse.Project analyzedProject = tester.projects().provision(organization);

    analyzeProject(analyzedProject.getKey(), organization.getKey());

    tester.wsClient().projects().bulkDelete(SearchRequest.builder()
      .setOrganization(organization.getKey())
      .setQuery("FIRST-PROVISIONED")
      .setOnProvisionedOnly(true).build());

    List<Component> projects = tester.wsClient().projects().search(SearchRequest.builder().setOrganization(organization.getKey()).build()).getComponentsList();
    assertThat(projects).extracting(Component::getKey)
      .containsExactlyInAnyOrder(analyzedProject.getKey(), secondProvisionedProject.getKey())
      .doesNotContain(firstProvisionedProject.getKey());
  }

  @Test
  public void delete_more_than_50_projects_at_the_same_time() {
    Organizations.Organization organization = tester.organizations().generate();
    IntStream.range(0, 60).forEach(i -> tester.projects().provision(organization));
    SearchRequest request = SearchRequest.builder().setOrganization(organization.getKey()).build();
    assertThat(tester.wsClient().projects().search(request).getPaging().getTotal()).isEqualTo(60);

    tester.wsClient().projects().bulkDelete(request);

    assertThat(tester.wsClient().projects().search(request).getComponentsList()).isEmpty();
    assertThat(tester.wsClient().projects().search(request).getPaging().getTotal()).isEqualTo(0);
  }

  private void analyzeProject(String projectKey, String organizationKey) {
    runProjectAnalysis(orchestrator, "shared/xoo-sample",
      "sonar.organization", organizationKey,
      "sonar.projectKey", projectKey,
      "sonar.login", "admin",
      "sonar.password", "admin");
  }
}
