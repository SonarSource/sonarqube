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

import org.apache.commons.lang.time.DateUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.*;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.internal.WorkDuration;
import org.sonar.batch.debt.RuleDebtCalculator;

import java.util.Calendar;
import java.util.Date;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ModuleIssuesTest {

  static final RuleKey SQUID_RULE_KEY = RuleKey.of("squid", "AvoidCycle");
  static final Rule SQUID_RULE = Rule.create("squid", "AvoidCycle").setName("Avoid Cycle");

  @Mock
  IssueCache cache;

  @Mock
  RulesProfile qProfile;

  @Mock
  Project project;

  @Mock
  IssueFilters filters;

  @Mock
  RuleDebtCalculator technicalDebtCalculator;

  @Mock
  RuleFinder ruleFinder;

  ModuleIssues moduleIssues;

  @Before
  public void setUp() {
    when(project.getAnalysisDate()).thenReturn(new Date());
    when(project.getEffectiveKey()).thenReturn("org.apache:struts-core");

    moduleIssues = new ModuleIssues(qProfile, cache, project, filters, technicalDebtCalculator, ruleFinder);
  }

  @Test
  public void fail_on_unknown_rule() throws Exception {
    when(ruleFinder.findByKey(SQUID_RULE_KEY)).thenReturn(null);
    DefaultIssue issue = new DefaultIssue().setRuleKey(SQUID_RULE_KEY);

    try {
      moduleIssues.initAndAddIssue(issue);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(MessageException.class);
    }

    verifyZeroInteractions(cache);
  }

  @Test
  public void fail_if_rule_has_no_name_and_issue_has_no_message() throws Exception {
    when(ruleFinder.findByKey(SQUID_RULE_KEY)).thenReturn(Rule.create("squid", "AvoidCycle"));
    DefaultIssue issue = new DefaultIssue().setRuleKey(SQUID_RULE_KEY).setMessage("");

    try {
      moduleIssues.initAndAddIssue(issue);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(MessageException.class);
    }

    verifyZeroInteractions(cache);
  }

  @Test
  public void ignore_null_active_rule() throws Exception {
    when(qProfile.getActiveRule(anyString(), anyString())).thenReturn(null);
    when(ruleFinder.findByKey(SQUID_RULE_KEY)).thenReturn(SQUID_RULE);

    DefaultIssue issue = new DefaultIssue().setRuleKey(SQUID_RULE_KEY);
    boolean added = moduleIssues.initAndAddIssue(issue);

    assertThat(added).isFalse();
    verifyZeroInteractions(cache);
  }

  @Test
  public void ignore_null_rule_of_active_rule() throws Exception {
    ActiveRule activeRule = mock(ActiveRule.class);
    when(activeRule.getRule()).thenReturn(null);
    when(qProfile.getActiveRule(anyString(), anyString())).thenReturn(activeRule);
    when(ruleFinder.findByKey(SQUID_RULE_KEY)).thenReturn(SQUID_RULE);

    DefaultIssue issue = new DefaultIssue().setRuleKey(SQUID_RULE_KEY);
    boolean added = moduleIssues.initAndAddIssue(issue);

    assertThat(added).isFalse();
    verifyZeroInteractions(cache);
  }

  @Test
  public void add_issue_to_cache() throws Exception {
    Rule rule = SQUID_RULE;
    ActiveRule activeRule = mock(ActiveRule.class);
    when(activeRule.getRule()).thenReturn(rule);
    when(activeRule.getSeverity()).thenReturn(RulePriority.INFO);
    when(qProfile.getActiveRule("squid", "AvoidCycle")).thenReturn(activeRule);
    when(ruleFinder.findByKey(SQUID_RULE_KEY)).thenReturn(rule);

    Date analysisDate = new Date();
    when(project.getAnalysisDate()).thenReturn(analysisDate);

    DefaultIssue issue = new DefaultIssue()
      .setKey("ABCDE")
      .setRuleKey(SQUID_RULE_KEY)
      .setSeverity(Severity.CRITICAL);
    when(filters.accept(issue, null)).thenReturn(true);

    boolean added = moduleIssues.initAndAddIssue(issue);

    assertThat(added).isTrue();
    ArgumentCaptor<DefaultIssue> argument = ArgumentCaptor.forClass(DefaultIssue.class);
    verify(cache).put(argument.capture());
    assertThat(argument.getValue().severity()).isEqualTo(Severity.CRITICAL);
    assertThat(argument.getValue().creationDate()).isEqualTo(DateUtils.truncate(analysisDate, Calendar.SECOND));
  }

  @Test
  public void use_severity_from_active_rule_if_no_severity() throws Exception {
    Rule rule = SQUID_RULE;
    ActiveRule activeRule = mock(ActiveRule.class);
    when(activeRule.getRule()).thenReturn(rule);
    when(activeRule.getSeverity()).thenReturn(RulePriority.INFO);
    when(qProfile.getActiveRule("squid", "AvoidCycle")).thenReturn(activeRule);
    when(ruleFinder.findByKey(SQUID_RULE_KEY)).thenReturn(rule);

    Date analysisDate = new Date();
    when(project.getAnalysisDate()).thenReturn(analysisDate);

    DefaultIssue issue = new DefaultIssue().setRuleKey(SQUID_RULE_KEY).setSeverity(null);
    when(filters.accept(issue, null)).thenReturn(true);
    moduleIssues.initAndAddIssue(issue);

    ArgumentCaptor<DefaultIssue> argument = ArgumentCaptor.forClass(DefaultIssue.class);
    verify(cache).put(argument.capture());
    assertThat(argument.getValue().severity()).isEqualTo(Severity.INFO);
    assertThat(argument.getValue().creationDate()).isEqualTo(DateUtils.truncate(analysisDate, Calendar.SECOND));
  }

  @Test
  public void use_rule_name_if_no_message() throws Exception {
    Rule rule = SQUID_RULE;
    ActiveRule activeRule = mock(ActiveRule.class);
    when(activeRule.getRule()).thenReturn(rule);
    when(activeRule.getSeverity()).thenReturn(RulePriority.INFO);
    when(qProfile.getActiveRule("squid", "AvoidCycle")).thenReturn(activeRule);
    when(ruleFinder.findByKey(SQUID_RULE_KEY)).thenReturn(rule);

    Date analysisDate = new Date();
    when(project.getAnalysisDate()).thenReturn(analysisDate);

    DefaultIssue issue = new DefaultIssue()
      .setKey("ABCDE")
      .setRuleKey(SQUID_RULE_KEY)
      .setSeverity(Severity.CRITICAL)
      .setMessage("");
    when(filters.accept(issue, null)).thenReturn(true);

    boolean added = moduleIssues.initAndAddIssue(issue);

    assertThat(added).isTrue();
    ArgumentCaptor<DefaultIssue> argument = ArgumentCaptor.forClass(DefaultIssue.class);
    verify(cache).put(argument.capture());
    assertThat(argument.getValue().message()).isEqualTo("Avoid Cycle");
  }

  @Test
  public void add_deprecated_violation() throws Exception {
    Rule rule = SQUID_RULE;
    Resource resource = new JavaFile("org.struts.Action").setEffectiveKey("struts:org.struts.Action");
    Violation violation = new Violation(rule, resource);
    violation.setLineId(42);
    violation.setSeverity(RulePriority.CRITICAL);
    violation.setMessage("the message");

    ActiveRule activeRule = mock(ActiveRule.class);
    when(activeRule.getRule()).thenReturn(rule);
    when(activeRule.getSeverity()).thenReturn(RulePriority.INFO);
    when(qProfile.getActiveRule("squid", "AvoidCycle")).thenReturn(activeRule);
    when(ruleFinder.findByKey(SQUID_RULE_KEY)).thenReturn(rule);
    when(filters.accept(any(DefaultIssue.class), eq(violation))).thenReturn(true);

    boolean added = moduleIssues.initAndAddViolation(violation);
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
  public void filter_issue() throws Exception {
    Rule rule = SQUID_RULE;
    ActiveRule activeRule = mock(ActiveRule.class);
    when(activeRule.getRule()).thenReturn(rule);
    when(activeRule.getSeverity()).thenReturn(RulePriority.INFO);
    when(qProfile.getActiveRule("squid", "AvoidCycle")).thenReturn(activeRule);
    when(ruleFinder.findByKey(SQUID_RULE_KEY)).thenReturn(rule);

    DefaultIssue issue = new DefaultIssue()
      .setKey("ABCDE")
      .setRuleKey(SQUID_RULE_KEY)
      .setSeverity(Severity.CRITICAL);

    when(filters.accept(issue, null)).thenReturn(false);

    boolean added = moduleIssues.initAndAddIssue(issue);

    assertThat(added).isFalse();
    verifyZeroInteractions(cache);
  }

  @Test
  public void set_remediation_cost() throws Exception {
    Rule rule = SQUID_RULE;
    ActiveRule activeRule = mock(ActiveRule.class);
    when(activeRule.getRule()).thenReturn(rule);
    when(activeRule.getSeverity()).thenReturn(RulePriority.INFO);
    when(qProfile.getActiveRule("squid", "AvoidCycle")).thenReturn(activeRule);
    when(ruleFinder.findByKey(SQUID_RULE_KEY)).thenReturn(rule);

    Date analysisDate = new Date();
    when(project.getAnalysisDate()).thenReturn(analysisDate);


    DefaultIssue issue = new DefaultIssue()
      .setKey("ABCDE")
      .setRuleKey(SQUID_RULE_KEY)
      .setSeverity(Severity.CRITICAL);

    WorkDuration debt = WorkDuration.createFromValueAndUnit(10, WorkDuration.UNIT.DAYS, 8);
    when(technicalDebtCalculator.calculateTechnicalDebt(issue.ruleKey(), issue.effortToFix())).thenReturn(debt);
    when(filters.accept(issue, null)).thenReturn(true);

    moduleIssues.initAndAddIssue(issue);

    ArgumentCaptor<DefaultIssue> argument = ArgumentCaptor.forClass(DefaultIssue.class);
    verify(cache).put(argument.capture());
    assertThat(argument.getValue().technicalDebt()).isEqualTo(debt);
  }

}
