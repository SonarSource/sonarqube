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
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.rules.Violation;
import org.sonar.core.issue.DefaultIssue;

import java.util.Date;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class ScanIssuesTest {

  IssueCache cache = mock(IssueCache.class);
  RulesProfile qProfile = mock(RulesProfile.class);
  Project project = mock(Project.class);
  IssueFilters filters = mock(IssueFilters.class);
  ScanIssues scanIssues = new ScanIssues(qProfile, cache, project, filters);

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
    Rule rule = Rule.create("squid", "AvoidCycle");
    ActiveRule activeRule = mock(ActiveRule.class);
    when(activeRule.getRule()).thenReturn(rule);
    when(activeRule.getSeverity()).thenReturn(RulePriority.INFO);
    when(qProfile.getActiveRule("squid", "AvoidCycle")).thenReturn(activeRule);

    Date analysisDate = new Date();
    when(project.getAnalysisDate()).thenReturn(analysisDate);

    DefaultIssue issue = new DefaultIssue()
      .setKey("ABCDE")
      .setRuleKey(RuleKey.of("squid", "AvoidCycle"))
      .setSeverity(Severity.CRITICAL);
    when(filters.accept(issue, null)).thenReturn(true);

    boolean added = scanIssues.initAndAddIssue(issue);

    assertThat(added).isTrue();
    ArgumentCaptor<DefaultIssue> argument = ArgumentCaptor.forClass(DefaultIssue.class);
    verify(cache).put(argument.capture());
    assertThat(argument.getValue().severity()).isEqualTo(Severity.CRITICAL);
    assertThat(argument.getValue().creationDate()).isEqualTo(analysisDate);
  }

  @Test
  public void should_use_severity_from_active_rule_if_no_severity() throws Exception {
    Rule rule = Rule.create("squid", "AvoidCycle");
    ActiveRule activeRule = mock(ActiveRule.class);
    when(activeRule.getRule()).thenReturn(rule);
    when(activeRule.getSeverity()).thenReturn(RulePriority.INFO);
    when(qProfile.getActiveRule("squid", "AvoidCycle")).thenReturn(activeRule);

    Date analysisDate = new Date();
    when(project.getAnalysisDate()).thenReturn(analysisDate);

    DefaultIssue issue = new DefaultIssue().setRuleKey(RuleKey.of("squid", "AvoidCycle")).setSeverity(null);
    when(filters.accept(issue, null)).thenReturn(true);
    scanIssues.initAndAddIssue(issue);

    ArgumentCaptor<DefaultIssue> argument = ArgumentCaptor.forClass(DefaultIssue.class);
    verify(cache).put(argument.capture());
    assertThat(argument.getValue().severity()).isEqualTo(Severity.INFO);
    assertThat(argument.getValue().creationDate()).isEqualTo(analysisDate);
  }

  @Test
  public void should_add_deprecated_violation() throws Exception {
    Rule rule = Rule.create("squid", "AvoidCycle");
    Resource resource = new JavaFile("org.struts.Action").setEffectiveKey("struts:org.struts.Action");
    Violation violation = new Violation(rule, resource);
    violation.setLineId(42);
    violation.setSeverity(RulePriority.CRITICAL);
    violation.setMessage("the message");

    ActiveRule activeRule = mock(ActiveRule.class);
    when(activeRule.getRule()).thenReturn(rule);
    when(activeRule.getSeverity()).thenReturn(RulePriority.INFO);
    when(qProfile.getActiveRule("squid", "AvoidCycle")).thenReturn(activeRule);
    when(filters.accept(any(DefaultIssue.class), eq(violation))).thenReturn(true);

    boolean added = scanIssues.initAndAddViolation(violation);
    assertThat(added).isTrue();

    ArgumentCaptor<DefaultIssue> argument = ArgumentCaptor.forClass(DefaultIssue.class);
    verify(cache).put(argument.capture());
    DefaultIssue issue = argument.getValue();
    assertThat(issue.severity()).isEqualTo(Severity.CRITICAL);
    assertThat(issue.line()).isEqualTo(42);
    assertThat(issue.message()).isEqualTo("the message");
    assertThat(issue.key()).isNotEmpty();
    assertThat(issue.ruleKey().toString()).isEqualTo("squid:AvoidCycle");
    assertThat(issue.componentKey().toString()).isEqualTo("struts:org.struts.Action");
  }

  @Test
  public void should_filter_issue() throws Exception {
    Rule rule = Rule.create("squid", "AvoidCycle");
    ActiveRule activeRule = mock(ActiveRule.class);
    when(activeRule.getRule()).thenReturn(rule);
    when(activeRule.getSeverity()).thenReturn(RulePriority.INFO);
    when(qProfile.getActiveRule("squid", "AvoidCycle")).thenReturn(activeRule);

    DefaultIssue issue = new DefaultIssue()
      .setKey("ABCDE")
      .setRuleKey(RuleKey.of("squid", "AvoidCycle"))
      .setSeverity(Severity.CRITICAL);

    when(filters.accept(issue, null)).thenReturn(false);

    boolean added = scanIssues.initAndAddIssue(issue);

    assertThat(added).isFalse();
    verifyZeroInteractions(cache);
  }
}
