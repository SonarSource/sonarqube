/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package it.issue;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueComment;
import org.sonar.wsclient.issue.IssueQuery;
import org.sonar.wsclient.issue.Issues;
import org.sonar.wsclient.issue.NewIssue;
import util.QaOnly;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static util.ItUtils.runProjectAnalysis;
import static util.ItUtils.verifyHttpException;

/**
 * SONAR-4304
 */
@Category(QaOnly.class)
public class ManualIssueTest extends AbstractIssueTest {

  private final static String COMPONENT_KEY = "sample:src/main/xoo/sample/Sample.xoo";

  @Before
  public void before() {
    ORCHESTRATOR.resetData();
    analyzeProject();
    createManualRule();
  }

  @Test
  public void create_manual_issue_through_ws() throws Exception {
    // Create the manual issue
    Issue newIssue = adminIssueClient().create(NewIssue.create().component(COMPONENT_KEY)
      .rule("manual:invalidclassname")
      .line(3)
      .severity("CRITICAL")
      .message("The name 'Sample' is too generic"));
    assertThat(newIssue.key()).isNotNull();
    assertThat(newIssue.creationDate()).isNotNull();
    assertThat(newIssue.updateDate()).isNotNull();
    assertThat(newIssue.ruleKey()).isEqualTo("manual:invalidclassname");
    assertThat(newIssue.line()).isEqualTo(3);
    assertThat(newIssue.severity()).isEqualTo(("CRITICAL"));
    assertThat(newIssue.message()).isEqualTo(("The name 'Sample' is too generic"));
    assertThat(newIssue.status()).isEqualTo("OPEN");
    assertThat(newIssue.resolution()).isNull();
    assertThat(newIssue.reporter()).isEqualTo("admin");

    Issues issues = search(IssueQuery.create().issues(newIssue.key()));
    assertThat(issues.list().get(0).reporter()).isEqualTo("admin");

    // get the detail of the reporter
    assertThat(issues.user("admin").name()).isEqualTo("Administrator");
  }

  @Test
  public void scan_should_keep_manual_issues_open() throws Exception {
    // Create the manual issue
    Issue newIssue = adminIssueClient().create(NewIssue.create().component(COMPONENT_KEY)
      .rule("manual:invalidclassname")
      .line(3)
      .severity("CRITICAL")
      .message("The name 'Sample' is too generic"));
    assertThat(newIssue.key()).isNotNull();
    assertThat(newIssue.creationDate()).isNotNull();
    assertThat(newIssue.updateDate()).isNotNull();

    // the metric 'issues' is not up-to-date yet
    assertThat(searchIssuesByComponent(COMPONENT_KEY)).hasSize(1);

    // re-inspect the project : the issue still exists
    analyzeProject();

    Issue issue = searchIssueByKey(newIssue.key());
    assertThat(issue.ruleKey()).isEqualTo("manual:invalidclassname");
    assertThat(issue.line()).isEqualTo(3);
    assertThat(issue.severity()).isEqualTo(("CRITICAL"));
    assertThat(issue.message()).isEqualTo(("The name 'Sample' is too generic"));
    assertThat(issue.status()).isEqualTo("OPEN");
    assertThat(issue.resolution()).isNull();
    assertThat(issue.reporter()).isEqualTo("admin");
    assertThat(issue.creationDate()).isEqualTo(newIssue.creationDate());
    assertThat(issue.updateDate()).isEqualTo(newIssue.updateDate());
  }

  @Test
  public void scan_should_close_issues_on_deleted_manual_rules() throws Exception {
    // Create another manual rule
    ORCHESTRATOR.getServer().adminWsClient().post("/api/rules/create", ImmutableMap.<String, Object>of(
      "manual_key", "ruletoberemoved",
      "name", "RuleToBeRemoved",
      "markdown_description", "Rule to be removed"
      ));

    // Create the manual issue
    Issue newIssue = adminIssueClient().create(NewIssue.create().component(COMPONENT_KEY)
      .rule("manual:ruletoberemoved")
      .line(3)
      .severity("CRITICAL")
      .message("The name 'Sample' is too generic"));
    assertThat(newIssue.status()).isEqualTo("OPEN");

    // Delete the manual rule (will be in fact disabled in the db, not removed)
    ORCHESTRATOR.getServer().adminWsClient().post("/api/rules/delete", ImmutableMap.<String, Object>of("key", "manual:ruletoberemoved"));

    analyzeProject();
    Issue closedIssue = searchIssueByKey(newIssue.key());
    assertThat(closedIssue.status()).isEqualTo("CLOSED");
    assertThat(closedIssue.resolution()).isEqualTo("REMOVED");
    assertThat(closedIssue.creationDate()).isEqualTo(newIssue.creationDate());
    assertThat(closedIssue.updateDate().before(newIssue.updateDate())).isFalse();
    assertThat(closedIssue.closeDate().before(closedIssue.creationDate())).isFalse();
  }

  @Test
  public void scan_should_close_manual_resolved_issues() throws Exception {
    // Create the manual issue
    Issue newIssue = adminIssueClient().create(NewIssue.create().component(COMPONENT_KEY)
      .rule("manual:invalidclassname")
      .line(3)
      .severity("CRITICAL")
      .message("The name 'Sample' is too generic"));
    assertThat(newIssue.status()).isEqualTo("OPEN");

    // mark issue as resolved
    adminIssueClient().doTransition(newIssue.key(), "resolve");

    analyzeProject();
    Issue closedIssue = searchIssueByKey(newIssue.key());
    assertThat(closedIssue.status()).isEqualTo("CLOSED");
    assertThat(closedIssue.resolution()).isEqualTo("FIXED");
    assertThat(closedIssue.creationDate()).isEqualTo(newIssue.creationDate());
    assertThat(closedIssue.updateDate().before(newIssue.updateDate())).isFalse();
    assertThat(closedIssue.closeDate().before(closedIssue.creationDate())).isFalse();
  }

  @Test
  public void add_comment_to_manual_issue() throws Exception {
    // Create the manual issue
    Issue manualIssue = adminIssueClient().create(NewIssue.create().component(COMPONENT_KEY)
      .rule("manual:invalidclassname")
      .line(3)
      .severity("CRITICAL")
      .message("The name 'Sample' is too generic"));

    // Add a comment on the manual issue
    IssueComment comment = adminIssueClient().addComment(manualIssue.key(), "this is my *comment*");

    // Reload manual issue
    Issue reloaded = searchIssueWithComments(manualIssue.key());

    assertThat(reloaded.comments()).hasSize(1);
    assertThat(reloaded.comments().get(0).key()).isEqualTo(comment.key());
  }

  @Test
  public void resolve_manual_issue() throws Exception {
    // Create the manual issue
    Issue manualIssue = adminIssueClient().create(NewIssue.create().component(COMPONENT_KEY)
      .rule("manual:invalidclassname")
      .line(3)
      .severity("CRITICAL")
      .message("The name 'Sample' is too generic"));

    // Resolve the manual issue
    adminIssueClient().doTransition(manualIssue.key(), "resolve");

    // Check the manual issue is well resolved
    Issue reloaded = searchIssueByKey(manualIssue.key());
    assertThat(reloaded.status()).isEqualTo("RESOLVED");
    assertThat(reloaded.resolution()).isEqualTo("FIXED");
    assertThat(reloaded.creationDate()).isEqualTo(manualIssue.creationDate());
    assertThat(reloaded.updateDate().before(manualIssue.updateDate())).isFalse();

    analyzeProject();

    // Reload after analyse -> manual issue should be closed
    reloaded = searchIssueByKey(manualIssue.key());
    assertThat(reloaded.status()).isEqualTo("CLOSED");
    assertThat(reloaded.resolution()).isEqualTo("FIXED");
    assertThat(reloaded.creationDate()).isEqualTo(manualIssue.creationDate());
    assertThat(reloaded.updateDate().before(manualIssue.updateDate())).isFalse();
    assertThat(reloaded.closeDate()).isNotNull();
    assertThat(reloaded.closeDate().before(reloaded.creationDate())).isFalse();
  }

  @Test
  public void resolve_and_reopen_manual_issue() throws Exception {
    // Create the manual issue
    Issue issue = adminIssueClient().create(NewIssue.create().component(COMPONENT_KEY)
      .rule("manual:invalidclassname")
      .line(3)
      .severity("CRITICAL")
      .message("The name 'Sample' is too generic"));

    // Resolve the manual issue
    adminIssueClient().doTransition(issue.key(), "resolve");

    // Check the manual issue is well resolved
    assertThat(searchIssueByKey(issue.key()).status()).isEqualTo("RESOLVED");

    analyzeProject();
    // Reload after analyse -> manual issue is closed
    assertThat(searchIssueByKey(issue.key()).status()).isEqualTo("CLOSED");

    // Reopen the manual issue
    adminIssueClient().doTransition(issue.key(), "reopen");

    analyzeProject();
    // Reload after analyse -> manual issue is reopened
    Issue reloaded = searchIssueByKey(issue.key());
    assertThat(reloaded.status()).isEqualTo("REOPENED");
    assertThat(reloaded.resolution()).isNull();
    assertThat(reloaded.creationDate()).isEqualTo(issue.creationDate());
    assertThat(reloaded.updateDate().before(issue.updateDate())).isFalse();
  }

  @Test
  public void fail_if_unknown_rule() throws Exception {
    try {
      adminIssueClient().create(NewIssue.create().component(COMPONENT_KEY)
        // this rule does not exist
        .rule("manual:unknown-rule")
        .line(3)
        .severity("CRITICAL")
        .message("The name 'Sample' is too generic"));
      fail();
    } catch (Exception e) {
      verifyHttpException(e, 400);
    }
  }

  @Test
  public void fail_if_missing_rule() throws Exception {
    try {
      adminIssueClient().create(NewIssue.create().component(COMPONENT_KEY)
        .line(3)
        .severity("CRITICAL")
        .message("The name 'Sample' is too generic"));
      fail();
    } catch (Exception e) {
      verifyHttpException(e, 400);
    }
  }

  @Test
  public void fail_if_not_a_manual_rule() throws Exception {
    try {
      adminIssueClient().create(NewIssue.create().component(COMPONENT_KEY)
        // Not a manual rule
        .rule("xoo:OneIssuePerLine")
        .line(3)
        .severity("CRITICAL")
        .message("The name 'Sample' is too generic"));
      fail();
    } catch (Exception e) {
      verifyHttpException(e, 400);
    }
  }

  @Test
  public void fail_if_rule_is_disabled() throws Exception {
    // Create and delete a manual rule
    ORCHESTRATOR.getServer().adminWsClient().post("/api/rules/create", ImmutableMap.<String, Object>of(
      "manual_key", "anotherinvalidclassname",
      "name", "AnotherInvalidClassName",
      "markdown_description", "Another invalid class name"
      ));
    ORCHESTRATOR.getServer().adminWsClient().post("/api/rules/delete", ImmutableMap.<String, Object>of(
      "key", "manual:anotherinvalidclassname"
      ));

    try {
      adminIssueClient().create(NewIssue.create().component(COMPONENT_KEY)
        // This rule is disabled
        .rule("manual:anotherinvalidclassname")
        .line(3)
        .severity("CRITICAL")
        .message("The name 'Sample' is too generic"));
      fail();
    } catch (Exception e) {
      verifyHttpException(e, 400);
    }
  }

  @Test
  public void fail_if_component_does_not_exist() throws Exception {
    try {
      adminIssueClient().create(NewIssue.create().component("unknown component")
        .rule("manual:invalidclassname")
        .line(3)
        .severity("CRITICAL")
        .message("The name 'Sample' is too generic"));
      fail();
    } catch (Exception e) {
      verifyHttpException(e, 400);
    }
  }

  @Test
  public void fail_if_not_logged_in() throws Exception {
    try {
      issueClient().create(NewIssue.create().component("unknown component")
        .rule("manual:invalidclassname")
        .line(3)
        .severity("CRITICAL")
        .message("The name 'Sample' is too generic"));
      fail();
    } catch (Exception e) {
      verifyHttpException(e, 401);
    }
  }

  private static void analyzeProject() {
    // no active rules
    runProjectAnalysis(ORCHESTRATOR, "shared/xoo-sample");
  }

  private static void createManualRule() {
    ORCHESTRATOR.getServer().adminWsClient().post("/api/rules/create", ImmutableMap.<String, Object>of(
      "manual_key", "invalidclassname",
      "name", "InvalidClassName",
      "markdown_description", "Invalid class name"
      ));
  }

  private static List<Issue> searchIssuesByComponent(String componentKey) {
    return search(IssueQuery.create().components(componentKey)).list();
  }

  private static Issue searchIssueWithComments(String issueKey) {
    return searchIssue(issueKey, true);
  }
}
