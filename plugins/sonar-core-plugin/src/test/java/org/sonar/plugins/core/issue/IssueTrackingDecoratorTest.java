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
package org.sonar.plugins.core.issue;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.issue.internal.IssueChangeContext;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.batch.issue.IssueCache;
import org.sonar.core.issue.IssueUpdater;
import org.sonar.core.issue.db.IssueDto;
import org.sonar.core.issue.workflow.IssueWorkflow;
import org.sonar.core.persistence.AbstractDaoTestCase;
import org.sonar.java.api.JavaClass;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class IssueTrackingDecoratorTest extends AbstractDaoTestCase {

  IssueTrackingDecorator decorator;
  IssueCache issueCache = mock(IssueCache.class, RETURNS_MOCKS);
  InitialOpenIssuesStack initialOpenIssues = mock(InitialOpenIssuesStack.class);
  IssueTracking tracking = mock(IssueTracking.class, RETURNS_MOCKS);
  IssueHandlers handlers = mock(IssueHandlers.class);
  IssueWorkflow workflow = mock(IssueWorkflow.class);
  IssueUpdater updater = mock(IssueUpdater.class);
  ResourcePerspectives perspectives = mock(ResourcePerspectives.class);
  RulesProfile profile = mock(RulesProfile.class);
  RuleFinder ruleFinder = mock(RuleFinder.class);

  @Before
  public void init() {
    decorator = new IssueTrackingDecorator(
      issueCache,
      initialOpenIssues,
      tracking,
      handlers,
      workflow,
      updater,
      mock(Project.class),
      perspectives,
      profile,
      ruleFinder);
  }

  @Test
  public void should_execute_on_project() {
    Project project = mock(Project.class);
    assertThat(decorator.shouldExecuteOnProject(project)).isTrue();
  }

  @Test
  public void should_not_be_executed_on_classes_not_methods() throws Exception {
    DecoratorContext context = mock(DecoratorContext.class);
    decorator.decorate(JavaClass.create("org.foo.Bar"), context);
    verifyZeroInteractions(context, issueCache, tracking, handlers, workflow);
  }

  @Test
  public void should_process_open_issues() throws Exception {
    Resource file = new File("Action.java").setEffectiveKey("struts:Action.java").setId(123);
    final DefaultIssue issue = new DefaultIssue();

    // INPUT : one issue, no open issues during previous scan, no filtering
    when(issueCache.byComponent("struts:Action.java")).thenReturn(Arrays.asList(issue));
    List<IssueDto> dbIssues = Collections.emptyList();
    when(initialOpenIssues.selectAndRemove("struts:Action.java")).thenReturn(dbIssues);

    decorator.doDecorate(file);

    // Apply filters, track, apply transitions, notify extensions then update cache
    verify(tracking).track(eq(file), eq(dbIssues), argThat(new ArgumentMatcher<Collection<DefaultIssue>>() {
      @Override
      public boolean matches(Object o) {
        List<DefaultIssue> issues = (List<DefaultIssue>) o;
        return issues.size() == 1 && issues.get(0) == issue;
      }
    }));
    verify(workflow).doAutomaticTransition(eq(issue), any(IssueChangeContext.class));
    verify(handlers).execute(eq(issue), any(IssueChangeContext.class));
    verify(issueCache).put(issue);
  }

  @Test
  public void should_register_unmatched_issues_as_end_of_life() throws Exception {
    // "Unmatched" issues existed in previous scan but not in current one -> they have to be closed
    Resource file = new File("Action.java").setEffectiveKey("struts:Action.java").setId(123);

    // INPUT : one issue existing during previous scan
    IssueDto unmatchedIssue = new IssueDto().setKee("ABCDE").setResolution(null).setStatus("OPEN").setRuleKey_unit_test_only("squid", "AvoidCycle");

    IssueTrackingResult trackingResult = new IssueTrackingResult();
    trackingResult.addUnmatched(unmatchedIssue);

    when(tracking.track(eq(file), anyCollection(), anyCollection())).thenReturn(trackingResult);

    decorator.doDecorate(file);

    verify(workflow, times(1)).doAutomaticTransition(any(DefaultIssue.class), any(IssueChangeContext.class));
    verify(handlers, times(1)).execute(any(DefaultIssue.class), any(IssueChangeContext.class));

    ArgumentCaptor<DefaultIssue> argument = ArgumentCaptor.forClass(DefaultIssue.class);
    verify(issueCache).put(argument.capture());

    DefaultIssue issue = argument.getValue();
    assertThat(issue.key()).isEqualTo("ABCDE");
    assertThat(issue.isNew()).isFalse();
    assertThat(issue.isEndOfLife()).isTrue();
  }

  @Test
  public void manual_issues_should_be_kept_open() throws Exception {
    // "Unmatched" issues existed in previous scan but not in current one -> they have to be closed
    Resource file = new File("Action.java").setEffectiveKey("struts:Action.java").setId(123);

    // INPUT : one issue existing during previous scan
    IssueDto unmatchedIssue = new IssueDto().setKee("ABCDE").setReporter("freddy").setStatus("OPEN").setRuleKey_unit_test_only("manual", "Performance");
    when(ruleFinder.findByKey(RuleKey.of("manual", "Performance"))).thenReturn(new Rule("manual", "Performance"));

    IssueTrackingResult trackingResult = new IssueTrackingResult();
    trackingResult.addUnmatched(unmatchedIssue);

    when(tracking.track(eq(file), anyCollection(), anyCollection())).thenReturn(trackingResult);

    decorator.doDecorate(file);

    verify(workflow, times(1)).doAutomaticTransition(any(DefaultIssue.class), any(IssueChangeContext.class));
    verify(handlers, times(1)).execute(any(DefaultIssue.class), any(IssueChangeContext.class));

    ArgumentCaptor<DefaultIssue> argument = ArgumentCaptor.forClass(DefaultIssue.class);
    verify(issueCache).put(argument.capture());

    DefaultIssue issue = argument.getValue();
    assertThat(issue.key()).isEqualTo("ABCDE");
    assertThat(issue.isNew()).isFalse();
    assertThat(issue.isEndOfLife()).isFalse();
    assertThat(issue.isOnDisabledRule()).isFalse();
  }

  @Test
  public void manual_issues_should_be_closed_if_manual_rule_is_removed() throws Exception {
    // "Unmatched" issues existed in previous scan but not in current one -> they have to be closed
    Resource file = new File("Action.java").setEffectiveKey("struts:Action.java").setId(123);

    // INPUT : one issue existing during previous scan
    IssueDto unmatchedIssue = new IssueDto().setKee("ABCDE").setReporter("freddy").setStatus("OPEN").setRuleKey_unit_test_only("manual", "Performance");
    when(ruleFinder.findByKey(RuleKey.of("manual", "Performance"))).thenReturn(new Rule("manual", "Performance").setStatus(Rule.STATUS_REMOVED));

    IssueTrackingResult trackingResult = new IssueTrackingResult();
    trackingResult.addUnmatched(unmatchedIssue);

    when(tracking.track(eq(file), anyCollection(), anyCollection())).thenReturn(trackingResult);

    decorator.doDecorate(file);

    verify(workflow, times(1)).doAutomaticTransition(any(DefaultIssue.class), any(IssueChangeContext.class));
    verify(handlers, times(1)).execute(any(DefaultIssue.class), any(IssueChangeContext.class));

    ArgumentCaptor<DefaultIssue> argument = ArgumentCaptor.forClass(DefaultIssue.class);
    verify(issueCache).put(argument.capture());

    DefaultIssue issue = argument.getValue();
    assertThat(issue.key()).isEqualTo("ABCDE");
    assertThat(issue.isNew()).isFalse();
    assertThat(issue.isEndOfLife()).isTrue();
    assertThat(issue.isOnDisabledRule()).isTrue();
  }

  @Test
  public void manual_issues_should_be_closed_if_manual_rule_is_not_found() throws Exception {
    // "Unmatched" issues existed in previous scan but not in current one -> they have to be closed
    Resource file = new File("Action.java").setEffectiveKey("struts:Action.java").setId(123);

    // INPUT : one issue existing during previous scan
    IssueDto unmatchedIssue = new IssueDto().setKee("ABCDE").setReporter("freddy").setStatus("OPEN").setRuleKey_unit_test_only("manual", "Performance");
    when(ruleFinder.findByKey(RuleKey.of("manual", "Performance"))).thenReturn(null);

    IssueTrackingResult trackingResult = new IssueTrackingResult();
    trackingResult.addUnmatched(unmatchedIssue);

    when(tracking.track(eq(file), anyCollection(), anyCollection())).thenReturn(trackingResult);

    decorator.doDecorate(file);

    verify(workflow, times(1)).doAutomaticTransition(any(DefaultIssue.class), any(IssueChangeContext.class));
    verify(handlers, times(1)).execute(any(DefaultIssue.class), any(IssueChangeContext.class));

    ArgumentCaptor<DefaultIssue> argument = ArgumentCaptor.forClass(DefaultIssue.class);
    verify(issueCache).put(argument.capture());

    DefaultIssue issue = argument.getValue();
    assertThat(issue.key()).isEqualTo("ABCDE");
    assertThat(issue.isNew()).isFalse();
    assertThat(issue.isEndOfLife()).isTrue();
    assertThat(issue.isOnDisabledRule()).isTrue();
  }

  @Test
  public void should_register_issues_on_deleted_components() throws Exception {
    Project project = new Project("struts");
    DefaultIssue openIssue = new DefaultIssue();
    when(issueCache.byComponent("struts")).thenReturn(Arrays.asList(openIssue));
    IssueDto deadIssue = new IssueDto().setKee("ABCDE").setResolution(null).setStatus("OPEN").setRuleKey_unit_test_only("squid", "AvoidCycle");
    when(initialOpenIssues.selectAll()).thenReturn(Arrays.asList(deadIssue));

    decorator.doDecorate(project);

    // the dead issue must be closed -> apply automatic transition, notify handlers and add to cache
    verify(workflow, times(2)).doAutomaticTransition(any(DefaultIssue.class), any(IssueChangeContext.class));
    verify(handlers, times(2)).execute(any(DefaultIssue.class), any(IssueChangeContext.class));
    verify(issueCache, times(2)).put(any(DefaultIssue.class));

    verify(issueCache).put(argThat(new ArgumentMatcher<DefaultIssue>() {
      @Override
      public boolean matches(Object o) {
        DefaultIssue dead = (DefaultIssue) o;
        return "ABCDE".equals(dead.key()) && !dead.isNew() && dead.isEndOfLife();
      }
    }));
  }
}
