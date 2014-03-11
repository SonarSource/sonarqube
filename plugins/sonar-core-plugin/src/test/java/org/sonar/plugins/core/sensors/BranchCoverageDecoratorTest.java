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

public class BranchCoverageDecoratorTest {
  private final BranchCoverageDecorator decorator = new BranchCoverageDecorator();
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

    verify(context).saveMeasure(CoreMetrics.BRANCH_COVERAGE, 25.0);
  }

  @Test
  public void shouldNotSaveBranchCoverageIfMissingConditions() {
    DecoratorContext context = mock(DecoratorContext.class);

    decorator.decorate(resource, context);

    verify(context, never()).saveMeasure(eq(CoreMetrics.BRANCH_COVERAGE), anyDouble());
  }

  private static DecoratorContext mockContext(int conditions, int uncoveredConditions) {
    DecoratorContext context = mock(DecoratorContext.class);
    when(context.getMeasure(CoreMetrics.CONDITIONS_TO_COVER)).thenReturn(new Measure(CoreMetrics.CONDITIONS_TO_COVER, (double) conditions));
    when(context.getMeasure(CoreMetrics.UNCOVERED_CONDITIONS)).thenReturn(new Measure(CoreMetrics.UNCOVERED_CONDITIONS, (double) uncoveredConditions));
    return context;
  }
}
