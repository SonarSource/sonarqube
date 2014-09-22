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
package org.sonar.api.test;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.mockito.ArgumentMatcher;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.RuleMeasure;
import org.sonar.api.rules.Rule;

public class IsRuleMeasure extends ArgumentMatcher<Measure> {

  private Metric metric = null;
  private Rule rule = null;
  private Double value = null;

  public IsRuleMeasure(Metric metric, Rule rule, Double value) {
    this.metric = metric;
    this.rule = rule;
    this.value = value;
  }

  @Override
  public boolean matches(Object o) {
    if (!(o instanceof RuleMeasure)) {
      return false;
    }
    RuleMeasure m = (RuleMeasure) o;
    return ObjectUtils.equals(metric, m.getMetric()) &&
      ObjectUtils.equals(rule.ruleKey(), m.ruleKey()) &&
      NumberUtils.compare(value, m.getValue()) == 0;
  }
}
