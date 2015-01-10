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
import org.sonar.api.component.mock.MockSourceFile;
import org.sonar.api.test.CoverageBlock;
import org.sonar.api.test.TestCase;
import org.sonar.api.test.exception.CoverageAlreadyExistsException;
import org.sonar.api.test.exception.IllegalDurationException;
import org.sonar.core.component.ComponentVertex;
import org.sonar.core.component.ScanGraph;
import org.sonar.core.graph.BeanGraph;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultTestCaseTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void no_coverage_blocks() {
    BeanGraph beanGraph = BeanGraph.createInMemory();
    DefaultTestCase testCase = beanGraph.createVertex(DefaultTestCase.class);

    assertThat(testCase.doesCover()).isFalse();
    assertThat(testCase.countCoveredLines()).isEqualTo(0);
    assertThat(testCase.coverageBlocks()).isEmpty();
  }

  @Test
  public void should_cover_testable() {
    BeanGraph beanGraph = BeanGraph.createInMemory();
    DefaultTestable testable = beanGraph.createVertex(DefaultTestable.class);
    DefaultTestCase testCase = beanGraph.createVertex(DefaultTestCase.class);
    testCase.setCoverageBlock(testable, Arrays.asList(10, 11, 12));

    assertThat(testCase.doesCover()).isTrue();
    assertThat(testCase.countCoveredLines()).isEqualTo(3);
    assertThat(testCase.coverageBlocks()).hasSize(1);

    CoverageBlock cover = Iterables.getFirst(testCase.coverageBlocks(), null);
    assertThat(cover.testCase()).isEqualTo(testCase);
    assertThat(cover.testable()).isSameAs(testable);
    assertThat(cover.lines()).containsExactly(10, 11, 12);
  }

  @Test
  public void should_cover_multiple_testables() {
    ScanGraph graph = ScanGraph.create();
    ComponentVertex file1 = graph.addComponent(MockSourceFile.createMain("org.foo.Bar"));
    DefaultTestable testable1 = graph.createAdjacentVertex(file1, DefaultTestable.class, "testable");
    ComponentVertex file2 = graph.addComponent(MockSourceFile.createMain("org.foo.File"));
    DefaultTestable testable2 = graph.createAdjacentVertex(file2, DefaultTestable.class, "testable");
    DefaultTestCase testCase = graph.createVertex(DefaultTestCase.class);

    testCase.setCoverageBlock(testable1, Arrays.asList(10, 11, 12));
    testCase.setCoverageBlock(testable2, Arrays.asList(12, 13, 14));

    assertThat(testCase.doesCover()).isTrue();
    assertThat(testCase.countCoveredLines()).isEqualTo(6);
    assertThat(testCase.coverageBlocks()).hasSize(2);
  }

  @Test
  public void should_return_cover_of_testable() {
    BeanGraph beanGraph = BeanGraph.createInMemory();

    ScanGraph graph = ScanGraph.create();
    ComponentVertex file1 = graph.addComponent(MockSourceFile.createMain("org.foo.Bar"));
    DefaultTestable testable1 = beanGraph.createAdjacentVertex(file1, DefaultTestable.class, "testable");

    ComponentVertex file2 = graph.addComponent(MockSourceFile.createMain("org.foo.File"));
    DefaultTestable testable2 = beanGraph.createAdjacentVertex(file2, DefaultTestable.class, "testable");

    DefaultTestCase testCase = beanGraph.createVertex(DefaultTestCase.class);
    testCase.setCoverageBlock(testable1, Arrays.asList(10, 11, 12));

    assertThat(testCase.coverageBlock(testable1).testable()).isEqualTo(testable1);
    assertThat(testCase.coverageBlock(testable1).testCase()).isEqualTo(testCase);
    assertThat(testCase.coverageBlock(testable2)).isNull();
  }

  @Test
  public void should_set_metadata() {
    BeanGraph beanGraph = BeanGraph.createInMemory();
    DefaultTestCase testCase = beanGraph.createVertex(DefaultTestCase.class);

    testCase.setName("T1")
      .setDurationInMs(1234L)
      .setMessage("Error msg")
      .setStackTrace("xxx")
      .setType(TestCase.TYPE_UNIT)
      .setStatus(TestCase.Status.ERROR);

    assertThat(testCase.name()).isEqualTo("T1");
    assertThat(testCase.message()).isEqualTo("Error msg");
    assertThat(testCase.stackTrace()).isEqualTo("xxx");
    assertThat(testCase.durationInMs()).isEqualTo(1234L);
    assertThat(testCase.status()).isEqualTo(TestCase.Status.ERROR);
    assertThat(testCase.type()).isEqualTo(TestCase.TYPE_UNIT);
  }

  @Test
  public void duration_should_be_positive() {
    thrown.expect(IllegalDurationException.class);
    thrown.expectMessage("Test duration must be positive (got: -1234)");

    BeanGraph beanGraph = BeanGraph.createInMemory();
    DefaultTestCase testCase = beanGraph.createVertex(DefaultTestCase.class);

    testCase.setDurationInMs(-1234L);
  }

  @Test
  public void should_fail_if_coverage_block_already_exits() {
    thrown.expect(CoverageAlreadyExistsException.class);

    ScanGraph graph = ScanGraph.create();

    ComponentVertex file = graph.addComponent(MockSourceFile.createMain("org.foo.Bar"));
    DefaultTestable testable = graph.createAdjacentVertex(file, DefaultTestable.class, "testable");

    DefaultTestCase testCase = graph.createVertex(DefaultTestCase.class);
    testCase.setCoverageBlock(testable, Arrays.asList(10, 11, 12));

    // error
    testCase.setCoverageBlock(testable, Arrays.asList(20));
  }
}
