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
package org.sonar.batch.cpd.decorators;

import org.sonar.batch.cpd.decorators.SumDuplicationsDecorator;

import org.junit.Test;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.test.IsMeasure;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class SumDuplicationsDecoratorTest {

  @Test
  public void parapets() {
    SumDuplicationsDecorator decorator = new SumDuplicationsDecorator();
    assertThat(decorator.generatesMetrics().size(), greaterThan(0));
    assertThat(decorator.shouldSaveZeroIfNoChildMeasures(), is(true));
  }

  @Test
  public void doNotSetDuplicationsOnUnitTests() {
    SumDuplicationsDecorator decorator = new SumDuplicationsDecorator();
    File unitTest = File.create("org/foo/BarTest.java");
    unitTest.setQualifier(Qualifiers.UNIT_TEST_FILE);
    DecoratorContext context = mock(DecoratorContext.class);

    decorator.decorate(unitTest, context);

    verify(context, never()).saveMeasure(any(Measure.class));
  }

  @Test
  public void saveZeroIfNoDuplications() {
    SumDuplicationsDecorator decorator = new SumDuplicationsDecorator();
    File file = File.create("org/foo/BarTest.java");
    DecoratorContext context = mock(DecoratorContext.class);

    decorator.decorate(file, context);

    verify(context).saveMeasure(argThat(new IsMeasure(CoreMetrics.DUPLICATED_LINES, 0.0)));
  }
}
