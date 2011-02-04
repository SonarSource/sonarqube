/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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

import org.junit.Test;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.MeasuresFilter;
import org.sonar.api.measures.RuleMeasure;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.test.IsMeasure;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;


public class WeightedViolationsDecoratorTest {

  private List<RuleMeasure> createViolationsMeasures() {
    return Arrays.asList(
        // categ 3
        new RuleMeasure(CoreMetrics.VIOLATIONS, new Rule(), RulePriority.INFO, 3).setValue(40.0),
        new RuleMeasure(CoreMetrics.VIOLATIONS, new Rule(), RulePriority.CRITICAL, 3).setValue(80.0),
        new RuleMeasure(CoreMetrics.VIOLATIONS, new Rule(), RulePriority.BLOCKER, 3).setValue(90.0),

        // categ 4
        new RuleMeasure(CoreMetrics.VIOLATIONS, new Rule(), RulePriority.INFO, 4).setValue(10.0),
        new RuleMeasure(CoreMetrics.VIOLATIONS, new Rule(), RulePriority.BLOCKER, 4).setValue(10.0)
    );
  }

  private Map<RulePriority, Integer> createWeights() {
    Map<RulePriority, Integer> weights = new HashMap();
    weights.put(RulePriority.BLOCKER, 10);
    weights.put(RulePriority.CRITICAL, 5);
    weights.put(RulePriority.MAJOR, 2);
    weights.put(RulePriority.MINOR, 1);
    weights.put(RulePriority.INFO, 0);
    return weights;
  }

  @Test
  public void weightedViolations() {
    Map<RulePriority, Integer> weights = createWeights();

    WeightedViolationsDecorator decorator = new WeightedViolationsDecorator(weights);
    DecoratorContext context = mock(DecoratorContext.class);

    when(context.getMeasures((MeasuresFilter) anyObject())).thenReturn(createViolationsMeasures());

    decorator.decorate(mock(Resource.class), context);

    verify(context).saveMeasure(argThat(new IsMeasure(CoreMetrics.WEIGHTED_VIOLATIONS, (double) (100 * 10 + 80 * 5 + 50 * 0))));
    verify(context).saveMeasure(argThat(new IsMeasure(CoreMetrics.WEIGHTED_VIOLATIONS, "INFO=50;CRITICAL=80;BLOCKER=100")));
  }

  @Test
  public void doNotSaveZero() {
    Map<RulePriority, Integer> weights = createWeights();

    WeightedViolationsDecorator decorator = new WeightedViolationsDecorator(weights);
    DecoratorContext context = mock(DecoratorContext.class);

    when(context.getMeasures((MeasuresFilter) anyObject())).thenReturn(Arrays.asList());

    decorator.decorate(mock(Resource.class), context);

    verify(context, never()).saveMeasure((Measure) anyObject());
  }

}
