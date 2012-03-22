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
package org.sonar.plugins.squid.decorators;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.PersistenceMode;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.JavaPackage;
import org.sonar.api.resources.Project;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class FileComplexityDistributionDecoratorTest {

  private FileComplexityDistributionDecorator decorator;

  @Before
  public void setUp() {
    decorator = new FileComplexityDistributionDecorator();
  }

  @Test
  public void shouldExecuteOnJavaProjectsOnly() throws Exception {
    assertThat(decorator.shouldExecuteOnProject(new Project("java").setLanguageKey(Java.KEY)), is(true));
    assertThat(decorator.shouldExecuteOnProject(new Project("php").setLanguageKey("php")), is(false));
  }

  @Test
  public void shouldExecuteOnFilesOnly() throws Exception {
    DecoratorContext context = mock(DecoratorContext.class);
    when(context.getMeasure(CoreMetrics.COMPLEXITY)).thenReturn(new Measure(CoreMetrics.COMPLEXITY, 20.0));

    decorator.decorate(new JavaPackage("org.foo"), context);
    verify(context, never()).saveMeasure(any(Measure.class));
  }

  @Test
  public void shouldDependOnComplexity() {
    DecoratorContext context = mock(DecoratorContext.class);
    assertThat(decorator.dependOnComplexity(), is(CoreMetrics.COMPLEXITY));
    assertThat(decorator.generatesComplexityDistribution(), is(CoreMetrics.FILE_COMPLEXITY_DISTRIBUTION));

    decorator.decorate(new JavaFile("org.foo.Bar"), context);
    verify(context, never()).saveMeasure(any(Measure.class));
  }

  @Test
  public void shouldCalculateDistributionOnFile() throws Exception {
    DecoratorContext context = mock(DecoratorContext.class);
    when(context.getMeasure(CoreMetrics.COMPLEXITY)).thenReturn(new Measure(CoreMetrics.COMPLEXITY, 20.0));

    decorator.decorate(JavaFile.fromRelativePath("org/foo/MyFile.java", false), context);
    ArgumentCaptor<Measure> measureCaptor = ArgumentCaptor.forClass(Measure.class);
    verify(context).saveMeasure(measureCaptor.capture());
    Measure measure = measureCaptor.getValue();
    assertThat(measure.getMetric(), is(CoreMetrics.FILE_COMPLEXITY_DISTRIBUTION));
    assertThat(measure.getData(), is("0=0;5=0;10=0;20=1;30=0;60=0;90=0"));
    assertThat(measure.getPersistenceMode(), is(PersistenceMode.MEMORY));
  }

}
