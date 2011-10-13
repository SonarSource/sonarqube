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
package org.sonar.plugins.jacoco;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.jacoco.core.analysis.ISourceFileCoverage;
import org.junit.Test;
import org.sonar.api.resources.JavaFile;

public class AbstractAnalyzerTest {
  @Test
  public void defaultPackage() {
    ISourceFileCoverage coverage = mock(ISourceFileCoverage.class);
    when(coverage.getPackageName()).thenReturn("").thenReturn("org/example");
    when(coverage.getName()).thenReturn("Hello.java");
    assertThat(AbstractAnalyzer.getResource(coverage), is(new JavaFile("[default].Hello")));
    assertThat(AbstractAnalyzer.getResource(coverage), is(new JavaFile("org.example.Hello")));
  }
}
