package org.sonar.server.es;/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

import java.util.stream.IntStream;
import org.apache.commons.lang.math.RandomUtils;
import org.assertj.core.data.Offset;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ResilientIndexerResultTest {

  private final ResilientIndexerResult underTest = new ResilientIndexerResult();

  @Before
  public void clear() {
    underTest.clear();
  }

  @Test
  public void ensure_correctness() {
    int success = 1 + RandomUtils.nextInt(100);
    int failures = RandomUtils.nextInt(100);
    IntStream.rangeClosed(1, success).forEach(i -> underTest.increaseSuccess());
    IntStream.rangeClosed(1, failures).forEach(i -> underTest.increaseFailure());

    assertThat(underTest.getFailures()).isEqualTo(failures);
    assertThat(underTest.getSuccess()).isEqualTo(success);
    assertThat(underTest.getTotal()).isEqualTo(success + failures);
    assertThat(underTest.getFailureRatio() + underTest.getSuccessRatio()).isEqualTo(1);
    assertThat(underTest.getFailureRatio()).isEqualTo(1.0d * failures / (success + failures), Offset.offset(0.000001d));
    assertThat(underTest.getSuccessRatio()).isEqualTo(1.0d * success / (success + failures), Offset.offset(0.000001d));
  }

  @Test
  public void correctness_even_with_no_data() {
    assertThat(underTest.getFailures()).isEqualTo(0);
    assertThat(underTest.getSuccess()).isEqualTo(0);
    assertThat(underTest.getTotal()).isEqualTo(0);
    assertThat(underTest.getFailureRatio() + underTest.getSuccessRatio()).isEqualTo(1);
    assertThat(underTest.getFailureRatio()).isEqualTo(1);
    assertThat(underTest.getSuccessRatio()).isEqualTo(0);
  }
}
