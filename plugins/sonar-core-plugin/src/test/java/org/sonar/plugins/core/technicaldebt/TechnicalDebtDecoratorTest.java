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
import org.sonar.api.issue.internal.WorkDayDuration;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.MeasuresFilter;
import org.sonar.api.measures.Metric;
import org.sonar.api.qualitymodel.Characteristic;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.test.IsMeasure;
import org.sonar.core.technicaldebt.TechnicalDebtCharacteristic;
import org.sonar.core.technicaldebt.TechnicalDebtConverter;
import org.sonar.core.technicaldebt.TechnicalDebtModel;
import org.sonar.core.technicaldebt.TechnicalDebtRequirement;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TechnicalDebtDecoratorTest {

  @Mock
  DecoratorContext context;

  @Mock
  Resource resource;

  @Mock
  TechnicalDebtConverter converter;

  @Mock
  TechnicalDebtModel technicalDebtModel;

  @Mock
  Issuable issuable;

  TechnicalDebtDecorator decorator;

  @Before
  public void before() throws Exception {
    ResourcePerspectives perspectives = mock(ResourcePerspectives.class);
    when(perspectives.as(Issuable.class, resource)).thenReturn(issuable);

    decorator = new TechnicalDebtDecorator(perspectives, technicalDebtModel, converter);
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
    TechnicalDebtRequirement requirement1 = mock(TechnicalDebtRequirement.class);
    TechnicalDebtRequirement requirement2 = mock(TechnicalDebtRequirement.class);

    Issue issue1 = createIssue("rule1", "repo1");
    Issue issue2 = createIssue("rule1", "repo1");
    Issue issue3 = createIssue("rule2", "repo2");
    Issue issue4 = createIssue("unmatchable", "repo2");

    List<Issue> issues = newArrayList(issue1, issue2, issue3, issue4);

    when(technicalDebtModel.getRequirementByRule("repo1", "rule1")).thenReturn(requirement1);
    when(technicalDebtModel.getRequirementByRule("repo2", "rule2")).thenReturn(requirement2);

    ListMultimap<TechnicalDebtRequirement, Issue> result = decorator.issuesByRequirement(issues);

    assertThat(result.keySet().size()).isEqualTo(2);
    assertThat(result.get(requirement1)).containsExactly(issue1, issue2);
    assertThat(result.get(requirement2)).containsExactly(issue3);
  }

  @Test
  public void add_technical_debt_from_one_issue_and_no_parent() throws Exception {
    WorkDayDuration technicalDebt = mock(WorkDayDuration.class);
    when(converter.toDays(technicalDebt)).thenReturn(1.0);

    Issue issue = createIssue("rule1", "repo1").setTechnicalDebt(technicalDebt);
    when(issuable.issues()).thenReturn(newArrayList(issue));

    Characteristic requirement = Characteristic.create();
    TechnicalDebtRequirement technicalDebtRequirement = mock(TechnicalDebtRequirement.class);
    when(technicalDebtRequirement.toCharacteristic()).thenReturn(requirement);
    when(technicalDebtRequirement.getParent()).thenReturn(null);

    when(technicalDebtModel.getRequirementByRule("repo1", "rule1")).thenReturn(technicalDebtRequirement);
    when(technicalDebtModel.getAllRequirements()).thenReturn(newArrayList(technicalDebtRequirement));

    decorator.decorate(resource, context);

    verify(context).saveMeasure(argThat(new IsCharacteristicMeasure(CoreMetrics.TECHNICAL_DEBT, null, 1.0)));
    verify(context).saveMeasure(argThat(new IsCharacteristicMeasure(CoreMetrics.TECHNICAL_DEBT, requirement, 1.0)));
  }

  @Test
  public void add_technical_debt_from_one_issue_and_propagate_to_parents() throws Exception {
    WorkDayDuration technicalDebt = mock(WorkDayDuration.class);
    when(converter.toDays(technicalDebt)).thenReturn(1.0);

    Issue issue = createIssue("rule1", "repo1").setTechnicalDebt(technicalDebt);
    when(issuable.issues()).thenReturn(newArrayList(issue));

    Characteristic requirement = Characteristic.createByName("requirement");
    Characteristic characteristic = Characteristic.createByName("characteristic");
    Characteristic parentCharacteristic = Characteristic.createByName("parentCharacteristic");

    TechnicalDebtCharacteristic parentTechDebtCharacteristic = new TechnicalDebtCharacteristic(parentCharacteristic);
    TechnicalDebtCharacteristic techDebtCharacteristic = new TechnicalDebtCharacteristic(characteristic, parentTechDebtCharacteristic);
    TechnicalDebtRequirement technicalDebtRequirement = mock(TechnicalDebtRequirement.class);
    when(technicalDebtRequirement.toCharacteristic()).thenReturn(requirement);
    when(technicalDebtRequirement.getParent()).thenReturn(techDebtCharacteristic);

    when(technicalDebtModel.getRequirementByRule("repo1", "rule1")).thenReturn(technicalDebtRequirement);
    when(technicalDebtModel.getAllRequirements()).thenReturn(newArrayList(technicalDebtRequirement));

    decorator.decorate(resource, context);

    verify(context).saveMeasure(argThat(new IsCharacteristicMeasure(CoreMetrics.TECHNICAL_DEBT, null, 1.0)));
    verify(context).saveMeasure(argThat(new IsCharacteristicMeasure(CoreMetrics.TECHNICAL_DEBT, parentCharacteristic, 1.0)));
    verify(context).saveMeasure(argThat(new IsCharacteristicMeasure(CoreMetrics.TECHNICAL_DEBT, characteristic, 1.0)));
    verify(context).saveMeasure(argThat(new IsCharacteristicMeasure(CoreMetrics.TECHNICAL_DEBT, requirement, 1.0)));
  }

  @Test
  public void add_technical_debt_from_issues() throws Exception {
    WorkDayDuration technicalDebt1 = mock(WorkDayDuration.class);
    when(converter.toDays(technicalDebt1)).thenReturn(1.0);

    WorkDayDuration technicalDebt2 = mock(WorkDayDuration.class);
    when(converter.toDays(technicalDebt2)).thenReturn(2.0);

    Issue issue1 = createIssue("rule1", "repo1").setTechnicalDebt(technicalDebt1);
    Issue issue2 = createIssue("rule1", "repo1").setTechnicalDebt(technicalDebt1);
    Issue issue3 = createIssue("rule2", "repo2").setTechnicalDebt(technicalDebt2);
    Issue issue4 = createIssue("rule2", "repo2").setTechnicalDebt(technicalDebt2);
    when(issuable.issues()).thenReturn(newArrayList(issue1, issue2, issue3, issue4));

    Characteristic requirement1 = Characteristic.createByName("requirement1");
    Characteristic requirement2 = Characteristic.createByName("requirement2");

    TechnicalDebtRequirement technicalDebtRequirement1 = mock(TechnicalDebtRequirement.class);
    when(technicalDebtRequirement1.toCharacteristic()).thenReturn(requirement1);
    TechnicalDebtRequirement technicalDebtRequirement2 = mock(TechnicalDebtRequirement.class);
    when(technicalDebtRequirement2.toCharacteristic()).thenReturn(requirement2);


    when(technicalDebtModel.getRequirementByRule("repo1", "rule1")).thenReturn(technicalDebtRequirement1);
    when(technicalDebtModel.getRequirementByRule("repo2", "rule2")).thenReturn(technicalDebtRequirement2);
    when(technicalDebtModel.getAllRequirements()).thenReturn(newArrayList(technicalDebtRequirement1, technicalDebtRequirement2));

    decorator.decorate(resource, context);

    verify(context).saveMeasure(argThat(new IsCharacteristicMeasure(CoreMetrics.TECHNICAL_DEBT, null, 6.0)));
    verify(context).saveMeasure(argThat(new IsCharacteristicMeasure(CoreMetrics.TECHNICAL_DEBT, requirement1, 2.0)));
    verify(context).saveMeasure(argThat(new IsCharacteristicMeasure(CoreMetrics.TECHNICAL_DEBT, requirement2, 4.0)));
  }

  @Test
  public void add_technical_debt_from_children_measures() throws Exception {
    WorkDayDuration technicalDebt1 = mock(WorkDayDuration.class);
    when(converter.toDays(technicalDebt1)).thenReturn(1.0);

    Issue issue1 = createIssue("rule1", "repo1").setTechnicalDebt(technicalDebt1);
    Issue issue2 = createIssue("rule1", "repo1").setTechnicalDebt(technicalDebt1);
    when(issuable.issues()).thenReturn(newArrayList(issue1, issue2));

    Characteristic requirement = Characteristic.createByName("requirement1");
    TechnicalDebtRequirement technicalDebtRequirement = mock(TechnicalDebtRequirement.class);
    when(technicalDebtRequirement.toCharacteristic()).thenReturn(requirement);

    when(technicalDebtModel.getRequirementByRule("repo1", "rule1")).thenReturn(technicalDebtRequirement);
    when(technicalDebtModel.getAllRequirements()).thenReturn(newArrayList(technicalDebtRequirement));

    Measure measure = new Measure().setCharacteristic(requirement).setValue(5.0);
    when(context.getChildrenMeasures(any(MeasuresFilter.class))).thenReturn(newArrayList(measure));

    decorator.decorate(resource, context);

    verify(context).saveMeasure(argThat(new IsCharacteristicMeasure(CoreMetrics.TECHNICAL_DEBT, null, 7.0)));
    verify(context).saveMeasure(argThat(new IsCharacteristicMeasure(CoreMetrics.TECHNICAL_DEBT, requirement, 7.0)));
  }

  @Test
  public void always_save_technical_debt_for_positive_values() throws Exception {
    // for a project
    DecoratorContext context = mock(DecoratorContext.class);
    when(context.getResource()).thenReturn(new Project("foo"));
    decorator.saveCost(context, null, 12.0, false);
    verify(context, times(1)).saveMeasure(new Measure(CoreMetrics.TECHNICAL_DEBT));

    // or for a file
    context = mock(DecoratorContext.class);
    when(context.getResource()).thenReturn(new File("foo"));
    decorator.saveCost(context, null, 12.0, false);
    verify(context, times(1)).saveMeasure(new Measure(CoreMetrics.TECHNICAL_DEBT));
  }

  @Test
  public void always_save_technical_debt_for_project_if_top_characteristic() throws Exception {
    DecoratorContext context = mock(DecoratorContext.class);
    when(context.getResource()).thenReturn(new Project("foo"));
    // this is a top characteristic
    Characteristic topCharacteristic = Characteristic.create();

    decorator.saveCost(context, topCharacteristic, 0.0, true);
    verify(context, times(1)).saveMeasure(new Measure(CoreMetrics.TECHNICAL_DEBT).setCharacteristic(topCharacteristic));
  }

  /**
   * SQALE-147
   */
  @Test
  public void never_save_technical_debt_for_project_if_not_top_characteristic() throws Exception {
    DecoratorContext context = mock(DecoratorContext.class);
    when(context.getResource()).thenReturn(new Project("foo"));
    Characteristic topCharacteristic = Characteristic.create();
    Characteristic childCharacteristic = Characteristic.create();
    topCharacteristic.addChild(childCharacteristic);

    decorator.saveCost(context, childCharacteristic, 0.0, true);
    verify(context, never()).saveMeasure(any(Measure.class));
  }

  @Test
  public void not_save_technical_debt_for_file_if_zero() throws Exception {
    DecoratorContext context = mock(DecoratorContext.class);
    when(context.getResource()).thenReturn(new File("foo"));

    decorator.saveCost(context, null, 0.0, true);
    verify(context, never()).saveMeasure(new Measure(CoreMetrics.TECHNICAL_DEBT));
  }

  @Test
  public void check_definitions() {
    assertThat(decorator.definitions()).hasSize(1);
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

    public boolean matches(Object o) {
      if (!(o instanceof Measure)) {
        return false;
      }
      Measure m = (Measure) o;
      return ObjectUtils.equals(metric, m.getMetric()) &&
        ObjectUtils.equals(characteristic, m.getCharacteristic()) &&
        ObjectUtils.equals(value, m.getValue());
    }
  }
}
