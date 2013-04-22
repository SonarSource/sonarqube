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

package org.sonar.squid.indexer;

import org.apache.commons.lang.math.NumberUtils;
import org.sonar.squid.api.Query;
import org.sonar.squid.api.SourceCode;
import org.sonar.squid.measures.Metric;
import org.sonar.squid.measures.MetricDef;

public class QueryByMeasure implements Query {

  private final MetricDef metric;
  private final Operator operator;
  private final double value;

  public enum Operator {
    GREATER_THAN, EQUALS, GREATER_THAN_EQUALS, LESS_THAN, LESS_THAN_EQUALS
  }

  /**
   * @deprecated use {@link #QueryByMeasure(MetricDef, Operator, double)} instead
   */
  @Deprecated
  public QueryByMeasure(Metric metric, Operator operator, double value) {
    this((MetricDef) metric, operator, value);
  }

  public QueryByMeasure(MetricDef metric, Operator operator, double value) {
    this.metric = metric;
    this.operator = operator;
    this.value = value;
  }

  public boolean match(SourceCode unit) {
    switch (operator) {
      case EQUALS:
        return NumberUtils.compare(unit.getDouble(metric), value)==0;
      case GREATER_THAN:
        return unit.getDouble(metric) > value;
      case GREATER_THAN_EQUALS:
        return unit.getDouble(metric) >= value;
      case LESS_THAN_EQUALS:
        return unit.getDouble(metric) <= value;
      case LESS_THAN:
        return unit.getDouble(metric) < value;
      default:
        throw new IllegalStateException("The operator value '" + operator + "' is unknown.");
    }
  }

}
