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
import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.Project;

public class BranchCoverageDecoratorTest {

  @Test
  public void noBranchCoverageIfMissingConditions() {
    Project resource = mock(Project.class);
    when(resource.getScope()).thenReturn(Project.SCOPE_SET);
    when(resource.getQualifier()).thenReturn(Project.QUALIFIER_SUBVIEW);

    DecoratorContext context = mockContext(null, null);
    new BranchCoverageDecorator().decorate(resource, context);

    verify(context, never()).saveMeasure(eq(CoreMetrics.BRANCH_COVERAGE), anyDouble());
  }

  @Test
  public void branchCoverage() {
    Project resource = mock(Project.class);
    when(resource.getScope()).thenReturn(Project.SCOPE_SET);
    when(resource.getQualifier()).thenReturn(Project.QUALIFIER_PROJECT);

    DecoratorContext context = mockContext(20, 15);

    new BranchCoverageDecorator().decorate(resource, context);
    verify(context).saveMeasure(CoreMetrics.BRANCH_COVERAGE, 25.0);
  }


  private DecoratorContext mockContext(Integer conditions, Integer uncoveredConditions) {
    DecoratorContext context = mock(DecoratorContext.class);
    if (conditions != null) {
      when(context.getMeasure(CoreMetrics.CONDITIONS_TO_COVER)).thenReturn(new Measure(CoreMetrics.CONDITIONS_TO_COVER, conditions.doubleValue()));
    }
    if (uncoveredConditions != null) {
      when(context.getMeasure(CoreMetrics.UNCOVERED_CONDITIONS)).thenReturn(new Measure(CoreMetrics.UNCOVERED_CONDITIONS, uncoveredConditions.doubleValue()));
    }
    return context;
  }
}
