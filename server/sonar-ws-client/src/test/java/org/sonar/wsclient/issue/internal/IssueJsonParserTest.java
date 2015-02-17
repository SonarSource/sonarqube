/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.wsclient.issue.internal;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.sonar.wsclient.base.Paging;
import org.sonar.wsclient.component.Component;
import org.sonar.wsclient.issue.*;
import org.sonar.wsclient.user.User;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class IssueJsonParserTest {

  @Test
  public void test_GET_search() throws Exception {
    String json = IOUtils.toString(getClass().getResourceAsStream("/org/sonar/wsclient/issue/internal/IssueJsonParserTest/search.json"));
    Issues issues = new IssueJsonParser().parseIssues(json);
    assertThat(issues).isNotNull();
    List<Issue> list = issues.list();
    assertThat(list).hasSize(2);
    Issue first = list.get(0);
    assertThat(first.key()).isEqualTo("ABCDE");
    assertThat(first.componentKey()).isEqualTo("Action.java");
    assertThat(first.componentId()).isEqualTo(5L);
    assertThat(first.projectKey()).isEqualTo("struts");
    assertThat(first.ruleKey()).isEqualTo("squid:CycleBetweenPackages");
    assertThat(first.severity()).isEqualTo("CRITICAL");
    assertThat(first.line()).isEqualTo(10);
    assertThat(first.resolution()).isEqualTo("FIXED");
    assertThat(first.status()).isEqualTo("OPEN");
    assertThat(first.assignee()).isEqualTo("karadoc");
    assertThat(first.message()).isEqualTo("the message");
    assertThat(first.debt()).isNull();
    assertThat(first.reporter()).isEqualTo("perceval");
    assertThat(first.author()).isEqualTo("pirlouis");
    assertThat(first.actionPlan()).isEqualTo("9450b10c-e725-48b8-bf01-acdec751c491");
    assertThat(first.creationDate()).isNotNull();
    assertThat(first.updateDate()).isNotNull();
    assertThat(first.closeDate()).isNotNull();
    assertThat(first.attribute("JIRA")).isEqualTo("FOO-1234");
    assertThat(first.attribute("OTHER")).isNull();
    assertThat(first.attributes()).hasSize(1);
    assertThat(first.comments()).isEmpty();

    Issue second = list.get(1);
    assertThat(second.key()).isEqualTo("FGHIJ");
    assertThat(second.line()).isNull();
    assertThat(second.debt()).isNull();
    assertThat(second.reporter()).isNull();
    assertThat(second.author()).isNull();
    assertThat(second.attribute("JIRA")).isNull();
    assertThat(second.attributes()).isEmpty();
    assertThat(second.comments()).isEmpty();

    assertThat(issues.rules()).hasSize(2);
    assertThat(issues.rule(first).key()).isEqualTo("squid:CycleBetweenPackages");
    assertThat(issues.rule(first).name()).isEqualTo("Avoid cycle between java packages");
    assertThat(issues.rule(first).description()).contains("When several packages");

    assertThat(issues.paging()).isNotNull();
    Paging paging = issues.paging();
    assertThat(paging.pageIndex()).isEqualTo(1);
    assertThat(paging.pageSize()).isEqualTo(100);
    assertThat(paging.pages()).isEqualTo(1);
    assertThat(paging.total()).isEqualTo(2);
  }

  @Test
  public void test_GET_empty_search() throws Exception {
    String json = IOUtils.toString(getClass().getResourceAsStream("/org/sonar/wsclient/issue/internal/IssueJsonParserTest/empty.json"));
    Issues issues = new IssueJsonParser().parseIssues(json);
    assertThat(issues).isNotNull();
    assertThat(issues.list()).isEmpty();
    assertThat(issues.rules()).isEmpty();
  }

  @Test
  public void test_GET_transitions() throws Exception {
    String json = IOUtils.toString(getClass().getResourceAsStream("/org/sonar/wsclient/issue/internal/IssueJsonParserTest/getTransitions.json"));
    List<String> transitions = new IssueJsonParser().parseTransitions(json);

    assertThat(transitions).isNotNull();
    assertThat(transitions).hasSize(2);
    assertThat(transitions).containsOnly("resolve", "falsepositive");
  }

  @Test
  public void parse_comments() throws Exception {
    String json = IOUtils.toString(getClass().getResourceAsStream("/org/sonar/wsclient/issue/internal/IssueJsonParserTest/issue-with-comments.json"));
    Issues issues = new IssueJsonParser().parseIssues(json);
    assertThat(issues.size()).isEqualTo(1);

    Issue issue = issues.list().get(0);
    assertThat(issue.comments()).hasSize(2);

    IssueComment firstComment = issue.comments().get(0);
    assertThat(firstComment.key()).isEqualTo("COMMENT-1");
    assertThat(firstComment.login()).isEqualTo("morgan");
    assertThat(firstComment.htmlText()).isEqualTo("the first comment");
    assertThat(firstComment.createdAt().getDate()).isEqualTo(18);

    IssueComment secondComment = issue.comments().get(1);
    assertThat(secondComment.key()).isEqualTo("COMMENT-2");
    assertThat(secondComment.login()).isEqualTo("arthur");
    assertThat(secondComment.htmlText()).isEqualTo("the second comment");
    assertThat(secondComment.createdAt().getDate()).isEqualTo(19);
  }

  @Test
  public void parse_users() throws Exception {
    String json = IOUtils.toString(getClass().getResourceAsStream("/org/sonar/wsclient/issue/internal/IssueJsonParserTest/issue-with-users.json"));
    Issues issues = new IssueJsonParser().parseIssues(json);

    assertThat(issues.users()).hasSize(2);

    User morgan = issues.user("morgan");
    assertThat(morgan.login()).isEqualTo("morgan");
    assertThat(morgan.name()).isEqualTo("Morgan");
    assertThat(morgan.active()).isTrue();
    assertThat(morgan.email()).isEqualTo("mor@gan.bzh");

    User arthur = issues.user("arthur");
    assertThat(arthur.login()).isEqualTo("arthur");
    assertThat(arthur.name()).isEqualTo("Arthur");
    assertThat(arthur.active()).isFalse();
    assertThat(arthur.email()).isEqualTo("ar@thur.bzh");
  }

  @Test
  public void parse_components() throws Exception {
    String json = IOUtils.toString(getClass().getResourceAsStream("/org/sonar/wsclient/issue/internal/IssueJsonParserTest/issue-with-components.json"));
    Issues issues = new IssueJsonParser().parseIssues(json);

    assertThat(issues.components()).hasSize(1);

    Component component = issues.component(issues.list().get(0));
    assertThat(component.key()).isEqualTo("struts:Action.java");
    assertThat(component.id()).isEqualTo(10L);
    assertThat(component.qualifier()).isEqualTo("CLA");
    assertThat(component.name()).isEqualTo("Action");
    assertThat(component.longName()).isEqualTo("org.struts.Action");
    assertThat(component.subProjectId()).isEqualTo(2L);
    assertThat(component.projectId()).isEqualTo(1L);

    assertThat(issues.componentByKey("struts:Action.java").key()).isEqualTo("struts:Action.java");
    assertThat(issues.componentById(10).key()).isEqualTo("struts:Action.java");
  }

  @Test
  public void parse_projects() throws Exception {
    String json = IOUtils.toString(getClass().getResourceAsStream("/org/sonar/wsclient/issue/internal/IssueJsonParserTest/issue-with-projects.json"));
    Issues issues = new IssueJsonParser().parseIssues(json);

    assertThat(issues.projects()).hasSize(1);

    Component component = issues.project(issues.list().get(0));
    assertThat(component.key()).isEqualTo("struts");
    assertThat(component.qualifier()).isEqualTo("TRK");
    assertThat(component.name()).isEqualTo("Struts");
    assertThat(component.longName()).isEqualTo("org.struts");
  }

  @Test
  public void parse_action_plans() throws Exception {
    String json = IOUtils.toString(getClass().getResourceAsStream("/org/sonar/wsclient/issue/internal/IssueJsonParserTest/issue-with-action-plans.json"));
    Issues issues = new IssueJsonParser().parseIssues(json);

    assertThat(issues.actionPlans()).hasSize(1);

    ActionPlan actionPlan = issues.actionPlans(issues.list().get(0));
    assertThat(actionPlan.key()).isEqualTo("9450b10c-e725-48b8-bf01-acdec751c491");
    assertThat(actionPlan.name()).isEqualTo("3.6");
    assertThat(actionPlan.status()).isEqualTo("OPEN");
    assertThat(actionPlan.project()).isEqualTo("struts");
    assertThat(actionPlan.deadLine().getTime()).isEqualTo(1369951200000l);
    assertThat(actionPlan.createdAt().getTime()).isEqualTo(1369828520000l);
    assertThat(actionPlan.updatedAt().getTime()).isEqualTo(1369828520000l);
  }

  @Test
  public void parse_technical_debt() throws Exception {
    String json = IOUtils.toString(getClass().getResourceAsStream("/org/sonar/wsclient/issue/internal/IssueJsonParserTest/issue-with-technical-debt.json"));
    Issues issues = new IssueJsonParser().parseIssues(json);
    assertThat(issues.size()).isEqualTo(1);

    Issue issue = issues.list().get(0);
    assertThat(issue.debt()).isEqualTo("3d10min");
  }

  @Test
  public void parse_changelog() throws Exception {
    String json = IOUtils.toString(getClass().getResourceAsStream("/org/sonar/wsclient/issue/internal/IssueJsonParserTest/changelog.json"));
    List<IssueChange> changes = new IssueJsonParser().parseChangelog(json);

    assertThat(changes).hasSize(2);
    IssueChange change1 = changes.get(0);
    assertThat(change1.user()).isEqualTo("julien");
    assertThat(change1.creationDate().getTime()).isEqualTo(1383202235000l);
    assertThat(change1.diffs()).hasSize(1);
    IssueChangeDiff diffChange1 = change1.diffs().get(0);
    assertThat(diffChange1.key()).isEqualTo("actionPlan");
    assertThat(diffChange1.newValue()).isEqualTo("1.0");
    assertThat(diffChange1.oldValue()).isNull();

    IssueChange change2 = changes.get(1);
    assertThat(change2.user()).isEqualTo("simon");
    assertThat(change2.creationDate().getTime()).isEqualTo(1383202239000l);
    assertThat(change2.diffs()).hasSize(2);
    IssueChangeDiff diff1Change2 = change2.diffs().get(0);
    assertThat(diff1Change2.key()).isEqualTo("severity");
    assertThat(diff1Change2.newValue()).isEqualTo("INFO");
    assertThat(diff1Change2.oldValue()).isEqualTo("BLOCKER");
    IssueChangeDiff diff2Change2 = change2.diffs().get(1);
    assertThat(diff2Change2.key()).isEqualTo("status");
    assertThat(diff2Change2.newValue()).isEqualTo("REOPEN");
    assertThat(diff2Change2.oldValue()).isEqualTo("RESOLVED");
  }

  @Test
  public void parse_changelog_with_technical_debt() throws Exception {
    String json = IOUtils.toString(getClass().getResourceAsStream("/org/sonar/wsclient/issue/internal/IssueJsonParserTest/changelog-with-technical-debt.json"));
    List<IssueChange> changes = new IssueJsonParser().parseChangelog(json);

    assertThat(changes).hasSize(1);
    IssueChange change = changes.get(0);
    assertThat(change.user()).isEqualTo("julien");
    assertThat(change.creationDate().getTime()).isEqualTo(1383202235000l);

    assertThat(change.diffs()).hasSize(1);
    IssueChangeDiff changeDiff = change.diffs().get(0);
    assertThat(changeDiff.key()).isEqualTo("technicalDebt");
    assertThat(changeDiff.newValue()).isEqualTo("2d1h");
    assertThat(changeDiff.oldValue()).isEqualTo("3d10min");
  }

  @Test
  public void parse_changelog_with_only_new_technical_debt() throws Exception {
    String json = IOUtils.toString(getClass().getResourceAsStream("/org/sonar/wsclient/issue/internal/IssueJsonParserTest/changelog-with-only-new-technical-debt.json"));
    List<IssueChange> changes = new IssueJsonParser().parseChangelog(json);

    assertThat(changes).hasSize(1);
    IssueChange change = changes.get(0);
    assertThat(change.user()).isEqualTo("julien");
    assertThat(change.creationDate().getTime()).isEqualTo(1383202235000l);

    assertThat(change.diffs()).hasSize(1);
    IssueChangeDiff changeDiff = change.diffs().get(0);
    assertThat(changeDiff.key()).isEqualTo("technicalDebt");
    assertThat(changeDiff.newValue()).isEqualTo("2d1h");
    assertThat(changeDiff.oldValue()).isNull();
  }

  @Test
  public void parse_bulk_change() throws Exception {
    String json = IOUtils.toString(getClass().getResourceAsStream("/org/sonar/wsclient/issue/internal/IssueJsonParserTest/bulk-change.json"));
    BulkChange bulkChange = new IssueJsonParser().parseBulkChange(json);

    assertThat(bulkChange.totalIssuesChanged()).isEqualTo(3);
    assertThat(bulkChange.totalIssuesNotChanged()).isEqualTo(2);
    assertThat(bulkChange.issuesNotChangedKeys()).containsOnly("06ed4db6-fd96-450a-bcb0-e0184db50105", "06ed4db6-fd96-450a-bcb0-e0184db50654");
  }
}
