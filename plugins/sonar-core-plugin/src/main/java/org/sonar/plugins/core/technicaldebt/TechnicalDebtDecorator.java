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

package org.sonar.plugins.core.technicaldebt;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import org.sonar.api.CoreProperties;
import org.sonar.api.PropertyType;
import org.sonar.api.batch.*;
import org.sonar.api.batch.rule.Rule;
import org.sonar.api.batch.rule.Rules;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.issue.Issuable;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.measures.*;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.technicaldebt.batch.Characteristic;
import org.sonar.api.technicaldebt.batch.TechnicalDebtModel;

import javax.annotation.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;

/**
 * Decorator that computes the technical debt metric
 */
@DependsUpon(DecoratorBarriers.ISSUES_TRACKED)
public final class TechnicalDebtDecorator implements Decorator {

  private final ResourcePerspectives perspectives;
  private final TechnicalDebtModel model;
  private final Rules rules;

  /**
   * ruleFinder is needed to load "old" rule in order to persist rule measure
   */
  private final RuleFinder ruleFinder;

  public TechnicalDebtDecorator(ResourcePerspectives perspectives, TechnicalDebtModel model, Rules rules, RuleFinder ruleFinder) {
    this.perspectives = perspectives;
    this.model = model;
    this.rules = rules;
    this.ruleFinder = ruleFinder;
  }

  public boolean shouldExecuteOnProject(Project project) {
    return true;
  }

  @DependedUpon
  public List<Metric> generatesMetrics() {
    return Arrays.asList(CoreMetrics.TECHNICAL_DEBT);
  }

  public void decorate(Resource resource, DecoratorContext context) {
    Issuable issuable = perspectives.as(Issuable.class, resource);
    if (issuable != null && shouldSaveMeasure(context)) {
      List<Issue> issues = newArrayList(issuable.issues());
      saveMeasures(context, issues);
    }
  }

  private void saveMeasures(DecoratorContext context, List<Issue> issues) {
    // group issues by rule keys
    ListMultimap<RuleKey, Issue> issuesByRule = issuesByRule(issues);

    double total = 0.0;
    Map<Characteristic, Double> characteristicCosts = newHashMap();
    Map<org.sonar.api.rules.Rule, Double> ruleDebtCosts = newHashMap();

    for (Rule newRule : rules.findWithDebt()) {
      String characteristicKey = newRule.characteristic();
      if (characteristicKey != null) {
        org.sonar.api.rules.Rule rule = ruleFinder.findByKey(newRule.key());
        double value = computeTechnicalDebt(CoreMetrics.TECHNICAL_DEBT, context, rule, issuesByRule.get(newRule.key()));
        ruleDebtCosts.put(rule, value);
        total += value;
        Characteristic characteristic = model.characteristicByKey(characteristicKey);
        propagateTechnicalDebtInParents(characteristic, value, characteristicCosts);
      }
    }

    context.saveMeasure(CoreMetrics.TECHNICAL_DEBT, total);
    saveOnCharacteristic(context, characteristicCosts);
    saveOnRule(context, ruleDebtCosts);
  }

  private void saveOnCharacteristic(DecoratorContext context, Map<Characteristic, Double> characteristicCosts) {
    for (Map.Entry<Characteristic, Double> entry : characteristicCosts.entrySet()) {
      saveTechnicalDebt(context, entry.getKey(), entry.getValue(), false);
    }
  }

  private void saveOnRule(DecoratorContext context, Map<org.sonar.api.rules.Rule, Double> requirementCosts) {
    for (Map.Entry<org.sonar.api.rules.Rule, Double> entry : requirementCosts.entrySet()) {
      saveTechnicalDebt(context, entry.getKey(), entry.getValue(), ResourceUtils.isEntity(context.getResource()));
    }
  }

  @VisibleForTesting
  void saveTechnicalDebt(DecoratorContext context, Characteristic characteristic, Double value, boolean inMemory) {
    // we need the value on projects (root or module) even if value==0 in order to display correctly the SQALE history chart (see SQALE-122)
    // BUT we don't want to save zero-values for non top-characteristics (see SQALE-147)
    if (value > 0.0 || (ResourceUtils.isProject(context.getResource()) && characteristic.isRoot())) {
      Measure measure = new Measure(CoreMetrics.TECHNICAL_DEBT);
      measure.setCharacteristic(characteristic);
      saveMeasure(context, measure, value, inMemory);
    }
  }

  @VisibleForTesting
  void saveTechnicalDebt(DecoratorContext context, org.sonar.api.rules.Rule rule, Double value, boolean inMemory) {
    // we need the value on projects (root or module) even if value==0 in order to display correctly the SQALE history chart (see SQALE-122)
    // BUT we don't want to save zero-values for non top-characteristics (see SQALE-147)
    if (value > 0.0) {
      RuleMeasure measure = new RuleMeasure(CoreMetrics.TECHNICAL_DEBT, rule, null, null);
      saveMeasure(context, measure, value, inMemory);
    }
  }

  private void saveMeasure(DecoratorContext context, Measure measure, Double value, boolean inMemory) {
    measure.setValue(value);
    if (inMemory) {
      measure.setPersistenceMode(PersistenceMode.MEMORY);
    }
    context.saveMeasure(measure);
  }

  @VisibleForTesting
  ListMultimap<RuleKey, Issue> issuesByRule(List<Issue> issues) {
    ListMultimap<RuleKey, Issue> result = ArrayListMultimap.create();
    for (Issue issue : issues) {
      result.put(issue.ruleKey(), issue);
    }
    return result;
  }

  private double computeTechnicalDebt(Metric metric, DecoratorContext context, org.sonar.api.rules.Rule rule, Collection<Issue> issues) {
    long debt = 0L;
    if (issues != null) {
      for (Issue issue : issues) {
        Long currentDebt = ((DefaultIssue) issue).debtInMinutes();
        if (currentDebt != null) {
          debt += currentDebt;
        }
      }
    }

    for (Measure measure : context.getChildrenMeasures(MeasuresFilters.rule(metric, rule))) {
      // Comparison on rule is only used for unit test, otherwise no need to do this check
      RuleMeasure ruleMeasure = (RuleMeasure) measure;
      if (measure != null && ruleMeasure.getRule().equals(rule) && measure.getValue() != null) {
        debt += measure.getValue();
      }
    }
    return debt;
  }

  private void propagateTechnicalDebtInParents(@Nullable Characteristic characteristic, double value, Map<Characteristic, Double> characteristicCosts) {
    if (characteristic != null) {
      Double parentCost = characteristicCosts.get(characteristic);
      if (parentCost == null) {
        characteristicCosts.put(characteristic, value);
      } else {
        characteristicCosts.put(characteristic, value + parentCost);
      }
      propagateTechnicalDebtInParents(characteristic.parent(), value, characteristicCosts);
    }
  }

  private boolean shouldSaveMeasure(DecoratorContext context) {
    return context.getMeasure(CoreMetrics.TECHNICAL_DEBT) == null;
  }

  public static List<PropertyDefinition> definitions() {
    return ImmutableList.of(
      PropertyDefinition.builder(CoreProperties.HOURS_IN_DAY)
        .name("Number of working hours in a day")
        .type(PropertyType.INTEGER)
        .defaultValue("8")
        .category(CoreProperties.CATEGORY_TECHNICAL_DEBT)
        .deprecatedKey("sqale.hoursInDay")
        .build()
    );
  }

}
