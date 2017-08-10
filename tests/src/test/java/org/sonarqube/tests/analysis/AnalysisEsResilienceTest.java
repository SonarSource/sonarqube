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
package org.sonarqube.tests.analysis;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.SonarScanner;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.tests.Byteman;
import org.sonarqube.tests.Tester;
import org.sonarqube.ws.Issues;
import org.sonarqube.ws.Organizations.Organization;
import org.sonarqube.ws.QualityProfiles.CreateWsResponse.QualityProfile;
import org.sonarqube.ws.WsProjects;
import org.sonarqube.ws.WsUsers.CreateWsResponse.User;
import org.sonarqube.ws.client.component.SuggestionsWsRequest;
import org.sonarqube.ws.client.issue.SearchWsRequest;
import util.ItUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonarqube.tests.Byteman.Process.CE;
import static util.ItUtils.projectDir;

public class AnalysisEsResilienceTest {

  @ClassRule
  public static final Orchestrator orchestrator;
  private static final Byteman byteman;

  static {
    byteman = new Byteman(Orchestrator.builderEnv(), CE);
    orchestrator = byteman
      .getOrchestratorBuilder()
      .addPlugin(ItUtils.xooPlugin())
      .build();
  }

  @Rule
  public Tester tester = new Tester(orchestrator);

  @After
  public void after() throws Exception {
    byteman.deactivateAllRules();
  }

  @Test
  public void activation_and_deactivation_of_rule_is_resilient_to_indexing_errors() throws Exception {
    Organization organization = tester.organizations().generate();
    User orgAdministrator = tester.users().generateAdministrator(organization);
    WsProjects.CreateWsResponse.Project project = tester.projects().generate(organization);
    String projectKey = project.getKey();
    String fileKey = projectKey + ":src/main/xoo/sample/Sample.xoo";
    String file2Key = projectKey + ":src/main/xoo/sample/Sample2.xoo";
    String file3Key = projectKey + ":src/main/xoo/sample/Sample3.xoo";

    QualityProfile profile = tester.qProfiles().createXooProfile(organization);
    tester.qProfiles()
      .activateRule(profile, "xoo:OneIssuePerFile")
      .assignQProfileToProject(profile, project);

    executeAnalysis(projectKey, organization, orgAdministrator, "analysis/resilience/resilience-sample-v1");
    assertThat(searchFile(fileKey, organization)).isNotEmpty();
    assertThat(searchFile(file2Key, organization)).isEmpty();
    assertThat(searchFile(file3Key, organization)).isEmpty();
    List<Issues.Issue> issues = searchIssues(projectKey);
    assertThat(issues)
      .extracting(Issues.Issue::getComponent)
      .containsExactlyInAnyOrder(fileKey);

    byteman.activateScript("resilience/making_ce_indexation_failing.btm");
    executeAnalysis(projectKey, organization, orgAdministrator, "analysis/resilience/resilience-sample-v2");
    assertThat(searchFile(fileKey, organization)).isNotEmpty();
    assertThat(searchFile(file2Key, organization)).isEmpty();// inconsistency: in DB there is also file2Key
    assertThat(searchFile(file3Key, organization)).isEmpty();// inconsistency: in DB there is also file3Key
    issues = searchIssues(projectKey);
    assertThat(issues)
      .extracting(Issues.Issue::getComponent)
      .containsExactlyInAnyOrder(fileKey /* inconsistency: in DB there is also file2Key and file3Key */);
    byteman.deactivateAllRules();

    executeAnalysis(projectKey, organization, orgAdministrator, "analysis/resilience/resilience-sample-v3");
    assertThat(searchFile(fileKey, organization)).isNotEmpty();
    assertThat(searchFile(file2Key, organization)).isEmpty();
    assertThat(searchFile(file3Key, organization)).isNotEmpty();
    issues = searchIssues(projectKey);
    assertThat(issues)
      .extracting(Issues.Issue::getComponent, Issues.Issue::getStatus)
      .containsExactlyInAnyOrder(
        tuple(fileKey, "OPEN"),
        tuple(file2Key, "CLOSED"),
        tuple(file3Key, "OPEN"));
  }

  private List<Issues.Issue> searchIssues(String projectKey) {
    SearchWsRequest request = new SearchWsRequest()
      .setProjectKeys(Collections.singletonList(projectKey));
    Issues.SearchWsResponse results = tester.wsClient().issues().search(request);
    return results.getIssuesList();
  }

  private List<String> searchFile(String key, Organization organization) {
    SuggestionsWsRequest query = SuggestionsWsRequest.builder()
      .setS(key)
      .build();
    Map<String, Object> response = ItUtils.jsonToMap(
      tester.wsClient().components().suggestions(query).content()
    );
    List results = (List) response.get("results");
    Map trkResult = (Map) results.stream().filter(result -> "FIL".equals(((Map) result).get("q"))).findAny().get();
    List items = (List) trkResult.get("items");
    Stream<String> x = items.stream().map(item -> (String) ((Map) item).get("key"));
    return x.collect(Collectors.toList());
  }

  private String executeAnalysis(String projectKey, Organization organization, User orgAdministrator, String projectPath) {
    BuildResult buildResult = orchestrator.executeBuild(SonarScanner.create(projectDir(projectPath),
      "sonar.organization", organization.getKey(),
      "sonar.projectKey", projectKey,
      "sonar.login", orgAdministrator.getLogin(),
      "sonar.password", orgAdministrator.getLogin()));
    return ItUtils.extractCeTaskId(buildResult);
  }

}
