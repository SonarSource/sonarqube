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
import org.sonar.core.technicaldebt.TechnicalDebtCharacteristic;
import org.sonar.core.technicaldebt.TechnicalDebtConverter;
import org.sonar.core.technicaldebt.TechnicalDebtModel;
import org.sonar.core.technicaldebt.TechnicalDebtRequirement;

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
  private final TechnicalDebtModel technicalDebtModel;
  private final TechnicalDebtConverter converter;

  public TechnicalDebtDecorator(ResourcePerspectives perspectives, TechnicalDebtModel technicalDebtModel, TechnicalDebtConverter converter) {
    this.perspectives = perspectives;
    this.technicalDebtModel = technicalDebtModel;
    this.converter = converter;
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
    ListMultimap<TechnicalDebtRequirement, Issue> issuesByRequirement = issuesByRequirement(issues);

    double total = 0.0;
    Map<TechnicalDebtCharacteristic, Double> characteristicCosts = newHashMap();
    Map<TechnicalDebtRequirement, Double> requirementCosts = newHashMap();

    for (TechnicalDebtRequirement requirement : technicalDebtModel.getAllRequirements()) {
      List<Issue> requirementIssues = issuesByRequirement.get(requirement);
      double value = computeTechnicalDebt(CoreMetrics.TECHNICAL_DEBT, context, requirement, requirementIssues);

      requirementCosts.put(requirement, value);
      total += value;
      propagateTechnicalDebtInParents(requirement.getParent(), value, characteristicCosts);
    }

    context.saveMeasure(new Measure(CoreMetrics.TECHNICAL_DEBT, total, DECIMALS_PRECISION));
    saveOnCharacteristic(context, characteristicCosts);
    saveOnRequirement(context, requirementCosts);
  }

  private void saveOnCharacteristic(DecoratorContext context, Map<TechnicalDebtCharacteristic, Double> characteristicCosts) {
    for (Map.Entry<TechnicalDebtCharacteristic, Double> entry : characteristicCosts.entrySet()) {
      saveCost(context, entry.getKey().toCharacteristic(), entry.getValue(), false);
    }
  }

  private void saveOnRequirement(DecoratorContext context, Map<TechnicalDebtRequirement, Double> requirementCosts) {
    for (Map.Entry<TechnicalDebtRequirement, Double> entry : requirementCosts.entrySet()) {
      saveCost(context, entry.getKey().toCharacteristic(), entry.getValue(), ResourceUtils.isEntity(context.getResource()));
    }
  }

  @VisibleForTesting
  void saveCost(DecoratorContext context, org.sonar.api.qualitymodel.Characteristic characteristic, Double value, boolean inMemory) {
    // we need the value on projects (root or module) even if value==0 in order to display correctly the SQALE history chart (see SQALE-122)
    // BUT we don't want to save zero-values for non top-characteristics (see SQALE-147)
    if (value > 0.0 || (ResourceUtils.isProject(context.getResource()) && characteristic.getDepth() == org.sonar.api.qualitymodel.Characteristic.ROOT_DEPTH)) {
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
  ListMultimap<TechnicalDebtRequirement, Issue> issuesByRequirement(List<Issue> issues) {
    ListMultimap<TechnicalDebtRequirement, Issue> issuesByRequirement = ArrayListMultimap.create();
    for (Issue issue : issues) {
      String repositoryKey = issue.ruleKey().repository();
      String key = issue.ruleKey().rule();
      TechnicalDebtRequirement requirement = technicalDebtModel.getRequirementByRule(repositoryKey, key);
      if (requirement == null) {
        LoggerFactory.getLogger(getClass()).debug("No technical debt requirement for: " + repositoryKey + "/" + key);
      } else {
        issuesByRequirement.put(requirement, issue);
      }
    }
    return issuesByRequirement;
  }

  private double computeTechnicalDebt(Metric metric, DecoratorContext context, TechnicalDebtRequirement requirement, Collection<Issue> issues) {
    double value = 0.0;
    if (issues != null) {
      for (Issue issue : issues){
        value += converter.toDays(((DefaultIssue) issue).technicalDebt());
      }
    }

    for (Measure measure : context.getChildrenMeasures(MeasuresFilters.characteristic(metric, requirement.toCharacteristic()))) {
      if (measure.getCharacteristic() != null && measure.getCharacteristic().equals(requirement.toCharacteristic()) && measure.getValue() != null) {
        value += measure.getValue();
      }
    }
    return value;
  }

  private void propagateTechnicalDebtInParents(TechnicalDebtCharacteristic characteristic, double value, Map<TechnicalDebtCharacteristic, Double> characteristicCosts) {
    if (characteristic != null) {
      Double parentCost = characteristicCosts.get(characteristic);
      if (parentCost == null) {
        characteristicCosts.put(characteristic, value);
      } else {
        characteristicCosts.put(characteristic, value + parentCost);
      }
      propagateTechnicalDebtInParents(characteristic.getParent(), value, characteristicCosts);
    }
  }

  private boolean shouldSaveMeasure(DecoratorContext context) {
    return context.getMeasure(CoreMetrics.TECHNICAL_DEBT) == null;
  }

  public static List<PropertyDefinition> definitions() {
    return ImmutableList.of(
      PropertyDefinition.builder(TechnicalDebtConverter.PROPERTY_HOURS_IN_DAY)
        .name("Number of working hours in a day")
        .type(PropertyType.INTEGER)
        .defaultValue("8")
        .category(CoreProperties.CATEGORY_TECHNICAL_DEBT)
        .deprecatedKey("sqale.hoursInDay")
        .build()
    );
  }
}
