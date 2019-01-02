/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.es;

import org.assertj.core.data.Offset;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class IndexingResultTest {

  private static final Offset<Double> DOUBLE_OFFSET = Offset.offset(0.000001d);

  private final IndexingResult underTest = new IndexingResult();

  @Test
  public void test_empty() {
    assertThat(underTest.getFailures()).isEqualTo(0);
    assertThat(underTest.getSuccess()).isEqualTo(0);
    assertThat(underTest.getTotal()).isEqualTo(0);
    assertThat(underTest.getSuccessRatio()).isEqualTo(1.0, DOUBLE_OFFSET);
    assertThat(underTest.isSuccess()).isTrue();
  }

  @Test
  public void test_success() {
    underTest.incrementRequests();
    underTest.incrementRequests();
    underTest.incrementSuccess();
    underTest.incrementSuccess();

    assertThat(underTest.getFailures()).isEqualTo(0);
    assertThat(underTest.getSuccess()).isEqualTo(2);
    assertThat(underTest.getTotal()).isEqualTo(2);
    assertThat(underTest.getSuccessRatio()).isEqualTo(1.0, DOUBLE_OFFSET);
    assertThat(underTest.isSuccess()).isTrue();
  }

  @Test
  public void test_failure() {
    underTest.incrementRequests();
    underTest.incrementRequests();

    assertThat(underTest.getFailures()).isEqualTo(2);
    assertThat(underTest.getSuccess()).isEqualTo(0);
    assertThat(underTest.getTotal()).isEqualTo(2);
    assertThat(underTest.getSuccessRatio()).isEqualTo(0.0, DOUBLE_OFFSET);
    assertThat(underTest.isSuccess()).isFalse();
  }

  @Test
  public void test_partial_failure() {
    underTest.incrementRequests();
    underTest.incrementRequests();
    underTest.incrementRequests();
    underTest.incrementRequests();
    underTest.incrementSuccess();

    assertThat(underTest.getFailures()).isEqualTo(3);
    assertThat(underTest.getSuccess()).isEqualTo(1);
    assertThat(underTest.getTotal()).isEqualTo(4);
    assertThat(underTest.getSuccessRatio()).isEqualTo(0.25, DOUBLE_OFFSET);
    assertThat(underTest.isSuccess()).isFalse();
  }

  @Test
  public void correctness_even_with_no_data() {
    assertThat(underTest.getFailures()).isEqualTo(0);
    assertThat(underTest.getSuccess()).isEqualTo(0);
    assertThat(underTest.getTotal()).isEqualTo(0);
    assertThat(underTest.getSuccessRatio()).isEqualTo(1.0);
    assertThat(underTest.isSuccess()).isTrue();
  }
}
