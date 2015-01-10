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

import com.google.common.collect.Iterables;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.test.MutableTestCase;
import org.sonar.api.test.TestPlan;
import org.sonar.core.graph.BeanGraph;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultTestPlanTest {
  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void should_not_have_test_cases() {
    BeanGraph beanGraph = BeanGraph.createInMemory();

    DefaultTestPlan plan = beanGraph.createVertex(DefaultTestPlan.class);
    assertThat(plan.testCases()).isEmpty();
  }

  @Test
  public void should_add_test_cases() {
    BeanGraph beanGraph = BeanGraph.createInMemory();

    DefaultTestPlan plan = beanGraph.createVertex(DefaultTestPlan.class);
    plan.addTestCase("T1");
    plan.addTestCase("T2");

    assertThat(plan.testCases()).hasSize(2);
    MutableTestCase firstTestCase = Iterables.get(plan.testCases(), 0);
    assertThat(firstTestCase.name()).isEqualTo("T1");
    assertThat(firstTestCase.testPlan()).isSameAs(plan);

    MutableTestCase secondTestCase = Iterables.get(plan.testCases(), 1);
    assertThat(secondTestCase.name()).isEqualTo("T2");
    assertThat(secondTestCase.testPlan()).isSameAs(plan);
  }

  @Test
  public void should_find_test_case_by_name() {
    BeanGraph beanGraph = BeanGraph.createInMemory();

    DefaultTestPlan plan = beanGraph.createVertex(DefaultTestPlan.class);
    plan.addTestCase("T1");
    plan.addTestCase("T2");

    assertThat(plan.testCasesByName("T1")).hasSize(1);
    assertThat(Iterables.get(plan.testCasesByName("T1"), 0).name()).isEqualTo("T1");
    assertThat(plan.testCasesByName("T3")).isEmpty();
  }

  @Test
  public void should_find_multiple_test_cases_by_name() {
    BeanGraph beanGraph = BeanGraph.createInMemory();

    DefaultTestPlan plan = beanGraph.createVertex(DefaultTestPlan.class);
    plan.addTestCase("T1");
    plan.addTestCase("T1");

    assertThat(plan.testCasesByName("T1")).hasSize(2);
  }
}
