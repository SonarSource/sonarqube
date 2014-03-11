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
package org.sonar.plugins.core.timemachine;

import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Matchers;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NewCoverageAggregatorTest {

  @Test
  public void shouldNotSaveDataWhenNoMeasures() {
    NewCoverageAggregator aggregator = new NewCoverageAggregator();
    DecoratorContext context = mock(DecoratorContext.class);
    when(context.getChildrenMeasures(CoreMetrics.NEW_LINES_TO_COVER)).thenReturn(Collections.<Measure>emptyList());

    aggregator.aggregate(context, CoreMetrics.NEW_LINES_TO_COVER, 3);

    verify(context, never()).saveMeasure(Matchers.<Measure>anyObject());
  }

  @Test
  public void shouldNotsetZeroWhenNoValueOnPeriod() {
    NewCoverageAggregator aggregator = new NewCoverageAggregator();
    DecoratorContext context = mock(DecoratorContext.class);
    when(context.getChildrenMeasures(CoreMetrics.NEW_LINES_TO_COVER)).thenReturn(Arrays.asList(newMeasure(null, 3.0, 2.0), newMeasure(null, 13.0, null)));

    aggregator.aggregate(context, CoreMetrics.NEW_LINES_TO_COVER, 3);

    verify(context).saveMeasure(argThat(new ArgumentMatcher<Measure>() {
      @Override
      public boolean matches(Object o) {
        Measure m = (Measure)o;
        return m.getVariation1()==null;
      }
    }));
  }

  @Test
  public void shouldSumValues() {
    NewCoverageAggregator aggregator = new NewCoverageAggregator();
    DecoratorContext context = mock(DecoratorContext.class);
    when(context.getChildrenMeasures(CoreMetrics.NEW_LINES_TO_COVER)).thenReturn(Arrays.asList(newMeasure(null, 3.0, 2.0), newMeasure(null, 13.0, null)));

    aggregator.aggregate(context, CoreMetrics.NEW_LINES_TO_COVER, 3);

    verify(context).saveMeasure(argThat(new ArgumentMatcher<Measure>() {
      @Override
      public boolean matches(Object o) {
        Measure m = (Measure)o;
        return m.getVariation2()==16.0 && m.getVariation3()==2.0;
      }
    }));
  }

  private Measure newMeasure(Double variation1, Double variation2, Double variation3) {
    return new Measure(CoreMetrics.NEW_LINES_TO_COVER)
        .setVariation1(variation1)
        .setVariation2(variation2)
        .setVariation3(variation3);
  }
}
