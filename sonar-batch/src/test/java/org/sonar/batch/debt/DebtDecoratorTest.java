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

package org.sonar.batch.debt;

import com.google.common.collect.Lists;
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
import org.sonar.api.batch.rule.Rules;
import org.sonar.api.batch.rule.internal.RulesBuilder;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.issue.Issuable;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.MeasuresFilter;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.RuleMeasure;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.technicaldebt.batch.Characteristic;
import org.sonar.api.technicaldebt.batch.TechnicalDebtModel;
import org.sonar.api.technicaldebt.batch.internal.DefaultCharacteristic;
import org.sonar.api.test.IsMeasure;
import org.sonar.api.utils.Duration;

import java.util.Collections;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DebtDecoratorTest {

  static final int HOURS_IN_DAY = 8;

  static final Long ONE_DAY_IN_MINUTES = 1L * HOURS_IN_DAY * 60;

  @Mock
  DecoratorContext context;

  @Mock
  Resource resource;

  @Mock
  TechnicalDebtModel debtModel;

  @Mock
  Issuable issuable;

  @Mock
  ResourcePerspectives perspectives;

  @Mock
  RuleFinder ruleFinder;

  RuleKey ruleKey1 = RuleKey.of("repo1", "rule1");
  RuleKey ruleKey2 = RuleKey.of("repo2", "rule2");
  Rules rules;

  DefaultCharacteristic efficiency = new DefaultCharacteristic().setKey("EFFICIENCY");
  DefaultCharacteristic memoryEfficiency = new DefaultCharacteristic().setKey("MEMORY_EFFICIENCY").setParent(efficiency);

  DefaultCharacteristic reusability = new DefaultCharacteristic().setKey("REUSABILITY");
  DefaultCharacteristic modularity = new DefaultCharacteristic().setKey("MODULARITY").setParent(reusability);

  DebtDecorator decorator;

  @Before
  public void before() throws Exception {
    when(perspectives.as(Issuable.class, resource)).thenReturn(issuable);
    RulesBuilder rulesBuilder = new RulesBuilder();
    rulesBuilder.add(ruleKey1).setName("rule1").setDebtSubCharacteristic("MEMORY_EFFICIENCY");
    rulesBuilder.add(ruleKey2).setName("rule2").setDebtSubCharacteristic("MODULARITY");
    rules = rulesBuilder.build();

    when(ruleFinder.findByKey(ruleKey1)).thenReturn(org.sonar.api.rules.Rule.create(ruleKey1.repository(), ruleKey1.rule()));
    when(ruleFinder.findByKey(ruleKey2)).thenReturn(org.sonar.api.rules.Rule.create(ruleKey2.repository(), ruleKey2.rule()));

    when(debtModel.characteristics()).thenReturn(newArrayList(efficiency, memoryEfficiency, reusability, modularity));
    when(debtModel.characteristicByKey("EFFICIENCY")).thenReturn(efficiency);
    when(debtModel.characteristicByKey("MEMORY_EFFICIENCY")).thenReturn(memoryEfficiency);
    when(debtModel.characteristicByKey("REUSABILITY")).thenReturn(reusability);
    when(debtModel.characteristicByKey("MODULARITY")).thenReturn(modularity);

    decorator = new DebtDecorator(perspectives, debtModel, rules, ruleFinder);
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
  public void add_technical_debt_from_one_issue_and_no_parent() throws Exception {
    Issue issue = createIssue("rule1", "repo1").setDebt(Duration.create(ONE_DAY_IN_MINUTES));
    when(issuable.issues()).thenReturn(newArrayList(issue));

    decorator.decorate(resource, context);

    verify(context).saveMeasure(CoreMetrics.TECHNICAL_DEBT, ONE_DAY_IN_MINUTES.doubleValue());
    verify(context).saveMeasure(argThat(new IsRuleMeasure(CoreMetrics.TECHNICAL_DEBT, ruleKey1, ONE_DAY_IN_MINUTES.doubleValue())));
  }

  @Test
  public void add_technical_debt_from_one_issue_without_debt() throws Exception {
    Issue issue = createIssue("rule1", "repo1").setDebt(null);
    when(issuable.issues()).thenReturn(newArrayList(issue));

    decorator.decorate(resource, context);

    verify(context).saveMeasure(CoreMetrics.TECHNICAL_DEBT, 0.0);
  }

  @Test
  public void add_technical_debt_from_one_issue_and_propagate_to_parents() throws Exception {
    Issue issue = createIssue("rule1", "repo1").setDebt(Duration.create(ONE_DAY_IN_MINUTES));
    when(issuable.issues()).thenReturn(newArrayList(issue));

    decorator.decorate(resource, context);

    verify(context).saveMeasure(CoreMetrics.TECHNICAL_DEBT, ONE_DAY_IN_MINUTES.doubleValue());
    verify(context).saveMeasure(argThat(new IsRuleMeasure(CoreMetrics.TECHNICAL_DEBT, ruleKey1, ONE_DAY_IN_MINUTES.doubleValue())));
    verify(context).saveMeasure(argThat(new IsCharacteristicMeasure(CoreMetrics.TECHNICAL_DEBT, efficiency, ONE_DAY_IN_MINUTES.doubleValue())));
    verify(context).saveMeasure(argThat(new IsCharacteristicMeasure(CoreMetrics.TECHNICAL_DEBT, memoryEfficiency, ONE_DAY_IN_MINUTES.doubleValue())));
  }

  @Test
  public void add_technical_debt_from_issues() throws Exception {
    Long technicalDebt1 = ONE_DAY_IN_MINUTES;
    Long technicalDebt2 = 2 * ONE_DAY_IN_MINUTES;

    Issue issue1 = createIssue("rule1", "repo1").setDebt(Duration.create(technicalDebt1));
    Issue issue2 = createIssue("rule1", "repo1").setDebt(Duration.create(technicalDebt1));
    Issue issue3 = createIssue("rule2", "repo2").setDebt(Duration.create(technicalDebt2));
    Issue issue4 = createIssue("rule2", "repo2").setDebt(Duration.create(technicalDebt2));
    when(issuable.issues()).thenReturn(newArrayList(issue1, issue2, issue3, issue4));

    decorator.decorate(resource, context);

    verify(context).saveMeasure(CoreMetrics.TECHNICAL_DEBT, 6d * ONE_DAY_IN_MINUTES);
    verify(context).saveMeasure(argThat(new IsRuleMeasure(CoreMetrics.TECHNICAL_DEBT, ruleKey1, 2d * ONE_DAY_IN_MINUTES)));
    verify(context).saveMeasure(argThat(new IsRuleMeasure(CoreMetrics.TECHNICAL_DEBT, ruleKey2, 4d * ONE_DAY_IN_MINUTES)));
  }

  @Test
  public void add_technical_debt_from_current_and_children_measures() throws Exception {
    Issue issue1 = createIssue("rule1", "repo1").setDebt(Duration.create(ONE_DAY_IN_MINUTES));
    Issue issue2 = createIssue("rule1", "repo1").setDebt(Duration.create(ONE_DAY_IN_MINUTES));
    when(issuable.issues()).thenReturn(newArrayList(issue1, issue2));

    when(context.getChildrenMeasures(any(MeasuresFilter.class))).thenReturn(Lists.<Measure>newArrayList(
      new RuleMeasure(CoreMetrics.TECHNICAL_DEBT,
        org.sonar.api.rules.Rule.create(ruleKey1.repository(), ruleKey1.rule()), null, null)
        .setValue(5d * ONE_DAY_IN_MINUTES)
      ));
    decorator.decorate(resource, context);

    verify(context).saveMeasure(CoreMetrics.TECHNICAL_DEBT, 7d * ONE_DAY_IN_MINUTES);
    verify(context).saveMeasure(argThat(new IsRuleMeasure(CoreMetrics.TECHNICAL_DEBT, ruleKey1, 7d * ONE_DAY_IN_MINUTES)));
    verify(context).saveMeasure(argThat(new IsCharacteristicMeasure(CoreMetrics.TECHNICAL_DEBT, memoryEfficiency, 7d * ONE_DAY_IN_MINUTES)));
    verify(context).saveMeasure(argThat(new IsCharacteristicMeasure(CoreMetrics.TECHNICAL_DEBT, efficiency, 7d * ONE_DAY_IN_MINUTES)));
  }

  @Test
  public void add_technical_debt_only_from_children_measures() throws Exception {
    when(issuable.issues()).thenReturn(Collections.<Issue>emptyList());

    when(context.getChildrenMeasures(any(MeasuresFilter.class))).thenReturn(Lists.<Measure>newArrayList(
      new RuleMeasure(CoreMetrics.TECHNICAL_DEBT,
        org.sonar.api.rules.Rule.create(ruleKey1.repository(), ruleKey1.rule())
        , null, null).setValue(5d * ONE_DAY_IN_MINUTES),

      new RuleMeasure(CoreMetrics.TECHNICAL_DEBT,
        org.sonar.api.rules.Rule.create(ruleKey2.repository(), ruleKey2.rule())
        , null, null).setValue(10d * ONE_DAY_IN_MINUTES)
      ));
    decorator.decorate(resource, context);

    verify(context).saveMeasure(CoreMetrics.TECHNICAL_DEBT, 15d * ONE_DAY_IN_MINUTES);
    verify(context).saveMeasure(argThat(new IsRuleMeasure(CoreMetrics.TECHNICAL_DEBT, ruleKey1, 5d * ONE_DAY_IN_MINUTES)));
    verify(context).saveMeasure(argThat(new IsRuleMeasure(CoreMetrics.TECHNICAL_DEBT, ruleKey2, 10d * ONE_DAY_IN_MINUTES)));
    verify(context).saveMeasure(argThat(new IsCharacteristicMeasure(CoreMetrics.TECHNICAL_DEBT, memoryEfficiency, 5d * ONE_DAY_IN_MINUTES)));
    verify(context).saveMeasure(argThat(new IsCharacteristicMeasure(CoreMetrics.TECHNICAL_DEBT, efficiency, 5d * ONE_DAY_IN_MINUTES)));
    verify(context).saveMeasure(argThat(new IsCharacteristicMeasure(CoreMetrics.TECHNICAL_DEBT, reusability, 10d * ONE_DAY_IN_MINUTES)));
    verify(context).saveMeasure(argThat(new IsCharacteristicMeasure(CoreMetrics.TECHNICAL_DEBT, modularity, 10d * ONE_DAY_IN_MINUTES)));
  }

  @Test
  public void always_save_technical_debt_for_positive_values() throws Exception {
    // for a project
    DecoratorContext context = mock(DecoratorContext.class);
    when(context.getResource()).thenReturn(new Project("foo"));
    decorator.saveCharacteristicMeasure(context, (Characteristic) null, 12.0, false);
    verify(context, times(1)).saveMeasure(new Measure(CoreMetrics.TECHNICAL_DEBT));

    // or for a file
    context = mock(DecoratorContext.class);
    when(context.getResource()).thenReturn(File.create("foo"));
    decorator.saveCharacteristicMeasure(context, (Characteristic) null, 12.0, false);
    verify(context, times(1)).saveMeasure(new Measure(CoreMetrics.TECHNICAL_DEBT));
  }

  @Test
  public void always_save_technical_debt_for_project_if_top_characteristic() throws Exception {
    DecoratorContext context = mock(DecoratorContext.class);
    when(context.getResource()).thenReturn(new Project("foo"));

    // this is a top characteristic
    DefaultCharacteristic rootCharacteristic = new DefaultCharacteristic().setKey("root");

    decorator.saveCharacteristicMeasure(context, rootCharacteristic, 0.0, true);
    verify(context, times(1)).saveMeasure(new Measure(CoreMetrics.TECHNICAL_DEBT).setCharacteristic(rootCharacteristic));
  }

  /**
   * SQALE-147
   */
  @Test
  public void never_save_technical_debt_for_project_if_not_top_characteristic() throws Exception {
    DecoratorContext context = mock(DecoratorContext.class);
    when(context.getResource()).thenReturn(new Project("foo"));

    DefaultCharacteristic rootCharacteristic = new DefaultCharacteristic().setKey("EFFICIENCY");
    DefaultCharacteristic characteristic = new DefaultCharacteristic().setKey("MEMORY_EFFICIENCY").setParent(rootCharacteristic);

    decorator.saveCharacteristicMeasure(context, characteristic, 0.0, true);
    verify(context, never()).saveMeasure(any(Measure.class));
  }

  @Test
  public void not_save_technical_debt_for_file_if_zero() throws Exception {
    DecoratorContext context = mock(DecoratorContext.class);
    when(context.getResource()).thenReturn(File.create("foo"));

    decorator.saveCharacteristicMeasure(context, null, 0.0, true);
    verify(context, never()).saveMeasure(new Measure(CoreMetrics.TECHNICAL_DEBT));
  }

  private DefaultIssue createIssue(String ruleKey, String repositoryKey) {
    return new DefaultIssue().setRuleKey(RuleKey.of(repositoryKey, ruleKey));
  }

  class IsCharacteristicMeasure extends ArgumentMatcher<Measure> {
    Metric metric = null;
    Characteristic characteristic = null;
    Double value = null;

    public IsCharacteristicMeasure(Metric metric, Characteristic characteristic, Double value) {
      this.metric = metric;
      this.characteristic = characteristic;
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
        ObjectUtils.equals(value, m.getValue());
    }

    @Override
    public void describeTo(Description description) {
      description.appendText(new StringBuilder()
        .append("value=").append(value).append(",")
        .append("characteristic=").append(characteristic.key()).append(",")
        .append("metric=").append(metric.getKey()).toString());
    }
  }

  class IsRuleMeasure extends ArgumentMatcher<RuleMeasure> {
    Metric metric = null;
    RuleKey ruleKey = null;
    Double value = null;

    public IsRuleMeasure(Metric metric, RuleKey ruleKey, Double value) {
      this.metric = metric;
      this.ruleKey = ruleKey;
      this.value = value;
    }

    @Override
    public boolean matches(Object o) {
      if (!(o instanceof RuleMeasure)) {
        return false;
      }
      RuleMeasure m = (RuleMeasure) o;
      return ObjectUtils.equals(metric, m.getMetric()) &&
        ObjectUtils.equals(ruleKey, m.ruleKey()) &&
        ObjectUtils.equals(value, m.getValue());
    }

    @Override
    public void describeTo(Description description) {
      description.appendText(ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE));
    }
  }
}
