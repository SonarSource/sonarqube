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
import com.google.common.collect.Lists;
import org.apache.commons.lang.time.DateUtils;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.measures.MeasuresFilter;
import org.sonar.api.qualitymodel.Characteristic;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.Violation;
import org.sonar.plugins.core.technicaldebt.functions.Functions;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class TechnicalDebtCalculatorTest {

  private static final Date NOW = new Date(System.currentTimeMillis());
  private static final Date YESTERDAY = DateUtils.addDays(NOW, -1);
  private static final Date LAST_MONTH = DateUtils.addMonths(NOW, -1);

  private TechnicalDebtModel technicalDebtModel;
  private Functions functions;
  private TechnicalDebtCalculator remediationCostCalculator;

  @Before
  public void initMocks() {
    technicalDebtModel = mock(TechnicalDebtModel.class);
    functions = mock(Functions.class);
    remediationCostCalculator = new TechnicalDebtCalculator(technicalDebtModel, functions);
  }

  @Test
  public void group_violations_by_requirement() throws Exception {

    TechnicalDebtRequirement requirement1 = mock(TechnicalDebtRequirement.class);
    TechnicalDebtRequirement requirement2 = mock(TechnicalDebtRequirement.class);

    Violation violation1 = buildViolation("rule1", "repo1", NOW);
    Violation violation2 = buildViolation("rule1", "repo1", NOW);
    Violation violation3 = buildViolation("rule2", "repo2", NOW);
    Violation violation4 = buildViolation("unmatchable", "repo2", NOW);

    List<Violation> violations = Lists.newArrayList(violation1, violation2, violation3, violation4);

    stub(technicalDebtModel.getRequirementByRule("repo1", "rule1")).toReturn(requirement1);
    stub(technicalDebtModel.getRequirementByRule("repo2", "rule2")).toReturn(requirement2);

    DecoratorContext context = mock(DecoratorContext.class);
    when(context.getViolations()).thenReturn(violations);

    ListMultimap<TechnicalDebtRequirement, Violation> groupedViolations = remediationCostCalculator.groupViolations(context);

    assertThat(groupedViolations.keySet().size()).isEqualTo(2);
    assertThat(groupedViolations.get(requirement1)).containsExactly(violation1, violation2);
    assertThat(groupedViolations.get(requirement2)).containsExactly(violation3);
  }

  @Test
  public void add_cost_with_no_parent() throws Exception {

    double requirementCost = 1.0;

    TechnicalDebtRequirement requirement = mock(TechnicalDebtRequirement.class);
    when(requirement.getParent()).thenReturn(null);

    remediationCostCalculator.updateRequirementCosts(requirement, requirementCost);

    assertThat(remediationCostCalculator.getRequirementCosts().get(requirement)).isEqualTo(requirementCost);
    assertThat(remediationCostCalculator.getTotal()).isEqualTo(requirementCost);
  }

  @Test
  public void add_cost_and_propagate_to_parents() throws Exception {

    double requirementCost = 1.0;

    TechnicalDebtCharacteristic parentCharacteristic = new TechnicalDebtCharacteristic(Characteristic.create());

    TechnicalDebtCharacteristic characteristic = new TechnicalDebtCharacteristic(Characteristic.create(), parentCharacteristic);

    TechnicalDebtRequirement requirement = mock(TechnicalDebtRequirement.class);
    when(requirement.getParent()).thenReturn(characteristic);

    remediationCostCalculator.updateRequirementCosts(requirement, requirementCost);

    assertThat(remediationCostCalculator.getRequirementCosts().get(requirement)).isEqualTo(requirementCost);
    assertThat(remediationCostCalculator.getCharacteristicCosts().get(characteristic)).isEqualTo(requirementCost);
    assertThat(remediationCostCalculator.getCharacteristicCosts().get(parentCharacteristic)).isEqualTo(requirementCost);
  }

  @Test
  public void compute_totals_costs() throws Exception {

    TechnicalDebtRequirement requirement1 = mock(TechnicalDebtRequirement.class);
    TechnicalDebtRequirement requirement2 = mock(TechnicalDebtRequirement.class);

    Violation violation1 = buildViolation("rule1", "repo1", NOW);
    Violation violation2 = buildViolation("rule1", "repo1", NOW);
    Violation violation3 = buildViolation("rule2", "repo2", YESTERDAY);
    Violation violation4 = buildViolation("rule2", "repo2", LAST_MONTH);

    List<Violation> violations = Lists.newArrayList(violation1, violation2, violation3, violation4);

    stub(technicalDebtModel.getRequirementByRule("repo1", "rule1")).toReturn(requirement1);
    stub(technicalDebtModel.getRequirementByRule("repo2", "rule2")).toReturn(requirement2);
    stub(technicalDebtModel.getAllRequirements()).toReturn(Lists.newArrayList(requirement1, requirement2));

    stub(functions.calculateCost(any(TechnicalDebtRequirement.class), any(Collection.class))).toReturn(1.0);

    DecoratorContext context = mock(DecoratorContext.class);
    stub(context.getViolations()).toReturn(violations);
    stub(context.getChildrenMeasures(any(MeasuresFilter.class))).toReturn(Collections.EMPTY_LIST);

    remediationCostCalculator.compute(context);

//    assertThat(remediationCostCalculator.getTotal()).isEqualTo(2.0);
    assertThat(remediationCostCalculator.getRequirementCosts().get(requirement1)).isEqualTo(1.0);
    assertThat(remediationCostCalculator.getRequirementCosts().get(requirement2)).isEqualTo(1.0);
  }

  private Violation buildViolation(String ruleKey, String repositoryKey, Date creationDate) {
    Violation violation = mock(Violation.class);
    stub(violation.getRule()).toReturn(Rule.create(repositoryKey, ruleKey));
    stub(violation.getCreatedAt()).toReturn(creationDate);
    return violation;
  }
}

