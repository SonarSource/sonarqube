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
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.File;
import org.sonar.api.test.IsMeasure;
import org.sonar.core.metric.DefaultMetricFinder;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ManualMeasureDecoratorTest extends AbstractDbUnitTestCase {

  private Metric reviewNote = new Metric.Builder("review_note", "Note", Metric.ValueType.FLOAT).create().setId(2);

  @Test
  public void testCopyManualMeasures() throws Exception {
    setupData("testCopyManualMeasures");

    File javaFile = File.create("Foo.java");
    javaFile.setId(40);

    ManualMeasureDecorator decorator = new ManualMeasureDecorator(getSession(), new DefaultMetricFinder(getSessionFactory()));
    DecoratorContext context = mock(DecoratorContext.class);
    decorator.decorate(javaFile, context);

    verify(context).saveMeasure(argThat(new IsMeasure(reviewNote, 6.0, "six")));
  }

}
