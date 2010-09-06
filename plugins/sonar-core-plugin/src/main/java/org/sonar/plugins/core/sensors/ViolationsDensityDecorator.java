/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.core.sensors;

import org.sonar.api.batch.Decorator;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.DependedUpon;
import org.sonar.api.batch.DependsUpon;
import org.sonar.jpa.dao.RulesDao;
import org.sonar.api.measures.*;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;
import org.sonar.api.rules.Iso9126RulesCategories;
import org.sonar.api.rules.RulesCategory;

import java.util.*;

public class ViolationsDensityDecorator implements Decorator {

  private Map<Integer, Metric> metricByCategoryId;

  public ViolationsDensityDecorator(RulesDao rulesDao) {
    metricByCategoryId = new HashMap<Integer, Metric>();
    for (RulesCategory category : rulesDao.getCategories()) {
      if (category.equals(Iso9126RulesCategories.EFFICIENCY)) {
        metricByCategoryId.put(category.getId(), CoreMetrics.EFFICIENCY);

      } else if (category.equals(Iso9126RulesCategories.MAINTAINABILITY)) {
        metricByCategoryId.put(category.getId(), CoreMetrics.MAINTAINABILITY);

      } else if (category.equals(Iso9126RulesCategories.PORTABILITY)) {
        metricByCategoryId.put(category.getId(), CoreMetrics.PORTABILITY);

      } else if (category.equals(Iso9126RulesCategories.RELIABILITY)) {
        metricByCategoryId.put(category.getId(), CoreMetrics.RELIABILITY);

      } else if (category.equals(Iso9126RulesCategories.USABILITY)) {
        metricByCategoryId.put(category.getId(), CoreMetrics.USABILITY);
      }
    }
  }

  protected ViolationsDensityDecorator(Map<Integer, Metric> metricByCategoryId) {
    this.metricByCategoryId = metricByCategoryId;
  }

  public boolean shouldExecuteOnProject(Project project) {
    return true;
  }

  @DependsUpon
  public List<Metric> dependsUponWeightedViolationsAndNcloc() {
    return Arrays.asList(CoreMetrics.WEIGHTED_VIOLATIONS, CoreMetrics.NCLOC);
  }

  @DependedUpon
  public Metric generatesViolationsDensity() {
    return CoreMetrics.VIOLATIONS_DENSITY;
  }

  public void decorate(Resource resource, DecoratorContext context) {
    if (shouldDecorateResource(context)) {
      decorateDensity(resource, context);
    }
  }

  protected boolean shouldDecorateResource(DecoratorContext context) {
    return context.getMeasure(CoreMetrics.VIOLATIONS_DENSITY) == null;
  }

  private void decorateDensity(Resource resource, DecoratorContext context) {
    Measure ncloc = context.getMeasure(CoreMetrics.NCLOC);
    if (MeasureUtils.hasValue(ncloc) && ncloc.getValue() > 0.0) {
      saveDensity(context, ncloc.getValue().intValue());
      if (ResourceUtils.isSpace(resource) || ResourceUtils.isSet(resource)) {
        saveDensityByCategory(context, ncloc.getValue().intValue());
      }
    }
  }

  private void saveDensity(DecoratorContext context, int ncloc) {
    Measure debt = context.getMeasure(CoreMetrics.WEIGHTED_VIOLATIONS);
    Integer debtValue = 0;
    if (MeasureUtils.hasValue(debt)) {
      debtValue = debt.getValue().intValue();
    }
    double density = calculate(debtValue, ncloc);
    context.saveMeasure(CoreMetrics.VIOLATIONS_DENSITY, density);
  }

  protected static double calculate(int debt, int ncloc) {
    double rci = (1.0 - ((double) debt / (double) ncloc)) * 100.0;
    rci = Math.max(rci, 0.0);
    return rci;
  }

  private void saveDensityByCategory(DecoratorContext context, int ncloc) {
    Collection<RuleMeasure> categDebts = context.getMeasures(MeasuresFilters.ruleCategories(CoreMetrics.WEIGHTED_VIOLATIONS));
    Set<Integer> categIdsDone = new HashSet<Integer>();
    if (categDebts != null) {
      for (RuleMeasure categDebt : categDebts) {
        if (MeasureUtils.hasValue(categDebt)) {
          double density = calculate(categDebt.getValue().intValue(), ncloc);
          context.saveMeasure(RuleMeasure.createForCategory(
              CoreMetrics.VIOLATIONS_DENSITY, categDebt.getRuleCategory(), density));
          context.saveMeasure(metricByCategoryId.get(categDebt.getRuleCategory()), density);
          categIdsDone.add(categDebt.getRuleCategory());
        }
      }
    }
    for (Map.Entry<Integer, Metric> entry : metricByCategoryId.entrySet()) {
      if (!categIdsDone.contains(entry.getKey())) {
        context.saveMeasure(entry.getValue(), 100.0);
      }
    }
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
}
