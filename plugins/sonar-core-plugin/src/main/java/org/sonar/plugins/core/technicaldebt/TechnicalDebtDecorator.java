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

import com.google.common.collect.ImmutableList;
import org.sonar.api.CoreProperties;
import org.sonar.api.PropertyType;
import org.sonar.api.batch.*;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.PersistenceMode;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;
import org.sonar.core.technicaldebt.TechnicalDebtCalculator;
import org.sonar.core.technicaldebt.TechnicalDebtCharacteristic;
import org.sonar.core.technicaldebt.TechnicalDebtConverter;
import org.sonar.core.technicaldebt.TechnicalDebtRequirement;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Decorator that computes the technical debt metric
 */
@DependsUpon(DecoratorBarriers.ISSUES_TRACKED)
public final class TechnicalDebtDecorator implements Decorator {

  public static final int DECIMALS_PRECISION = 5;
  private TechnicalDebtCalculator costCalculator;

  public TechnicalDebtDecorator(TechnicalDebtCalculator costCalculator) {
    this.costCalculator = costCalculator;
  }

  public boolean shouldExecuteOnProject(Project project) {
    return true;
  }

  @DependedUpon
  public List<Metric> generatesMetrics() {
    return Arrays.asList(CoreMetrics.TECHNICAL_DEBT);
  }

  public void decorate(Resource resource, DecoratorContext context) {
    if (ResourceUtils.isPersistable(resource) && !ResourceUtils.isUnitTestClass(resource)) {
      costCalculator.compute(context);
      saveCostMeasures(context);
    }
  }

  protected void saveCostMeasures(DecoratorContext context) {
    context.saveMeasure(new Measure(CoreMetrics.TECHNICAL_DEBT, costCalculator.getTotal(), DECIMALS_PRECISION));
    saveCharacteristicCosts(context);
    saveRequirementCosts(context);
  }

  private void saveCharacteristicCosts(DecoratorContext context) {
    for (Map.Entry<TechnicalDebtCharacteristic, Double> entry : costCalculator.getCharacteristicCosts().entrySet()) {
      saveCost(context, entry.getKey().toCharacteristic(), entry.getValue(), false);
    }
  }

  private void saveRequirementCosts(DecoratorContext context) {
    for (Map.Entry<TechnicalDebtRequirement, Double> entry : costCalculator.getRequirementCosts().entrySet()) {
      saveCost(context, entry.getKey().toCharacteristic(), entry.getValue(), ResourceUtils.isEntity(context.getResource()));
    }
  }

  protected void saveCost(DecoratorContext context, org.sonar.api.qualitymodel.Characteristic characteristic, Double value, boolean inMemory) {
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

  public static List<?> extensions() {
    ImmutableList.Builder<Object> extensions = ImmutableList.builder();
    extensions.addAll(definitions());
    extensions.add(
      TechnicalDebtDecorator.class, TechnicalDebtCalculator.class
    );
    return extensions.build();
  }

  private static List<PropertyDefinition> definitions() {
    return ImmutableList.of(
      PropertyDefinition.builder(TechnicalDebtConverter.PROPERTY_HOURS_IN_DAY)
        .name("Number of working hours in a day")
        .type(PropertyType.INTEGER)
        .defaultValue("8")
        .category(CoreProperties.CATEGORY_TECHNICAL_DEBT)
        .build()
    );
  }
}
