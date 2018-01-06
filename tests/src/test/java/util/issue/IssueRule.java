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
package util.issue;

import com.sonar.orchestrator.Orchestrator;
import java.util.List;
import org.junit.rules.ExternalResource;
import org.sonarqube.ws.Issues.Issue;
import org.sonarqube.ws.Issues.SearchWsResponse;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.issues.SearchRequest;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.newAdminWsClient;

public class IssueRule extends ExternalResource {

  private final Orchestrator orchestrator;

  private WsClient adminWsClient;

  private IssueRule(Orchestrator orchestrator) {
    this.orchestrator = orchestrator;
  }

  public static IssueRule from(Orchestrator orchestrator) {
    return new IssueRule(requireNonNull(orchestrator, "Orchestrator instance can not be null"));
  }

  public SearchWsResponse search(SearchRequest request) {
    return adminWsClient().issues().search(request);
  }

  public Issue getRandomIssue() {
    List<Issue> issues = search(new SearchRequest()).getIssuesList();
    assertThat(issues).isNotEmpty();
    return issues.get(0);
  }

  public Issue getByKey(String issueKey) {
    List<Issue> issues = search(new SearchRequest().setIssues(singletonList(issueKey)).setAdditionalFields(singletonList("_all"))).getIssuesList();
    assertThat(issues).hasSize(1);
    return issues.iterator().next();
  }

  public List<Issue> getByKeys(String... issueKeys) {
    List<Issue> issues = search(new SearchRequest().setIssues(asList(issueKeys)).setAdditionalFields(singletonList("_all"))).getIssuesList();
    assertThat(issues).hasSize(issueKeys.length);
    return issues;
  }

  private WsClient adminWsClient() {
    if (adminWsClient == null) {
      adminWsClient = newAdminWsClient(orchestrator);
    }
    return adminWsClient;
  }

}
