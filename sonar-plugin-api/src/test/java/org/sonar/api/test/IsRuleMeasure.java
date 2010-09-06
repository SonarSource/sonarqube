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
package org.sonar.api.test;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.RuleMeasure;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RulePriority;

public class IsRuleMeasure extends BaseMatcher<Measure> {

  private Metric metric = null;
  private Rule rule = null;
  private Integer category = null;
  private RulePriority priority = null;
  private Double value = null;

  public IsRuleMeasure(Metric metric, Rule rule, Integer category, RulePriority priority, Double value) {
    this.metric = metric;
    this.rule = rule;
    this.category = category;
    this.priority = priority;
    this.value = value;
  }

  public boolean matches(Object o) {
    if (!(o instanceof RuleMeasure)) {
      return false;
    }
    RuleMeasure m = (RuleMeasure) o;
    return ObjectUtils.equals(metric, m.getMetric()) &&
        ObjectUtils.equals(rule, m.getRule()) &&
        ObjectUtils.equals(category, m.getRuleCategory()) &&
        ObjectUtils.equals(priority, m.getRulePriority()) &&
        NumberUtils.compare(value, m.getValue()) == 0;
  }

  public void describeTo(Description description) {

  }
}
