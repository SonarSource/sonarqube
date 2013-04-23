/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.batch.issue;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueChange;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Project;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RulePriority;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.workflow.IssueWorkflow;

import java.util.Date;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Mockito.*;

public class ScanIssuesTest {

  IssueCache cache = mock(IssueCache.class);
  IssueWorkflow workflow = mock(IssueWorkflow.class);
  RulesProfile qProfile = mock(RulesProfile.class);
  Project project = mock(Project.class);
  ScanIssues scanIssues = new ScanIssues(qProfile, cache, project, workflow);

  @Test
  public void should_get_issues() throws Exception {
    scanIssues.issues("key");
    verify(cache).componentIssues("key");
  }

  @Test
  public void should_ignore_null_active_rule() throws Exception {
    when(qProfile.getActiveRule(anyString(), anyString())).thenReturn(null);

    DefaultIssue issue = new DefaultIssue().setRuleKey(RuleKey.of("squid", "AvoidCycle"));
    boolean added = scanIssues.initAndAddIssue(issue);

    assertThat(added).isFalse();
    verifyZeroInteractions(cache);
  }

  @Test
  public void should_ignore_null_rule_of_active_rule() throws Exception {
    ActiveRule activeRule = mock(ActiveRule.class);
    when(activeRule.getRule()).thenReturn(null);
    when(qProfile.getActiveRule(anyString(), anyString())).thenReturn(activeRule);

    DefaultIssue issue = new DefaultIssue().setRuleKey(RuleKey.of("squid", "AvoidCycle"));
    boolean added = scanIssues.initAndAddIssue(issue);

    assertThat(added).isFalse();
    verifyZeroInteractions(cache);
  }

  @Test
  public void should_add_issue_to_cache() throws Exception {
    Rule rule = Rule.create("repoKey", "ruleKey");
    ActiveRule activeRule = mock(ActiveRule.class);
    when(activeRule.getRule()).thenReturn(rule);
    when(activeRule.getSeverity()).thenReturn(RulePriority.INFO);
    when(qProfile.getActiveRule(anyString(), anyString())).thenReturn(activeRule);

    Date analysisDate = new Date();
    when(project.getAnalysisDate()).thenReturn(analysisDate);

    DefaultIssue issue = new DefaultIssue().setRuleKey(RuleKey.of("squid", "AvoidCycle")).setSeverity(Severity.CRITICAL);
    boolean added = scanIssues.initAndAddIssue(issue);

    assertThat(added).isTrue();
    ArgumentCaptor<Issue> argument = ArgumentCaptor.forClass(Issue.class);
    verify(cache).addOrUpdate(argument.capture());
    assertThat(argument.getValue().key()).isNotNull();
    assertThat(argument.getValue().severity()).isEqualTo(Severity.CRITICAL);
    assertThat(argument.getValue().createdAt()).isEqualTo(analysisDate);
  }

  @Test
  public void should_use_severity_from_active_rule_if_no_severity() throws Exception {
    Rule rule = Rule.create("repoKey", "ruleKey");
    ActiveRule activeRule = mock(ActiveRule.class);
    when(activeRule.getRule()).thenReturn(rule);
    when(activeRule.getSeverity()).thenReturn(RulePriority.INFO);
    when(qProfile.getActiveRule(anyString(), anyString())).thenReturn(activeRule);

    Date analysisDate = new Date();
    when(project.getAnalysisDate()).thenReturn(analysisDate);

    DefaultIssue issue = new DefaultIssue().setRuleKey(RuleKey.of("squid", "AvoidCycle")).setSeverity(null);
    scanIssues.initAndAddIssue(issue);

    ArgumentCaptor<Issue> argument = ArgumentCaptor.forClass(Issue.class);
    verify(cache).addOrUpdate(argument.capture());
    assertThat(argument.getValue().key()).isNotNull();
    assertThat(argument.getValue().severity()).isEqualTo(Severity.INFO);
    assertThat(argument.getValue().createdAt()).isEqualTo(analysisDate);
  }

  @Test
  public void should_ignore_empty_change() throws Exception {
    Issue issue = new DefaultIssue().setComponentKey("org/struts/Action.java").setKey("ABCDE");
    when(cache.componentIssue("org/struts/Action.java", "ABCDE")).thenReturn(issue);
    Issue changed = scanIssues.change(issue, IssueChange.create());
    verifyZeroInteractions(cache);
    assertThat(changed).isSameAs(issue);
    assertThat(changed.updatedAt()).isNull();
  }

  @Test
  public void unknown_issue_is_a_bad_api_usage() throws Exception {
    Issue issue = new DefaultIssue().setComponentKey("org/struts/Action.java").setKey("ABCDE");
    when(cache.componentIssue("org/struts/Action.java", "ABCDE")).thenReturn(null);
    try {
      scanIssues.change(issue, IssueChange.create().setLine(200));
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Bad API usage. Unregistered issues can't be changed.");
    }
  }

  @Test
  public void should_change_fields() throws Exception {
    DefaultIssue issue = new DefaultIssue().setComponentKey("org/struts/Action.java").setKey("ABCDE");
    when(cache.componentIssue("org/struts/Action.java", "ABCDE")).thenReturn(issue);

    IssueChange change = IssueChange.create().setTransition("resolve");
    scanIssues.change(issue, change);

    verify(cache).addOrUpdate(issue);
    verify(workflow).change(issue, change);
  }
}
