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
package org.sonar.plugins.squid.decorators;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Test;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.JavaPackage;
import org.sonar.api.resources.Project;
import org.sonar.java.api.JavaClass;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ClassComplexityDistributionBuilderTest {

  @Test
  public void shouldExecuteOnJavaProjectsOnly() throws Exception {
    ClassComplexityDistributionBuilder builder = new ClassComplexityDistributionBuilder();
    assertThat(builder.shouldExecuteOnProject(new Project("java").setLanguageKey(Java.KEY)), is(true));
    assertThat(builder.shouldExecuteOnProject(new Project("php").setLanguageKey("php")), is(false));
  }

  @Test
  public void shouldExecuteOnFilesOnly() throws Exception {
    ClassComplexityDistributionBuilder builder = new ClassComplexityDistributionBuilder();
    DecoratorContext context = mock(DecoratorContext.class);
    when(context.getMeasure(CoreMetrics.COMPLEXITY)).thenReturn(new Measure(CoreMetrics.COMPLEXITY, 20.0));

    assertThat(builder.shouldExecuteOn(new JavaPackage("org.foo"), context), is(false));
    assertThat(builder.shouldExecuteOn(new JavaFile("org.foo.Bar"), context), is(true));
    assertThat(builder.shouldExecuteOn(JavaClass.create("org.foo.Bar"), context), is(false));
  }

  @Test
  public void shouldCalculateDistributionOnFile() throws Exception {

    List<DecoratorContext> children = Arrays.asList(
        // first range
        newClassChild("One", 2.0), newClassChild("Two", 1.0), newClassChild("Zero complexity", 0.0),

        // second range
        newClassChild("Three", 8.0),

        // out of range
        newClassChild("No complexity", null));

    DecoratorContext context = mock(DecoratorContext.class);
    when(context.getMeasure(CoreMetrics.COMPLEXITY)).thenReturn(new Measure(CoreMetrics.COMPLEXITY, 20.0));
    when(context.getChildren()).thenReturn(children);

    ClassComplexityDistributionBuilder builder = new ClassComplexityDistributionBuilder();
    builder.decorate(JavaFile.fromRelativePath("org/foo/MyFile.java", false), context);

    verify(context).saveMeasure(argThat(new BaseMatcher<Measure>() {
      public boolean matches(Object o) {
        Measure measure = (Measure) o;
        return measure.getMetric().equals(CoreMetrics.CLASS_COMPLEXITY_DISTRIBUTION) &&
            measure.getData().equals("0=3;5=1;10=0;20=0;30=0;60=0;90=0");
      }

      public void describeTo(Description description) {

      }
    }));
  }

  private DecoratorContext newClassChild(String classname, Double complexity) {
    DecoratorContext context = mock(DecoratorContext.class);
    when(context.getResource()).thenReturn(JavaClass.create(classname));
    when(context.getMeasure(CoreMetrics.COMPLEXITY)).thenReturn(new Measure(CoreMetrics.COMPLEXITY, complexity));
    return context;
  }
}
