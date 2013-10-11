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

import org.sonar.api.batch.*;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

/**
 * Decorator that computes the technical debt density metric
 */
@DependsUpon(DecoratorBarriers.ISSUES_TRACKED)
public final class TechnicalDebtDensityDecorator implements Decorator {

  private static final int DECIMALS_PRECISION = 5;

  public boolean shouldExecuteOnProject(Project project) {
    return true;
  }

  @DependedUpon
  public List<Metric> generatesMetrics() {
    return newArrayList(CoreMetrics.TECHNICAL_DEBT_DENSITY);
  }

  @DependsUpon
  public Metric dependsUponTechnicalDebt() {
    return CoreMetrics.TECHNICAL_DEBT;
  }

  public void decorate(Resource resource, DecoratorContext context) {
    if (context.getMeasure(CoreMetrics.TECHNICAL_DEBT_DENSITY) != null) {
      return;
    }

    Measure technicalDebt = context.getMeasure(CoreMetrics.TECHNICAL_DEBT);
    Measure ncloc = context.getMeasure(CoreMetrics.NCLOC);

    if (technicalDebt != null && ncloc != null && ncloc.getValue() > 0d) {
      double value = technicalDebt.getValue() / ncloc.getValue();
      context.saveMeasure(new Measure(CoreMetrics.TECHNICAL_DEBT_DENSITY, value, DECIMALS_PRECISION));
    }
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }

}
