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

import com.google.common.annotations.VisibleForTesting;
import org.sonar.api.batch.Decorator;
import org.sonar.api.batch.DecoratorBarriers;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.DependedUpon;
import org.sonar.api.batch.DependsUpon;
import org.sonar.api.batch.RequiresDB;
import org.sonar.api.batch.rule.Rule;
import org.sonar.api.batch.rule.Rules;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.issue.Issuable;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.MeasuresFilters;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.PersistenceMode;
import org.sonar.api.measures.RuleMeasure;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.technicaldebt.batch.Characteristic;
import org.sonar.api.technicaldebt.batch.TechnicalDebtModel;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;

/**
 * Decorator that computes the technical debt metric
 */
@DependsUpon(DecoratorBarriers.ISSUES_TRACKED)
@RequiresDB
public final class DebtDecorator implements Decorator {

  private final ResourcePerspectives perspectives;
  private final TechnicalDebtModel model;
  private final Rules rules;

  /**
   * ruleFinder is needed to load "old" rule in order to persist rule measure
   */
  private final RuleFinder ruleFinder;

  public DebtDecorator(ResourcePerspectives perspectives, TechnicalDebtModel model, Rules rules, RuleFinder ruleFinder) {
    this.perspectives = perspectives;
    this.model = model;
    this.rules = rules;
    this.ruleFinder = ruleFinder;
  }

  @Override
  public boolean shouldExecuteOnProject(Project project) {
    return true;
  }

  @DependedUpon
  public List<Metric> generatesMetrics() {
    return Arrays.<Metric>asList(CoreMetrics.TECHNICAL_DEBT);
  }

  @Override
  public void decorate(Resource resource, DecoratorContext context) {
    Issuable issuable = perspectives.as(Issuable.class, resource);
    if (issuable != null && shouldSaveMeasure(context)) {
      List<Issue> issues = newArrayList(issuable.issues());
      saveMeasures(context, issues);
    }
  }

  private void saveMeasures(DecoratorContext context, List<Issue> issues) {
    Long total = 0L;
    SumMap<RuleKey> ruleDebts = new SumMap<RuleKey>();
    SumMap<Characteristic> characteristicDebts = new SumMap<Characteristic>();

    // Aggregate rules debt from current issues (and populate current characteristic debt)
    for (Issue issue : issues) {
      Long debt = ((DefaultIssue) issue).debtInMinutes();
      total += computeDebt(debt, issue.ruleKey(), ruleDebts, characteristicDebts);
    }

    // Aggregate rules debt from children (and populate children characteristics debt)
    for (Measure measure : context.getChildrenMeasures(MeasuresFilters.rules(CoreMetrics.TECHNICAL_DEBT))) {
      Long debt = measure.getValue().longValue();
      RuleMeasure ruleMeasure = (RuleMeasure) measure;
      total += computeDebt(debt, ruleMeasure.ruleKey(), ruleDebts, characteristicDebts);
    }

    context.saveMeasure(CoreMetrics.TECHNICAL_DEBT, total.doubleValue());
    saveOnRule(context, ruleDebts);
    for (Characteristic characteristic : model.characteristics()) {
      Long debt = characteristicDebts.get(characteristic);
      saveCharacteristicMeasure(context, characteristic, debt != null ? debt.doubleValue() : 0d, false);
    }
  }

  private Long computeDebt(@Nullable Long debt, RuleKey ruleKey, SumMap<RuleKey> ruleDebts, SumMap<Characteristic> characteristicDebts) {
    if (debt != null) {
      Rule rule = rules.find(ruleKey);
      if (rule != null) {
        String characteristicKey = rule.debtSubCharacteristic();
        if (characteristicKey != null) {
          Characteristic characteristic = model.characteristicByKey(characteristicKey);
          if (characteristic != null) {
            ruleDebts.add(ruleKey, debt);
            characteristicDebts.add(characteristic, debt);
            propagateTechnicalDebtInParents(characteristic.parent(), debt, characteristicDebts);
            return debt;
          }
        }
      }
    }
    return 0L;
  }

  private void propagateTechnicalDebtInParents(@Nullable Characteristic characteristic, long value, SumMap<Characteristic> characteristicDebts) {
    if (characteristic != null) {
      characteristicDebts.add(characteristic, value);
      propagateTechnicalDebtInParents(characteristic.parent(), value, characteristicDebts);
    }
  }

  private void saveOnRule(DecoratorContext context, SumMap<RuleKey> ruleDebts) {
    for (Map.Entry<RuleKey, Long> entry : ruleDebts.entrySet()) {
      org.sonar.api.rules.Rule oldRule = ruleFinder.findByKey(entry.getKey());
      if (oldRule != null) {
        saveRuleMeasure(context, oldRule, entry.getValue().doubleValue(), ResourceUtils.isEntity(context.getResource()));
      }
    }
  }

  @VisibleForTesting
  void saveCharacteristicMeasure(DecoratorContext context, Characteristic characteristic, Double value, boolean inMemory) {
    // we need the value on projects (root or module) even if value==0 in order to display correctly the SQALE history chart (see SQALE-122)
    // BUT we don't want to save zero-values for non top-characteristics (see SQALE-147)
    if (value > 0.0 || (ResourceUtils.isProject(context.getResource()) && characteristic.isRoot())) {
      Measure measure = new Measure(CoreMetrics.TECHNICAL_DEBT);
      measure.setCharacteristic(characteristic);
      saveMeasure(context, measure, value, inMemory);
    }
  }

  @VisibleForTesting
  void saveRuleMeasure(DecoratorContext context, org.sonar.api.rules.Rule rule, Double value, boolean inMemory) {
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

  private boolean shouldSaveMeasure(DecoratorContext context) {
    return context.getMeasure(CoreMetrics.TECHNICAL_DEBT) == null;
  }

  private static class SumMap<E> {
    private Map<E, Long> sumByKeys;

    public SumMap() {
      sumByKeys = newHashMap();
    }

    public void add(@Nullable E key, Long value) {
      if (key != null) {
        Long currentValue = sumByKeys.get(key);
        sumByKeys.put(key, currentValue != null ? currentValue + value : value);
      }
    }

    @CheckForNull
    public Long get(E key) {
      return sumByKeys.get(key);
    }

    public Set<Map.Entry<E, Long>> entrySet() {
      return sumByKeys.entrySet();
    }
  }
}
