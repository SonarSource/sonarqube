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

import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;
import org.junit.Test;
import org.sonar.api.component.SourceFile;
import org.sonar.api.component.mock.MockSourceFile;
import org.sonar.api.test.MutableTestCase;
import org.sonar.api.test.MutableTestPlan;
import org.sonar.core.component.ComponentGraph;
import org.sonar.core.component.ComponentWrapper;
import org.sonar.core.graph.graphson.GraphsonReader;

import static org.fest.assertions.Assertions.assertThat;

public class TestPlanBuilderTest {
  @Test
  public void should_create_empty_plan() {
    ComponentGraph graph = new ComponentGraph();
    SourceFile file = MockSourceFile.createMain("org/codehaus/sonar/Main.java");
    ComponentWrapper fileWrapper = graph.createComponent(file);

    MutableTestPlan plan = new TestPlanBuilder().build(fileWrapper);
    assertThat(plan).isNotNull();
    assertThat(plan.component().getKey()).isEqualTo(file.getKey());
    assertThat(plan.component().getQualifier()).isEqualTo(file.getQualifier());
    assertThat(plan.component().getName()).isEqualTo(file.getName());
    assertThat(plan.testCases()).isEmpty();
  }

  @Test
  public void should_add_test_case() {
    ComponentGraph graph = new ComponentGraph();
    SourceFile file = MockSourceFile.createMain("org/codehaus/sonar/Main.java");
    ComponentWrapper fileWrapper = graph.createComponent(file);

    MutableTestPlan plan = new TestPlanBuilder().build(fileWrapper);
    MutableTestCase testCase = plan.addTestCase("should_pass");
    assertThat(testCase.key()).isEqualTo("should_pass");
    assertThat(testCase.name()).isNull();
    assertThat(plan.testCases()).hasSize(1);
    assertThat(plan.testCases()).containsExactly(testCase);
  }

  @Test
  public void should_load_test_plan() {
    TinkerGraph graph = new TinkerGraph();
    new GraphsonReader().read(getClass().getResourceAsStream("/org/sonar/core/test/TestPlanBuilderTest/plan_with_test_cases.json"), graph);

    Vertex componentVertex = graph.getVertex("33");
    ComponentGraph componentGraph = new ComponentGraph(graph, componentVertex);

    MutableTestPlan testPlan = new TestPlanBuilder().build(componentGraph.wrap(componentVertex, ComponentWrapper.class));
    assertThat(testPlan.testCases()).hasSize(4);
  }

}
