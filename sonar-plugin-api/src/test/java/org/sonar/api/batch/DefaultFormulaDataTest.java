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
package org.sonar.api.batch;

import org.junit.Test;
import org.sonar.api.measures.MeasuresFilter;
import org.sonar.api.measures.Metric;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DefaultFormulaDataTest {

  @Test
  public void isDecoratorContextProxy() {
    DecoratorContext context = mock(DecoratorContext.class);
    DefaultFormulaData data = new DefaultFormulaData(context);

    data.getChildrenMeasures(any(MeasuresFilter.class));
    verify(context).getChildrenMeasures(any(MeasuresFilter.class));

    data.getChildrenMeasures(any(Metric.class));
    verify(context).getChildrenMeasures(any(Metric.class));

    data.getMeasures(any(MeasuresFilter.class));
    verify(context).getMeasures(any(MeasuresFilter.class));

    data.getMeasure(any(Metric.class));
    verify(context).getMeasure(any(Metric.class));
  }

  @Test
  public void getChildren() {
    DecoratorContext context = mock(DecoratorContext.class);
    DecoratorContext child1 = mock(DecoratorContext.class);
    DecoratorContext child2 = mock(DecoratorContext.class);
    when(context.getChildren()).thenReturn(Arrays.asList(child1, child2));

    DefaultFormulaData data = new DefaultFormulaData(context);
    assertThat(data.getChildren()).hasSize(2);
  }
}
