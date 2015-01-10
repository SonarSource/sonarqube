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
package org.sonar.core.test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import org.junit.Test;
import org.sonar.api.component.mock.MockSourceFile;
import org.sonar.api.test.MutableTestCase;
import org.sonar.core.component.ComponentVertex;
import org.sonar.core.component.ScanGraph;
import org.sonar.core.graph.BeanGraph;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultTestableTest {

  @Test
  public void not_have_tested_lines() {
    BeanGraph beanGraph = BeanGraph.createInMemory();

    DefaultTestable testable = beanGraph.createVertex(DefaultTestable.class);
    assertThat(testable.testedLines()).isEmpty();
  }

  @Test
  public void tested_lines() {
    BeanGraph beanGraph = BeanGraph.createInMemory();

    DefaultTestable testable = beanGraph.createVertex(DefaultTestable.class);
    DefaultTestCase testCase1 = beanGraph.createVertex(DefaultTestCase.class);
    testCase1.setCoverageBlock(testable, Arrays.asList(10, 11, 12));
    DefaultTestCase testCase2 = beanGraph.createVertex(DefaultTestCase.class);
    testCase2.setCoverageBlock(testable, Arrays.asList(12, 48, 49));

    assertThat(testable.testedLines()).containsOnly(10, 11, 12, 48, 49);
    assertThat(testable.countTestCasesOfLine(2)).isEqualTo(0);
    assertThat(testable.countTestCasesOfLine(10)).isEqualTo(1);
    assertThat(testable.countTestCasesOfLine(12)).isEqualTo(2);
  }

  @Test
  public void get_test_cases() {
    BeanGraph beanGraph = BeanGraph.createInMemory();

    DefaultTestable testable = beanGraph.createVertex(DefaultTestable.class);
    DefaultTestCase testCase1 = beanGraph.createVertex(DefaultTestCase.class);
    testCase1.setCoverageBlock(testable, Arrays.asList(10, 11, 12));
    DefaultTestCase testCase2 = beanGraph.createVertex(DefaultTestCase.class);
    testCase2.setCoverageBlock(testable, Arrays.asList(12, 48, 49));

    assertThat(testable.testCases()).containsOnly(testCase1, testCase2);
    assertThat(testable.testCasesOfLine(5)).isEmpty();
    assertThat(testable.testCasesOfLine(10)).containsExactly(testCase1);
    assertThat(testable.testCasesOfLine(12)).contains(testCase1, testCase2);
  }

  @Test
  public void get_test_case_by_key() {
    BeanGraph beanGraph = BeanGraph.createInMemory();

    DefaultTestPlan plan = beanGraph.createVertex(DefaultTestPlan.class);
    plan.addTestCase("T1");
    plan.addTestCase("T2");

    DefaultTestable testable = beanGraph.createVertex(DefaultTestable.class);
    MutableTestCase testCase1 = Iterables.get(plan.testCases(), 0);
    testCase1.setCoverageBlock(testable, Arrays.asList(10, 11, 12));
    MutableTestCase testCase2 = Iterables.get(plan.testCases(), 1);
    testCase2.setCoverageBlock(testable, Arrays.asList(12, 48, 49));

    assertThat(testable.testCaseByName("T1")).isEqualTo(testCase1);
    assertThat(testable.testCaseByName("T2")).isEqualTo(testCase2);
    assertThat(testable.testCaseByName("Unknown")).isNull();
  }

  @Test
  public void return_cover_of_testCase(){
    BeanGraph beanGraph = BeanGraph.createInMemory();

    ScanGraph graph = ScanGraph.create();
    ComponentVertex file1 = graph.addComponent(MockSourceFile.createMain("org.foo.Bar"));
    DefaultTestable testable1 = beanGraph.createAdjacentVertex(file1, DefaultTestable.class, "testable");

    ComponentVertex file2 = graph.addComponent(MockSourceFile.createMain("org.foo.File"));
    DefaultTestable testable2 = beanGraph.createAdjacentVertex(file2, DefaultTestable.class, "testable");

    DefaultTestPlan plan = beanGraph.createVertex(DefaultTestPlan.class);
    plan.addTestCase("T1");

    MutableTestCase testCase = Iterables.get(plan.testCases(), 0);
    testCase.setCoverageBlock(testable1, Arrays.asList(10, 11, 12));

    assertThat(testable1.coverageBlock(testCase).testCase()).isEqualTo(testCase);
    assertThat(testable1.coverageBlock(testCase).testable()).isEqualTo(testable1);
    assertThat(testable2.coverageBlock(testCase)).isNull();
  }

  @Test
  public void test_cases_by_lines() {
    BeanGraph beanGraph = BeanGraph.createInMemory();

    DefaultTestable testable = beanGraph.createVertex(DefaultTestable.class);
    DefaultTestCase testCase1 = beanGraph.createVertex(DefaultTestCase.class);
    testCase1.setCoverageBlock(testable, Arrays.asList(10, 11, 12));
    DefaultTestCase testCase2 = beanGraph.createVertex(DefaultTestCase.class);
    testCase2.setCoverageBlock(testable, Arrays.asList(12, 48, 49));

    assertThat(testable.testCasesByLines()).isEqualTo(ImmutableMap.of(49, 1, 48, 1, 10, 1, 11, 1, 12, 2));
  }
}
