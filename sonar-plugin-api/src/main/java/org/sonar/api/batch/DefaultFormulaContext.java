/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.api.batch;

import org.sonar.api.measures.Formula;
import org.sonar.api.measures.FormulaContext;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Resource;

/**
 * @since 1.11
 * @deprecated since 5.2. Aggregation of measures is provided by {@link org.sonar.api.ce.measure.MeasureComputer}. {@link org.sonar.api.batch.Decorator}
 * and {@link Formula} are no more supported.
 */
@Deprecated
public class DefaultFormulaContext implements FormulaContext {

  public DefaultFormulaContext(Metric metric) {
  }

  @Override
  public Metric getTargetMetric() {
    throw fail();
  }

  @Override
  public Resource getResource() {
    throw fail();
  }

  public void setMetric(Metric metric) {
    throw fail();
  }

  public void setDecoratorContext(DecoratorContext decoratorContext) {
    throw fail();
  }

  private static RuntimeException fail() {
    throw new UnsupportedOperationException(
      "Unsupported since version 5.2. Decorators and formulas are not used anymore for aggregation measures. Please use org.sonar.api.ce.measure.MeasureComputer.");
  }
}
