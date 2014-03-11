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

package org.sonar.plugins.core.sensors;

import org.junit.Test;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.test.IsMeasure;

import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CommentDensityDecoratorTest {

  @Test
  public void densityIsBalancedByNcloc() {
    DecoratorContext context = mock(DecoratorContext.class);
    when(context.getMeasure(CoreMetrics.NCLOC)).thenReturn(new Measure(CoreMetrics.NCLOC, 300.0));
    when(context.getMeasure(CoreMetrics.COMMENT_LINES)).thenReturn(new Measure(CoreMetrics.COMMENT_LINES, 200.0));
    CommentDensityDecorator decorator = new CommentDensityDecorator();
    decorator.decorate(null, context);
    // 200 / (200 + 300) = 40%
    verify(context).saveMeasure(argThat(new IsMeasure(CoreMetrics.COMMENT_LINES_DENSITY, 40.0)));
  }

  @Test
  public void noDensityIfUnknownComments() {
    DecoratorContext context = mock(DecoratorContext.class);
    when(context.getMeasure(CoreMetrics.NCLOC)).thenReturn(new Measure(CoreMetrics.NCLOC, 300.0));
    CommentDensityDecorator decorator = new CommentDensityDecorator();
    decorator.decorate(null, context);
    verify(context, never()).saveMeasure(argThat(new IsMeasure(CoreMetrics.COMMENT_LINES_DENSITY)));
  }

  @Test
  public void noDensityIfZeroNcloc() {
    DecoratorContext context = mock(DecoratorContext.class);
    when(context.getMeasure(CoreMetrics.NCLOC)).thenReturn(new Measure(CoreMetrics.NCLOC, 0.0));
    when(context.getMeasure(CoreMetrics.COMMENT_LINES)).thenReturn(new Measure(CoreMetrics.COMMENT_LINES, 0.0));
    CommentDensityDecorator decorator = new CommentDensityDecorator();
    decorator.decorate(null, context);
    verify(context, never()).saveMeasure(argThat(new IsMeasure(CoreMetrics.COMMENT_LINES_DENSITY)));
  }

  @Test
  public void zeroDensityWhenZeroComments() {
    DecoratorContext context = mock(DecoratorContext.class);
    when(context.getMeasure(CoreMetrics.NCLOC)).thenReturn(new Measure(CoreMetrics.NCLOC, 40.0));
    when(context.getMeasure(CoreMetrics.COMMENT_LINES)).thenReturn(new Measure(CoreMetrics.COMMENT_LINES, 0.0));
    CommentDensityDecorator decorator = new CommentDensityDecorator();
    decorator.decorate(null, context);
    verify(context).saveMeasure(argThat(new IsMeasure(CoreMetrics.COMMENT_LINES_DENSITY, 0.0)));
  }
}
