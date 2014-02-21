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

package org.sonar.plugins.core.technicaldebt;

import com.google.common.collect.ListMultimap;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.hamcrest.Description;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.issue.Issuable;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.MeasuresFilter;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.technicaldebt.batch.Characteristic;
import org.sonar.api.technicaldebt.batch.Requirement;
import org.sonar.api.technicaldebt.batch.TechnicalDebtModel;
import org.sonar.api.technicaldebt.batch.internal.DefaultCharacteristic;
import org.sonar.api.technicaldebt.batch.internal.DefaultRequirement;
import org.sonar.api.test.IsMeasure;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TechnicalDebtDecoratorTest {

  final static Long ONE_DAY_DEBT = 1 * 8 * 60 * 60L;

  @Mock
  DecoratorContext context;

  @Mock
  Resource resource;

  @Mock
  TechnicalDebtModel defaultTechnicalDebtModel;

  @Mock
  Issuable issuable;

  TechnicalDebtDecorator decorator;

  @Before
  public void before() throws Exception {
    ResourcePerspectives perspectives = mock(ResourcePerspectives.class);
    when(perspectives.as(Issuable.class, resource)).thenReturn(issuable);

    decorator = new TechnicalDebtDecorator(perspectives, defaultTechnicalDebtModel);
  }

  @Test
  public void generates_metrics() throws Exception {
    assertThat(decorator.generatesMetrics()).hasSize(1);
  }

  @Test
  public void execute_on_project() throws Exception {
    assertThat(decorator.shouldExecuteOnProject(null)).isTrue();
  }

  @Test
  public void not_save_if_measure_already_computed() {
    when(context.getMeasure(CoreMetrics.TECHNICAL_DEBT)).thenReturn(new Measure());

    decorator.decorate(resource, context);

    verify(context, never()).saveMeasure(argThat(new IsMeasure(CoreMetrics.TECHNICAL_DEBT)));
  }

  @Test
  public void group_issues_by_requirement() throws Exception {
    Requirement requirement1 = mock(Requirement.class);
    Requirement requirement2 = mock(Requirement.class);

    Issue issue1 = createIssue("rule1", "repo1");
    Issue issue2 = createIssue("rule1", "repo1");
    Issue issue3 = createIssue("rule2", "repo2");
    Issue issue4 = createIssue("unmatchable", "repo2");

    List<Issue> issues = newArrayList(issue1, issue2, issue3, issue4);

    when(defaultTechnicalDebtModel.requirementsByRule(RuleKey.of("repo1", "rule1"))).thenReturn(requirement1);
    when(defaultTechnicalDebtModel.requirementsByRule(RuleKey.of("repo2", "rule2"))).thenReturn(requirement2);

    ListMultimap<Requirement, Issue> result = decorator.issuesByRequirement(issues);

    assertThat(result.keySet().size()).isEqualTo(2);
    assertThat(result.get(requirement1)).containsExactly(issue1, issue2);
    assertThat(result.get(requirement2)).containsExactly(issue3);
  }

  @Test
  public void add_technical_debt_from_one_issue_and_no_parent() throws Exception {
    Issue issue = createIssue("rule1", "repo1").setDebt(ONE_DAY_DEBT);
    when(issuable.issues()).thenReturn(newArrayList(issue));

    Requirement requirement = mock(Requirement.class);
    when(defaultTechnicalDebtModel.requirementsByRule(RuleKey.of("repo1", "rule1"))).thenReturn(requirement);
    doReturn(newArrayList(requirement)).when(defaultTechnicalDebtModel).requirements();

    decorator.decorate(resource, context);

    verify(context).saveMeasure(CoreMetrics.TECHNICAL_DEBT, ONE_DAY_DEBT.doubleValue());
    verify(context).saveMeasure(argThat(new IsCharacteristicMeasure(CoreMetrics.TECHNICAL_DEBT, null, requirement, ONE_DAY_DEBT.doubleValue())));
  }

  @Test
  public void add_technical_debt_from_one_issue_without_debt() throws Exception {
    Issue issue = createIssue("rule1", "repo1").setDebt(null);
    when(issuable.issues()).thenReturn(newArrayList(issue));

    Requirement requirement = mock(Requirement.class);
    when(defaultTechnicalDebtModel.requirementsByRule(RuleKey.of("repo1", "rule1"))).thenReturn(requirement);
    doReturn(newArrayList(requirement)).when(defaultTechnicalDebtModel).requirements();

    decorator.decorate(resource, context);

    verify(context).saveMeasure(CoreMetrics.TECHNICAL_DEBT, 0.0);
  }

  @Test
  public void add_technical_debt_from_one_issue_and_propagate_to_parents() throws Exception {
    Issue issue = createIssue("rule1", "repo1").setDebt(ONE_DAY_DEBT);
    when(issuable.issues()).thenReturn(newArrayList(issue));

    DefaultCharacteristic parentCharacteristic = new DefaultCharacteristic().setKey("parentCharacteristic");
    DefaultCharacteristic characteristic = new DefaultCharacteristic().setKey("characteristic").setParent(parentCharacteristic);
    RuleKey ruleKey = RuleKey.of("repo1", "rule1");
    DefaultRequirement requirement = new DefaultRequirement().setCharacteristic(characteristic).setRuleKey(ruleKey);

    when(defaultTechnicalDebtModel.requirementsByRule(ruleKey)).thenReturn(requirement);
    doReturn(newArrayList(requirement)).when(defaultTechnicalDebtModel).requirements();

    decorator.decorate(resource, context);

    verify(context).saveMeasure(CoreMetrics.TECHNICAL_DEBT, ONE_DAY_DEBT.doubleValue());
    verify(context).saveMeasure(argThat(new IsCharacteristicMeasure(CoreMetrics.TECHNICAL_DEBT, parentCharacteristic, ONE_DAY_DEBT.doubleValue())));
    verify(context).saveMeasure(argThat(new IsCharacteristicMeasure(CoreMetrics.TECHNICAL_DEBT, characteristic, ONE_DAY_DEBT.doubleValue())));
    verify(context).saveMeasure(argThat(new IsCharacteristicMeasure(CoreMetrics.TECHNICAL_DEBT, requirement, ONE_DAY_DEBT.doubleValue())));
  }

  @Test
  public void add_technical_debt_from_issues() throws Exception {
    Long technicalDebt1 = ONE_DAY_DEBT;
    Long technicalDebt2 = 2 * ONE_DAY_DEBT;

    Issue issue1 = createIssue("rule1", "repo1").setDebt(technicalDebt1);
    Issue issue2 = createIssue("rule1", "repo1").setDebt(technicalDebt1);
    Issue issue3 = createIssue("rule2", "repo2").setDebt(technicalDebt2);
    Issue issue4 = createIssue("rule2", "repo2").setDebt(technicalDebt2);
    when(issuable.issues()).thenReturn(newArrayList(issue1, issue2, issue3, issue4));

    DefaultCharacteristic rootCharacteristic = new DefaultCharacteristic().setKey("rootCharacteristic");
    DefaultCharacteristic characteristic = new DefaultCharacteristic().setKey("characteristic").setParent(rootCharacteristic);
    RuleKey ruleKey1 = RuleKey.of("repo1", "rule1");
    DefaultRequirement requirement1 = new DefaultRequirement().setRuleKey(ruleKey1).setCharacteristic(characteristic);
    RuleKey ruleKey2 = RuleKey.of("repo2", "rule2");
    DefaultRequirement requirement2 = new DefaultRequirement().setRuleKey(ruleKey2).setCharacteristic(characteristic);

    when(defaultTechnicalDebtModel.requirementsByRule(ruleKey1)).thenReturn(requirement1);
    when(defaultTechnicalDebtModel.requirementsByRule(ruleKey2)).thenReturn(requirement2);
    doReturn(newArrayList(requirement1, requirement2)).when(defaultTechnicalDebtModel).requirements();

    decorator.decorate(resource, context);

    verify(context).saveMeasure(CoreMetrics.TECHNICAL_DEBT, 6d * ONE_DAY_DEBT);
    verify(context).saveMeasure(argThat(new IsCharacteristicMeasure(CoreMetrics.TECHNICAL_DEBT, requirement1, 2d * ONE_DAY_DEBT)));
    verify(context).saveMeasure(argThat(new IsCharacteristicMeasure(CoreMetrics.TECHNICAL_DEBT, requirement2, 4d * ONE_DAY_DEBT)));
  }

  @Test
  public void add_technical_debt_from_children_measures() throws Exception {
    Issue issue1 = createIssue("rule1", "repo1").setDebt(ONE_DAY_DEBT);
    Issue issue2 = createIssue("rule1", "repo1").setDebt(ONE_DAY_DEBT);
    when(issuable.issues()).thenReturn(newArrayList(issue1, issue2));

    DefaultCharacteristic rootCharacteristic = new DefaultCharacteristic().setKey("rootCharacteristic");
    DefaultCharacteristic characteristic = new DefaultCharacteristic().setKey("characteristic").setParent(rootCharacteristic);
    RuleKey ruleKey1 = RuleKey.of("repo1", "rule1");
    DefaultRequirement requirement = new DefaultRequirement().setRuleKey(ruleKey1).setCharacteristic(characteristic);

    when(defaultTechnicalDebtModel.requirementsByRule(ruleKey1)).thenReturn(requirement);
    doReturn(newArrayList(requirement)).when(defaultTechnicalDebtModel).requirements();

    Measure measure = new Measure().setRequirement(requirement).setValue(5d * ONE_DAY_DEBT);
    when(context.getChildrenMeasures(any(MeasuresFilter.class))).thenReturn(newArrayList(measure));

    decorator.decorate(resource, context);

    verify(context).saveMeasure(CoreMetrics.TECHNICAL_DEBT, 7d * ONE_DAY_DEBT);
    verify(context).saveMeasure(argThat(new IsCharacteristicMeasure(CoreMetrics.TECHNICAL_DEBT, requirement, 7d * ONE_DAY_DEBT)));
  }

  @Test
  public void always_save_technical_debt_for_positive_values() throws Exception {
    // for a project
    DecoratorContext context = mock(DecoratorContext.class);
    when(context.getResource()).thenReturn(new Project("foo"));
    decorator.saveTechnicalDebt(context, (Characteristic) null, 12.0, false);
    verify(context, times(1)).saveMeasure(new Measure(CoreMetrics.TECHNICAL_DEBT));

    // or for a file
    context = mock(DecoratorContext.class);
    when(context.getResource()).thenReturn(new File("foo"));
    decorator.saveTechnicalDebt(context, (Characteristic) null, 12.0, false);
    verify(context, times(1)).saveMeasure(new Measure(CoreMetrics.TECHNICAL_DEBT));
  }

  @Test
  public void always_save_technical_debt_for_project_if_top_characteristic() throws Exception {
    DecoratorContext context = mock(DecoratorContext.class);
    when(context.getResource()).thenReturn(new Project("foo"));
    // this is a top characteristic
    DefaultCharacteristic rootCharacteristic = new DefaultCharacteristic().setKey("root");

    decorator.saveTechnicalDebt(context, rootCharacteristic, 0.0, true);
    verify(context, times(1)).saveMeasure(new Measure(CoreMetrics.TECHNICAL_DEBT).setCharacteristic(rootCharacteristic));
  }

  /**
   * SQALE-147
   */
  @Test
  public void never_save_technical_debt_for_project_if_not_top_characteristic() throws Exception {
    DecoratorContext context = mock(DecoratorContext.class);
    when(context.getResource()).thenReturn(new Project("foo"));

    DefaultCharacteristic rootCharacteristic = new DefaultCharacteristic().setKey("rootCharacteristic");
    DefaultCharacteristic characteristic = new DefaultCharacteristic().setKey("characteristic").setParent(rootCharacteristic);

    decorator.saveTechnicalDebt(context, characteristic, 0.0, true);
    verify(context, never()).saveMeasure(any(Measure.class));
  }

  @Test
  public void not_save_technical_debt_for_file_if_zero() throws Exception {
    DecoratorContext context = mock(DecoratorContext.class);
    when(context.getResource()).thenReturn(new File("foo"));

    decorator.saveTechnicalDebt(context, (Characteristic) null, 0.0, true);
    verify(context, never()).saveMeasure(new Measure(CoreMetrics.TECHNICAL_DEBT));
  }

  @Test
  public void check_definitions() {
    assertThat(TechnicalDebtDecorator.definitions()).hasSize(1);
  }

  private DefaultIssue createIssue(String ruleKey, String repositoryKey) {
    return new DefaultIssue().setRuleKey(RuleKey.of(repositoryKey, ruleKey));
  }

  class IsCharacteristicMeasure extends ArgumentMatcher<Measure> {
    Metric metric = null;
    Characteristic characteristic = null;
    Requirement requirement = null;
    Double value = null;

    public IsCharacteristicMeasure(Metric metric, Characteristic characteristic, Requirement requirement, Double value) {
      this.metric = metric;
      this.characteristic = characteristic;
      this.requirement = requirement;
      this.value = value;
    }

    public IsCharacteristicMeasure(Metric metric, Characteristic characteristic, Double value) {
      this.metric = metric;
      this.characteristic = characteristic;
      this.requirement = null;
      this.value = value;
    }

    public IsCharacteristicMeasure(Metric metric, Requirement requirement, Double value) {
      this.metric = metric;
      this.characteristic = null;
      this.requirement = requirement;
      this.value = value;
    }

    public IsCharacteristicMeasure(Metric metric, Double value) {
      this.metric = metric;
      this.characteristic = null;
      this.requirement = null;
      this.value = value;
    }

    @Override
    public boolean matches(Object o) {
      if (!(o instanceof Measure)) {
        return false;
      }
      Measure m = (Measure) o;
      return ObjectUtils.equals(metric, m.getMetric()) &&
        ObjectUtils.equals(characteristic, m.getCharacteristic()) &&
        ObjectUtils.equals(requirement, m.getRequirement()) &&
        ObjectUtils.equals(value, m.getValue());
    }

    @Override
    public void describeTo(Description description) {
      description.appendText(ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE));
    }
  }
}
