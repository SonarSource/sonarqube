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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.test.exception.IllegalDurationException;
import org.sonar.api.test.TestCase;
import org.sonar.core.graph.BeanGraph;

import java.util.Arrays;

import static org.fest.assertions.Assertions.assertThat;

public class DefaultTestCaseTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void no_coverage_data() {
    BeanGraph beanGraph = BeanGraph.createInMemory();
    DefaultTestCase testCase = beanGraph.createVertex(DefaultTestCase.class);

    assertThat(testCase.doesCover()).isFalse();
    assertThat(testCase.countCoveredLines()).isEqualTo(0);
    assertThat(testCase.covers()).isEmpty();
  }

  @Test
  public void should_cover_testables() {
    BeanGraph beanGraph = BeanGraph.createInMemory();
    DefaultTestable testable1 = beanGraph.createVertex(DefaultTestable.class);
    DefaultTestable testable2 = beanGraph.createVertex(DefaultTestable.class);
    DefaultTestCase testCase = beanGraph.createVertex(DefaultTestCase.class);

    testCase.setCover(testable1, Arrays.asList(10, 11, 12));
    testCase.setCover(testable2, Arrays.asList(12, 13, 14));

    assertThat(testCase.doesCover()).isTrue();
    assertThat(testCase.countCoveredLines()).isEqualTo(6);
    assertThat(testCase.covers()).hasSize(2);
  }

  @Test
  public void should_set_metadata() {
    BeanGraph beanGraph = BeanGraph.createInMemory();
    DefaultTestCase testCase = beanGraph.createVertex(DefaultTestCase.class);

    testCase.setKey("T1")
      .setName("Test one")
      .setDurationInMs(1234L)
      .setMessage("Error msg")
      .setStackTrace("xxx")
      .setStatus(TestCase.STATUS_FAIL);

    assertThat(testCase.key()).isEqualTo("T1");
    assertThat(testCase.name()).isEqualTo("Test one");
    assertThat(testCase.message()).isEqualTo("Error msg");
    assertThat(testCase.stackTrace()).isEqualTo("xxx");
    assertThat(testCase.durationInMs()).isEqualTo(1234L);
    assertThat(testCase.status()).isEqualTo(TestCase.STATUS_FAIL);
  }

  @Test
  public void duration_should_be_positive() {
    thrown.expect(IllegalDurationException.class);
    thrown.expectMessage("Test duration must be positive (got: -1234)");

    BeanGraph beanGraph = BeanGraph.createInMemory();
    DefaultTestCase testCase = beanGraph.createVertex(DefaultTestCase.class);

    testCase.setDurationInMs(-1234L);
  }
}
