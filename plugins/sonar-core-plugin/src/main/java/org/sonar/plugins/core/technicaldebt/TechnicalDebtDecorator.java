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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import org.slf4j.LoggerFactory;
import org.sonar.api.CoreProperties;
import org.sonar.api.PropertyType;
import org.sonar.api.batch.*;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.issue.Issuable;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.measures.*;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;
import org.sonar.api.technicaldebt.batch.Characteristic;
import org.sonar.api.technicaldebt.batch.Requirement;
import org.sonar.api.technicaldebt.batch.TechnicalDebtModel;
import org.sonar.api.utils.WorkDurationFactory;

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

  private static final int DECIMALS_PRECISION = 5;

  private final ResourcePerspectives perspectives;
  private final TechnicalDebtModel model;
  private final WorkDurationFactory workDurationFactory;

  public TechnicalDebtDecorator(ResourcePerspectives perspectives, TechnicalDebtModel model, WorkDurationFactory workDurationFactory) {
    this.perspectives = perspectives;
    this.model = model;
    this.workDurationFactory = workDurationFactory;
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
    // group issues by requirement
    ListMultimap<Requirement, Issue> issuesByRequirement = issuesByRequirement(issues);

    double total = 0.0;
    Map<Characteristic, Double> characteristicCosts = newHashMap();
    Map<Requirement, Double> requirementCosts = newHashMap();

    for (Requirement requirement : model.requirements()) {
      List<Issue> requirementIssues = issuesByRequirement.get(requirement);
      double value = computeTechnicalDebt(CoreMetrics.TECHNICAL_DEBT, context, requirement, requirementIssues);

      requirementCosts.put(requirement, value);
      total += value;
      propagateTechnicalDebtInParents(requirement.characteristic(), value, characteristicCosts);
    }

    context.saveMeasure(new Measure(CoreMetrics.TECHNICAL_DEBT, total, DECIMALS_PRECISION));
    saveOnCharacteristic(context, characteristicCosts);
    saveOnRequirement(context, requirementCosts);
  }

  private void saveOnCharacteristic(DecoratorContext context, Map<Characteristic, Double> characteristicCosts) {
    for (Map.Entry<Characteristic, Double> entry : characteristicCosts.entrySet()) {
      saveTechnicalDebt(context, entry.getKey(), entry.getValue(), false);
    }
  }

  private void saveOnRequirement(DecoratorContext context, Map<Requirement, Double> requirementCosts) {
    for (Map.Entry<Requirement, Double> entry : requirementCosts.entrySet()) {
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
      measure.setValue(value, DECIMALS_PRECISION);
      if (inMemory) {
        measure.setPersistenceMode(PersistenceMode.MEMORY);
      }
      context.saveMeasure(measure);
    }
  }

  @VisibleForTesting
  void saveTechnicalDebt(DecoratorContext context, Requirement requirement, Double value, boolean inMemory) {
    // we need the value on projects (root or module) even if value==0 in order to display correctly the SQALE history chart (see SQALE-122)
    // BUT we don't want to save zero-values for non top-characteristics (see SQALE-147)
    if (value > 0.0) {
      Measure measure = new Measure(CoreMetrics.TECHNICAL_DEBT);
      measure.setRequirement(requirement);
      measure.setValue(value, DECIMALS_PRECISION);
      if (inMemory) {
        measure.setPersistenceMode(PersistenceMode.MEMORY);
      }
      context.saveMeasure(measure);
    }
  }

  @VisibleForTesting
  ListMultimap<Requirement, Issue> issuesByRequirement(List<Issue> issues) {
    ListMultimap<Requirement, Issue> issuesByRequirement = ArrayListMultimap.create();
    for (Issue issue : issues) {
      String repositoryKey = issue.ruleKey().repository();
      String key = issue.ruleKey().rule();
      Requirement requirement = model.requirementsByRule(issue.ruleKey());
      if (requirement == null) {
        LoggerFactory.getLogger(getClass()).debug("No technical debt requirement for: " + repositoryKey + "/" + key);
      } else {
        issuesByRequirement.put(requirement, issue);
      }
    }
    return issuesByRequirement;
  }

  private double computeTechnicalDebt(Metric metric, DecoratorContext context, Requirement requirement, Collection<Issue> issues) {
    long debt = 0L;
    if (issues != null) {
      for (Issue issue : issues) {
        Long currentDebt = ((DefaultIssue) issue).debt();
        if (currentDebt != null) {
          debt += currentDebt;
        }
      }
    }

    double debtInDays = workDurationFactory.createFromSeconds(debt).toWorkingDays();
    for (Measure measure : context.getChildrenMeasures(MeasuresFilters.requirement(metric, requirement))) {
      Requirement measureRequirement = measure.getRequirement();
      if (measureRequirement != null && measureRequirement.equals(requirement) && measure.getValue() != null) {
        debtInDays += measure.getValue();
      }
    }
    return debtInDays;
  }

  private void propagateTechnicalDebtInParents(Characteristic characteristic, double value, Map<Characteristic, Double> characteristicCosts) {
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
