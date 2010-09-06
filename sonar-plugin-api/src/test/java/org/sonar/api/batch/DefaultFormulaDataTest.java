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
package org.sonar.api.batch;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.*;
import org.sonar.api.measures.MeasuresFilter;
import org.sonar.api.measures.Metric;

import java.util.Arrays;

public class DefaultFormulaDataTest {

  @Test
  public void isDecoratorContextProxy() {
    DecoratorContext context = mock(DecoratorContext.class);
    DefaultFormulaData data = new DefaultFormulaData(context);

    data.getChildrenMeasures((MeasuresFilter) anyObject());
    verify(context).getChildrenMeasures((MeasuresFilter) anyObject());

    data.getChildrenMeasures((Metric) anyObject());
    verify(context).getChildrenMeasures((Metric) anyObject());

    data.getMeasures((MeasuresFilter) anyObject());
    verify(context).getMeasures((MeasuresFilter) anyObject());

    data.getMeasure((Metric) anyObject());
    verify(context).getMeasure((Metric) anyObject());
  }

  @Test
  public void getChildren() {
    DecoratorContext context = mock(DecoratorContext.class);
    DecoratorContext child1 = mock(DecoratorContext.class);
    DecoratorContext child2 = mock(DecoratorContext.class);
    when(context.getChildren()).thenReturn(Arrays.asList(child1, child2));

    DefaultFormulaData data = new DefaultFormulaData(context);
    assertThat(data.getChildren().size(), is(2));
  }
}
