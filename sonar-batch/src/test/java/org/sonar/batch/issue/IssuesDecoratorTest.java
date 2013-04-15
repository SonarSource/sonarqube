/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.batch.issue;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.issue.Issuable;
import org.sonar.api.issue.Issue;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.MeasuresFilter;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.Scopes;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.test.IsRuleMeasure;
import org.sonar.core.issue.DefaultIssue;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class IssuesDecoratorTest {

  private Rule ruleA1;
  private Rule ruleA2;
  private Rule ruleB1;
  private IssuesDecorator decorator;
  private Resource resource;
  private DecoratorContext context;
  private Issuable issuable;
  private RuleFinder rulefinder;

  @Before
  public void before() {
    ruleA1 = Rule.create().setRepositoryKey("ruleA1").setKey("ruleA1").setName("nameA1");
    ruleA2 = Rule.create().setRepositoryKey("ruleA2").setKey("ruleA2").setName("nameA2");
    ruleB1 = Rule.create().setRepositoryKey("ruleB1").setKey("ruleB1").setName("nameB1");

    rulefinder = mock(RuleFinder.class);
    when(rulefinder.findByKey(ruleA1.getRepositoryKey(), ruleA1.getKey())).thenReturn(ruleA1);
    when(rulefinder.findByKey(ruleA2.getRepositoryKey(), ruleA2.getKey())).thenReturn(ruleA2);
    when(rulefinder.findByKey(ruleB1.getRepositoryKey(), ruleB1.getKey())).thenReturn(ruleB1);

    resource = mock(Resource.class);
    context = mock(DecoratorContext.class);
    when(context.getResource()).thenReturn(resource);

    issuable = mock(Issuable.class);
    ResourcePerspectives perspectives = mock(ResourcePerspectives.class);
    when(perspectives.as(Issuable.class, resource)).thenReturn(issuable);
    decorator = new IssuesDecorator(perspectives, rulefinder);
  }

  @Test
  public void should_count_issues() {
    when(resource.getScope()).thenReturn(Scopes.PROJECT);
    when(issuable.issues()).thenReturn(createIssues());
    when(context.getChildrenMeasures(any(MeasuresFilter.class))).thenReturn(Lists.<Measure>newArrayList());

    decorator.decorate(resource, context);

    verify(context).saveMeasure(CoreMetrics.ISSUES, 4.0);
  }

  /**
   * See http://jira.codehaus.org/browse/SONAR-1729
   */
  @Test
  public void should_not_count_issues_if_measure_already_exists() {
    when(resource.getScope()).thenReturn(Scopes.PROJECT);
    when(issuable.issues()).thenReturn(createIssues());
    when(context.getChildrenMeasures(any(MeasuresFilter.class))).thenReturn(Lists.<Measure>newArrayList());
    when(context.getMeasure(CoreMetrics.ISSUES)).thenReturn(new Measure(CoreMetrics.ISSUES, 3000.0));
    when(context.getMeasure(CoreMetrics.MAJOR_ISSUES)).thenReturn(new Measure(CoreMetrics.MAJOR_ISSUES, 500.0));

    decorator.decorate(resource, context);

    verify(context, never()).saveMeasure(eq(CoreMetrics.ISSUES), anyDouble());// not changed
    verify(context, never()).saveMeasure(eq(CoreMetrics.MAJOR_ISSUES), anyDouble());// not changed
    verify(context, times(1)).saveMeasure(eq(CoreMetrics.CRITICAL_ISSUES), anyDouble());// did not exist
  }

  @Test
  public void should_save_zero_on_projects() {
    when(resource.getScope()).thenReturn(Scopes.PROJECT);
    when(issuable.issues()).thenReturn(Lists.<Issue>newArrayList());
    when(context.getChildrenMeasures(any(MeasuresFilter.class))).thenReturn(Lists.<Measure>newArrayList());

    decorator.decorate(resource, context);

    verify(context).saveMeasure(CoreMetrics.ISSUES, 0.0);
  }

  @Test
  public void should_save_zero_on_directories() {
    when(resource.getScope()).thenReturn(Scopes.DIRECTORY);
    when(issuable.issues()).thenReturn(Lists.<Issue>newArrayList());
    when(context.getChildrenMeasures(any(MeasuresFilter.class))).thenReturn(Lists.<Measure>newArrayList());

    decorator.decorate(resource, context);

    verify(context).saveMeasure(CoreMetrics.ISSUES, 0.0);
  }

  @Test
  public void should_count_issues_by_severity() {
    when(resource.getScope()).thenReturn(Scopes.PROJECT);
    when(issuable.issues()).thenReturn(createIssues());
    when(context.getChildrenMeasures(any(MeasuresFilter.class))).thenReturn(Lists.<Measure>newArrayList());

    decorator.decorate(resource, context);

    verify(context).saveMeasure(CoreMetrics.BLOCKER_ISSUES, 0.0);
    verify(context).saveMeasure(CoreMetrics.CRITICAL_ISSUES, 2.0);
    verify(context).saveMeasure(CoreMetrics.MAJOR_ISSUES, 1.0);
    verify(context).saveMeasure(CoreMetrics.MINOR_ISSUES, 1.0);
    verify(context).saveMeasure(CoreMetrics.INFO_ISSUES, 0.0);
  }

  @Test
  public void should_count_issues_per_rule() {
    List<Issue> issues = newArrayList();
    issues.add(new DefaultIssue().setRuleRepositoryKey(ruleA1.getRepositoryKey()).setRuleKey(ruleA1.getKey()).setSeverity(RulePriority.CRITICAL.name()));
    issues.add(new DefaultIssue().setRuleRepositoryKey(ruleA1.getRepositoryKey()).setRuleKey(ruleA1.getKey()).setSeverity(RulePriority.CRITICAL.name()));
    issues.add(new DefaultIssue().setRuleRepositoryKey(ruleA2.getRepositoryKey()).setRuleKey(ruleA2.getKey()).setSeverity(RulePriority.MAJOR.name()));
    when(issuable.issues()).thenReturn(issues);

    decorator.decorate(resource, context);

    verify(context).saveMeasure(argThat(new IsRuleMeasure(CoreMetrics.CRITICAL_ISSUES, ruleA1, 2.0)));
    verify(context, never()).saveMeasure(argThat(new IsRuleMeasure(CoreMetrics.MAJOR_ISSUES, ruleA1, 0.0)));
    verify(context).saveMeasure(argThat(new IsRuleMeasure(CoreMetrics.MAJOR_ISSUES, ruleA2, 1.0)));
  }

  @Test
  public void same_rule_should_have_different_severities() {
    List<Issue> issues = newArrayList();
    issues.add(new DefaultIssue().setRuleRepositoryKey(ruleA1.getRepositoryKey()).setRuleKey(ruleA1.getKey()).setSeverity(RulePriority.CRITICAL.name()));
    issues.add(new DefaultIssue().setRuleRepositoryKey(ruleA1.getRepositoryKey()).setRuleKey(ruleA1.getKey()).setSeverity(RulePriority.CRITICAL.name()));
    issues.add(new DefaultIssue().setRuleRepositoryKey(ruleA1.getRepositoryKey()).setRuleKey(ruleA1.getKey()).setSeverity(RulePriority.MINOR.name()));
    when(issuable.issues()).thenReturn(issues);

    decorator.decorate(resource, context);

    verify(context).saveMeasure(argThat(new IsRuleMeasure(CoreMetrics.CRITICAL_ISSUES, ruleA1, 2.0)));
    verify(context).saveMeasure(argThat(new IsRuleMeasure(CoreMetrics.MINOR_ISSUES, ruleA1, 1.0)));
  }

  private List<Issue> createIssues() {
    List<Issue> issues = newArrayList();
    issues.add(new DefaultIssue().setRuleRepositoryKey(ruleA1.getRepositoryKey()).setRuleKey(ruleA1.getKey()).setSeverity(RulePriority.CRITICAL.name()));
    issues.add(new DefaultIssue().setRuleRepositoryKey(ruleA1.getRepositoryKey()).setRuleKey(ruleA1.getKey()).setSeverity(RulePriority.CRITICAL.name()));
    issues.add(new DefaultIssue().setRuleRepositoryKey(ruleA2.getRepositoryKey()).setRuleKey(ruleA2.getKey()).setSeverity(RulePriority.MAJOR.name()));
    issues.add(new DefaultIssue().setRuleRepositoryKey(ruleB1.getRepositoryKey()).setRuleKey(ruleB1.getKey()).setSeverity(RulePriority.MINOR.name()));
    return issues;
  }
}
