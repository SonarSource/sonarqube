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

package org.sonar.server.issue.ws;

import java.io.StringWriter;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.issue.Issue;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.api.web.UserRole;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.workflow.Transition;
import org.sonar.server.issue.IssueService;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.test.JsonAssert;

import static com.google.common.collect.Lists.newArrayList;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class IssueActionsWriterTest {
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  @Mock
  IssueService issueService;

  IssueActionsWriter writer;

  @Before
  public void setUp() {
    writer = new IssueActionsWriter(issueService, userSessionRule);
  }

  @Test
  public void write_all_standard_actions() {
    Issue issue = new DefaultIssue()
      .setKey("ABCD")
      .setComponentUuid("BCDE")
      .setComponentKey("sample:src/main/xoo/sample/Sample.xoo")
      .setProjectUuid("ABCD")
      .setProjectKey("sample")
      .setRuleKey(RuleKey.of("squid", "AvoidCycle"));

    userSessionRule.login("john").addProjectUuidPermissions(UserRole.ISSUE_ADMIN, "ABCD");

    testActions(issue,
      "{\"actions\": " +
        "[" +
        "\"comment\", \"assign\", \"set_tags\", \"assign_to_me\", \"plan\", \"set_severity\"\n" +
        "]}");
  }

  @Test
  public void write_only_comment_action() {
    Issue issue = new DefaultIssue()
      .setKey("ABCD")
      .setComponentKey("sample:src/main/xoo/sample/Sample.xoo")
      .setProjectKey("sample")
      .setRuleKey(RuleKey.of("squid", "AvoidCycle"))
      .setResolution("CLOSED");

    userSessionRule.login("john");

    testActions(issue,
      "{\"actions\": " +
        "[" +
        "\"comment\"" +
        "]}");
  }

  @Test
  public void write_no_action_if_not_logged() {
    Issue issue = new DefaultIssue()
      .setKey("ABCD")
      .setComponentKey("sample:src/main/xoo/sample/Sample.xoo")
      .setProjectKey("sample")
      .setRuleKey(RuleKey.of("squid", "AvoidCycle"));

    testActions(issue,
      "{\"actions\": []}");
  }

  @Test
  public void write_actions_without_assign_to_me() {
    Issue issue = new DefaultIssue()
      .setKey("ABCD")
      .setComponentKey("sample:src/main/xoo/sample/Sample.xoo")
      .setProjectKey("sample")
      .setRuleKey(RuleKey.of("squid", "AvoidCycle"))
      .setAssignee("john");

    userSessionRule.login("john");

    testActions(issue,
      "{\"actions\": " +
        "[" +
        "\"comment\", \"assign\", \"set_tags\", \"plan\"\n" +
        "]}");
  }

  @Test
  public void write_transitions() {
    Issue issue = new DefaultIssue()
      .setKey("ABCD")
      .setComponentKey("sample:src/main/xoo/sample/Sample.xoo")
      .setProjectKey("sample")
      .setRuleKey(RuleKey.of("squid", "AvoidCycle"));

    when(issueService.listTransitions(eq(issue))).thenReturn(newArrayList(Transition.create("reopen", "RESOLVED", "REOPEN")));
    userSessionRule.login("john");

    testTransitions(issue,
      "{\"transitions\": [\n" +
        "        \"reopen\"\n" +
        "      ]}");
  }

  @Test
  public void write_no_transitions() {
    Issue issue = new DefaultIssue()
      .setKey("ABCD")
      .setComponentKey("sample:src/main/xoo/sample/Sample.xoo")
      .setProjectKey("sample")
      .setRuleKey(RuleKey.of("squid", "AvoidCycle"));

    userSessionRule.login("john");

    testTransitions(issue,
      "{\"transitions\": []}");
  }

  private void testActions(Issue issue, String expected) {
    StringWriter output = new StringWriter();
    JsonWriter jsonWriter = JsonWriter.of(output);
    jsonWriter.beginObject();
    writer.writeActions(issue, jsonWriter);
    jsonWriter.endObject();
    JsonAssert.assertJson(output.toString()).isSimilarTo(expected);
  }

  private void testTransitions(Issue issue, String expected) {
    StringWriter output = new StringWriter();
    JsonWriter jsonWriter = JsonWriter.of(output);
    jsonWriter.beginObject();
    writer.writeTransitions(issue, jsonWriter);
    jsonWriter.endObject();
    JsonAssert.assertJson(output.toString()).isSimilarTo(expected);
  }

}
