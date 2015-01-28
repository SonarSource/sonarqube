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
package org.sonar.api.measures;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.resources.Directory;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Scopes;

import java.util.Collections;
import java.util.List;

import static junit.framework.Assert.assertNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SumChildDistributionFormulaTest {
  SumChildDistributionFormula formula;
  FormulaContext context;
  FormulaData data;

  @Before
  public void init() {
    formula = new SumChildDistributionFormula();
    context = mock(FormulaContext.class);
    when(context.getResource()).thenReturn(File.create("foo"));
    data = mock(FormulaData.class);
  }

  @Test
  public void testWhenGetChildrenReturnsNull() {
    when(context.getTargetMetric()).thenReturn(new Metric("foo"));
    when(data.getChildrenMeasures(new Metric("foo"))).thenReturn(null);
    assertNull(formula.calculate(data, context));
  }

  @Test
  public void testWhenGetChildrenReturnsEmpty() {
    when(context.getTargetMetric()).thenReturn(new Metric("foo"));
    when(data.getChildrenMeasures(new Metric("foo"))).thenReturn(Collections.<Measure>emptyList());
    assertNull(formula.calculate(data, context));
  }

  @Test
  public void shouldNotSumDifferentRanges() {
    Metric m = new Metric("foo", Metric.ValueType.DATA);
    when(context.getTargetMetric()).thenReturn(m);

    List<Measure> list = Lists.newArrayList(
      new Measure(m, "1=0;2=2;5=0;10=10;20=2"),
      new Measure(m, "1=0;2=2;5=0;10=10;30=3")
      );
    when(data.getChildrenMeasures(new Metric("foo"))).thenReturn(list);
    assertThat(formula.calculate(data, context), nullValue());
  }

  @Test
  public void shouldSumSameIntRanges() {
    Metric m = new Metric("foo", Metric.ValueType.DATA);
    when(context.getTargetMetric()).thenReturn(m);

    List<Measure> list = Lists.newArrayList(
      new Measure(m, "1=0;2=2;5=0;10=10;20=2"),
      new Measure(m, "1=3;2=2;5=3;10=12;20=0")
      );
    when(data.getChildrenMeasures(new Metric("foo"))).thenReturn(list);
    assertThat(formula.calculate(data, context).getData(), is("1=3;2=4;5=3;10=22;20=2"));
  }

  @Test
  public void shouldSumSameDoubleRanges() {
    initContextWithChildren();
    assertThat(formula.calculate(data, context).getData(), is("0.5=3;2.5=6"));
  }

  @Test
  public void shouldNotPersistWhenScopeLowerThanMinimun() throws Exception {
    when(context.getResource()).thenReturn(File.create("org/Foo.java"));

    initContextWithChildren();
    formula.setMinimumScopeToPersist(Scopes.DIRECTORY);

    Measure distribution = formula.calculate(data, context);
    assertThat(distribution.getPersistenceMode().useDatabase(), is(false));
  }

  @Test
  public void shouldPersistWhenScopeEqualsMinimun() throws Exception {
    when(context.getResource()).thenReturn(File.create("org/Foo.java"));

    initContextWithChildren();
    formula.setMinimumScopeToPersist(Scopes.FILE);

    Measure distribution = formula.calculate(data, context);
    assertThat(distribution.getPersistenceMode().useDatabase(), is(true));
  }

  @Test
  public void shouldPersistWhenScopeHigherThanMinimun() throws Exception {
    when(context.getResource()).thenReturn(Directory.create("org/foo"));

    initContextWithChildren();
    formula.setMinimumScopeToPersist(Scopes.FILE);

    Measure distribution = formula.calculate(data, context);
    assertThat(distribution.getPersistenceMode().useDatabase(), is(true));
  }

  private void initContextWithChildren() {
    Metric m = new Metric("foo", Metric.ValueType.DATA);
    when(context.getTargetMetric()).thenReturn(m);
    List<Measure> list = Lists.newArrayList(
      new Measure(m, "0.5=0;2.5=2"),
      new Measure(m, "0.5=3;2.5=4")
      );
    when(data.getChildrenMeasures(new Metric("foo"))).thenReturn(list);
  }
}
