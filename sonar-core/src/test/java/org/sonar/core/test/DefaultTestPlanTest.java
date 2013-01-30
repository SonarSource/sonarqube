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

import com.google.common.collect.Iterables;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.test.exception.TestCaseAlreadyExistsException;
import org.sonar.api.test.MutableTestCase;
import org.sonar.api.test.TestPlan;
import org.sonar.core.component.ComponentVertex;
import org.sonar.core.graph.BeanGraph;

import static org.fest.assertions.Assertions.assertThat;

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
    assertThat(Iterables.<MutableTestCase>get(plan.testCases(), 0).key()).isEqualTo("T1");
    assertThat(Iterables.<MutableTestCase>get(plan.testCases(), 1).key()).isEqualTo("T2");
  }

  @Test
  public void should_find_test_case_by_key() {
    BeanGraph beanGraph = BeanGraph.createInMemory();

    DefaultTestPlan plan = beanGraph.createVertex(DefaultTestPlan.class);
    plan.addTestCase("T1");
    plan.addTestCase("T2");

    assertThat(plan.testCaseByKey("T1").key()).isEqualTo("T1");
    assertThat(plan.testCaseByKey("T3")).isNull();
  }

  @Test
  public void should_set_type() {
    BeanGraph beanGraph = BeanGraph.createInMemory();

    DefaultTestPlan plan = beanGraph.createVertex(DefaultTestPlan.class);
    assertThat(plan.type()).isNull();

    plan.setType(TestPlan.TYPE_UNIT);
    assertThat(plan.type()).isEqualTo(TestPlan.TYPE_UNIT);
  }

  @Test
  public void keys_of_test_cases_should_be_unique() {
    thrown.expect(TestCaseAlreadyExistsException.class);

    BeanGraph beanGraph = BeanGraph.createInMemory();
    ComponentVertex component = beanGraph.createVertex(ComponentVertex.class);

    DefaultTestPlan plan = beanGraph.createAdjacentVertex(component, DefaultTestPlan.class, "testplan");
    plan.addTestCase("T1");
    plan.addTestCase("T1");
  }
}
