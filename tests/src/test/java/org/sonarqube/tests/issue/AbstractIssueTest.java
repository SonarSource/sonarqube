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
package org.sonarqube.tests.issue;

import com.sonar.orchestrator.Orchestrator;
import java.util.List;
import org.junit.ClassRule;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueClient;
import org.sonar.wsclient.issue.IssueQuery;
import org.sonar.wsclient.issue.Issues;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class AbstractIssueTest {

  @ClassRule
  public static final Orchestrator ORCHESTRATOR = IssueSuite.ORCHESTRATOR;

  static IssueClient adminIssueClient() {
    return ORCHESTRATOR.getServer().adminWsClient().issueClient();
  }

  static IssueClient issueClient() {
    return ORCHESTRATOR.getServer().wsClient().issueClient();
  }

  static Issue searchRandomIssue() {
    List<Issue> issues = searchIssues(IssueQuery.create());
    assertThat(issues).isNotEmpty();
    return issues.get(0);
  }

  static Issues search(IssueQuery issueQuery) {
    issueQuery.urlParams().put("additionalFields", "_all");
    return issueClient().find(issueQuery);
  }

  static List<Issue> searchIssues() {
    return searchIssues(IssueQuery.create());
  }

  static List<Issue> searchIssues(IssueQuery issueQuery) {
    return issueClient().find(issueQuery).list();
  }

  static List<Issue> searchIssuesByProject(String projectKey) {
    return search(IssueQuery.create().componentRoots(projectKey)).list();
  }
}
