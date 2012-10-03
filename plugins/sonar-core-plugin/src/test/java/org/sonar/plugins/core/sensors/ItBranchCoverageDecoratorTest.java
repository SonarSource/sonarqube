/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Scopes;

import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ItBranchCoverageDecoratorTest {
  private final ItBranchCoverageDecorator decorator = new ItBranchCoverageDecorator();
  private final Project resource = mock(Project.class);

  @Before
  public void setUp() {
    when(resource.getScope()).thenReturn(Scopes.PROJECT);
    when(resource.getQualifier()).thenReturn(Qualifiers.PROJECT);
  }

  @Test
  public void shouldSaveBranchCoverage() {
    DecoratorContext context = mockContext(20, 15);

    decorator.decorate(resource, context);

    verify(context).saveMeasure(CoreMetrics.IT_BRANCH_COVERAGE, 25.0);
  }

  @Test
  public void shouldNotSaveBranchCoverageIfMissingConditions() {
    DecoratorContext context = mock(DecoratorContext.class);

    decorator.decorate(resource, context);

    verify(context, never()).saveMeasure(eq(CoreMetrics.IT_BRANCH_COVERAGE), anyDouble());
  }

  private static DecoratorContext mockContext(int conditions, int uncoveredConditions) {
    DecoratorContext context = mock(DecoratorContext.class);
    when(context.getMeasure(CoreMetrics.IT_CONDITIONS_TO_COVER)).thenReturn(new Measure(CoreMetrics.IT_CONDITIONS_TO_COVER, (double) conditions));
    when(context.getMeasure(CoreMetrics.IT_UNCOVERED_CONDITIONS)).thenReturn(new Measure(CoreMetrics.IT_UNCOVERED_CONDITIONS, (double) uncoveredConditions));
    return context;
  }
}
