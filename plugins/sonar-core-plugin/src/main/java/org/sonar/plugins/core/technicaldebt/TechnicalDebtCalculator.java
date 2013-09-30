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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchExtension;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.MeasuresFilters;
import org.sonar.api.measures.Metric;
import org.sonar.api.rules.Violation;
import org.sonar.plugins.core.technicaldebt.functions.Functions;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Computes the remediation cost based on the quality and analysis models.
 */
public class TechnicalDebtCalculator implements BatchExtension {

  private double total = 0.0;
  private Map<TechnicalDebtCharacteristic, Double> characteristicCosts = Maps.newHashMap();
  private Map<TechnicalDebtRequirement, Double> requirementCosts = Maps.newHashMap();

  private Functions functions;
  private TechnicalDebtModel technicalDebtModel;

  public TechnicalDebtCalculator(TechnicalDebtModel technicalDebtModel, Functions functions) {
    this.technicalDebtModel = technicalDebtModel;
    this.functions = functions;
  }

  public void compute(DecoratorContext context) {
    reset();

    // group violations by requirement
    ListMultimap<TechnicalDebtRequirement, Violation> violationsByRequirement = groupViolations(context);

    // the total cost is: cost(violations)
    for (TechnicalDebtRequirement requirement : technicalDebtModel.getAllRequirements()) {
      List<Violation> violations = violationsByRequirement.get(requirement);
      double allViolationsCost = computeRemediationCost(CoreMetrics.TECHNICAL_DEBT, context, requirement, violations);
      updateRequirementCosts(requirement, allViolationsCost);
    }
  }

  public double getTotal() {
    return total;
  }

  public Map<TechnicalDebtCharacteristic, Double> getCharacteristicCosts() {
    return characteristicCosts;
  }

  public Map<TechnicalDebtRequirement, Double> getRequirementCosts() {
    return requirementCosts;
  }

  @VisibleForTesting
  protected ListMultimap<TechnicalDebtRequirement, Violation> groupViolations(DecoratorContext context) {
    ListMultimap<TechnicalDebtRequirement, Violation> violationsByRequirement = ArrayListMultimap.create();
    for (Violation violation : context.getViolations()) {
      String repositoryKey = violation.getRule().getRepositoryKey();
      String key = violation.getRule().getKey();
      TechnicalDebtRequirement requirement = technicalDebtModel.getRequirementByRule(repositoryKey, key);
      if (requirement == null) {
        LoggerFactory.getLogger(getClass()).debug("No technical debt requirement for: " + repositoryKey + "/" + key);
      } else {
        violationsByRequirement.put(requirement, violation);
      }
    }
    return violationsByRequirement;
  }

  @VisibleForTesting
  protected void updateRequirementCosts(TechnicalDebtRequirement requirement, double cost) {
    requirementCosts.put(requirement, cost);
    total += cost;
    propagateCostInParents(requirement.getParent(), cost);
  }

  private double computeRemediationCost(Metric metric, DecoratorContext context, TechnicalDebtRequirement requirement, Collection<Violation> violations) {
    double cost = 0.0;
    if (violations != null) {
      cost = functions.calculateCost(requirement, violations);
    }

    for (Measure measure : context.getChildrenMeasures(MeasuresFilters.characteristic(metric, requirement.toCharacteristic()))) {
      if (measure.getCharacteristic() != null && measure.getCharacteristic().equals(requirement.toCharacteristic()) && measure.getValue() != null) {
        cost += measure.getValue();
      }
    }
    return cost;
  }

  private void reset() {
    total = 0.0;
    characteristicCosts.clear();
    requirementCosts.clear();
  }

  private void propagateCostInParents(TechnicalDebtCharacteristic characteristic, double cost) {
    if (characteristic != null) {
      Double parentCost = characteristicCosts.get(characteristic);
      if (parentCost == null) {
        characteristicCosts.put(characteristic, cost);
      } else {
        characteristicCosts.put(characteristic, cost + parentCost);
      }
      propagateCostInParents(characteristic.getParent(), cost);
    }
  }

}
