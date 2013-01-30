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
package org.sonar.core.test;

import org.junit.Test;
import org.sonar.core.graph.BeanGraph;

import java.util.Arrays;

import static org.fest.assertions.Assertions.assertThat;

public class DefaultTestableTest {
  @Test
  public void should_not_have_tested_lines() {
    BeanGraph beanGraph = BeanGraph.createInMemory();

    DefaultTestable testable = beanGraph.createVertex(DefaultTestable.class);
    assertThat(testable.testedLines()).isEmpty();
  }

  @Test
  public void should_have_tested_lines() {
    BeanGraph beanGraph = BeanGraph.createInMemory();

    DefaultTestable testable = beanGraph.createVertex(DefaultTestable.class);
    DefaultTestCase testCase1 = beanGraph.createVertex(DefaultTestCase.class);
    testCase1.setCover(testable, Arrays.asList(10, 11, 12));
    DefaultTestCase testCase2 = beanGraph.createVertex(DefaultTestCase.class);
    testCase2.setCover(testable, Arrays.asList(12, 48, 49));

    assertThat(testable.testedLines()).containsOnly(10, 11, 12, 48, 49);
  }

  @Test
  public void should_get_test_cases() {
    BeanGraph beanGraph = BeanGraph.createInMemory();

    DefaultTestable testable = beanGraph.createVertex(DefaultTestable.class);
    DefaultTestCase testCase1 = beanGraph.createVertex(DefaultTestCase.class);
    testCase1.setCover(testable, Arrays.asList(10, 11, 12));
    DefaultTestCase testCase2 = beanGraph.createVertex(DefaultTestCase.class);
    testCase2.setCover(testable, Arrays.asList(12, 48, 49));

    assertThat(testable.testCases()).containsOnly(testCase1, testCase2);
    assertThat(testable.testCasesOfLine(5)).isEmpty();
    assertThat(testable.testCasesOfLine(10)).containsExactly(testCase1);
    assertThat(testable.testCasesOfLine(12)).contains(testCase1, testCase2);
  }
}
