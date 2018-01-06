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
package org.sonar.api.measures;

import org.junit.Test;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RulePriority;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class MeasuresFiltersTest {

  @Test
  public void metric() {
    MeasuresFilter<Measure> filter = MeasuresFilters.metric(CoreMetrics.VIOLATIONS);

    Collection<Measure> measures = Arrays.asList(
      RuleMeasure.createForPriority(CoreMetrics.VIOLATIONS, RulePriority.CRITICAL, 50.0),
      new Measure(CoreMetrics.VIOLATIONS, 500.0));

    assertThat(filter.filter(measures).getValue(), is(500.0));
  }

  @Test
  public void all() {
    Collection<Measure> measures = Arrays.asList(
      RuleMeasure.createForPriority(CoreMetrics.VIOLATIONS, RulePriority.CRITICAL, 50.0),
      new Measure(CoreMetrics.VIOLATIONS, 500.0));

    Iterator<Measure> filteredMeasures = MeasuresFilters.all().filter(measures).iterator();
    filteredMeasures.next();
    filteredMeasures.next();
    assertThat(filteredMeasures.hasNext(), is(false));
  }

  @Test
  public void rule() {
    Rule rule1 = new Rule("pmd", "key1");
    Rule rule2 = new Rule("pmd", "key2");
    MeasuresFilter<RuleMeasure> filter = MeasuresFilters.rule(CoreMetrics.VIOLATIONS, rule1);
    List<Measure> measures = Arrays.asList(
      RuleMeasure.createForRule(CoreMetrics.VIOLATIONS, rule1, 50.0),
      RuleMeasure.createForRule(CoreMetrics.VIOLATIONS, rule2, 10.0),
      RuleMeasure.createForRule(CoreMetrics.INFO_VIOLATIONS, rule2, 3.3),

      RuleMeasure.createForPriority(CoreMetrics.VIOLATIONS, RulePriority.CRITICAL, 400.0),
      RuleMeasure.createForPriority(CoreMetrics.COVERAGE, RulePriority.CRITICAL, 400.0),
      new Measure(CoreMetrics.VIOLATIONS, 500.0));

    assertThat(filter.filter(measures).getValue(), is(50.0));
  }

  @Test
  public void rules() {
    Rule rule1 = new Rule("pmd", "key1");
    Rule rule2 = new Rule("pmd", "key2");
    MeasuresFilter<Collection<RuleMeasure>> filter = MeasuresFilters.rules(CoreMetrics.VIOLATIONS);
    List<Measure> measures = Arrays.asList(
      RuleMeasure.createForRule(CoreMetrics.VIOLATIONS, rule1, 50.0),
      RuleMeasure.createForRule(CoreMetrics.VIOLATIONS, rule2, 10.0),
      RuleMeasure.createForRule(CoreMetrics.INFO_VIOLATIONS, rule2, 3.3),

      RuleMeasure.createForPriority(CoreMetrics.VIOLATIONS, RulePriority.CRITICAL, 400.0),
      RuleMeasure.createForPriority(CoreMetrics.COVERAGE, RulePriority.CRITICAL, 400.0),
      new Measure(CoreMetrics.VIOLATIONS, 500.0));

    assertThat(filter.filter(measures).size(), is(2));
  }

  @Test
  public void measure() {
    MeasuresFilter<Measure> filter = MeasuresFilters.measure(new Measure(CoreMetrics.VIOLATIONS));
    List<Measure> measures = Arrays.asList(
      new Measure(CoreMetrics.COMMENT_LINES, 50.0),
      new Measure(CoreMetrics.VIOLATIONS, 10.0),
      RuleMeasure.createForCategory(CoreMetrics.VIOLATIONS, 2, 12.0),
      new Measure(CoreMetrics.COVERAGE, 15.0));

    assertThat(filter.filter(measures).getValue(), is(10.0));
  }
}
