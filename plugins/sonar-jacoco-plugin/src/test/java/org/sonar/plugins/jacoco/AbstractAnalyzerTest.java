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
package org.sonar.plugins.jacoco;

import org.jacoco.core.analysis.ISourceFileCoverage;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.Resource;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AbstractAnalyzerTest {
  ISourceFileCoverage coverage = mock(ISourceFileCoverage.class);
  SensorContext context = mock(SensorContext.class);

  @Test
  public void should_recognize_default_package() {
    when(coverage.getPackageName()).thenReturn("");
    when(coverage.getName()).thenReturn("Hello.java");
    when(context.getResource(any(Resource.class))).thenAnswer(sameResource());

    JavaFile resource = AbstractAnalyzer.getResource(coverage, context);

    assertThat(resource).isEqualTo(new JavaFile("[default].Hello"));
  }

  @Test
  public void should_recognize_non_default_package() {
    when(coverage.getPackageName()).thenReturn("org/example");
    when(coverage.getName()).thenReturn("Hello.java");
    when(context.getResource(any(Resource.class))).thenAnswer(sameResource());

    JavaFile resource = AbstractAnalyzer.getResource(coverage, context);

    assertThat(resource).isEqualTo(new JavaFile("org.example.Hello"));
  }

  @Test
  public void should_ignore_resource_not_found_in_context() {
    when(coverage.getPackageName()).thenReturn("org/example");
    when(coverage.getName()).thenReturn("HelloTest.java");
    when(context.getResource(any(Resource.class))).thenReturn(null);

    JavaFile resource = AbstractAnalyzer.getResource(coverage, context);

    assertThat(resource).isNull();
  }

  @Test
  public void should_ignore_unit_tests() {
    when(coverage.getPackageName()).thenReturn("org/example");
    when(coverage.getName()).thenReturn("HelloTest.java");
    when(context.getResource(any(Resource.class))).thenReturn(new JavaFile("HelloTest.java", true));

    JavaFile resource = AbstractAnalyzer.getResource(coverage, context);

    assertThat(resource).isNull();
  }

  static Answer<Resource> sameResource() {
    return new Answer<Resource>() {
      public Resource answer(InvocationOnMock invocation) {
        return (Resource) invocation.getArguments()[0];
      }
    };
  }
}
