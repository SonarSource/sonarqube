/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import org.assertj.core.api.AbstractIntegerAssert;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BulkIndexerConcurrentRequestCalculationTest {

  @Test
  public void should_not_parallelize_if_regular_size() {
    assertConcurrentRequests(BulkIndexer.Size.REGULAR, cores(4))
      .isEqualTo(0);
  }

  @Test
  public void should_not_parallelize_if_large_indexing_but_few_cores() {
    assertConcurrentRequests(BulkIndexer.Size.LARGE, cores(4))
      .isEqualTo(0);
  }

  /**
   * see https://jira.sonarsource.com/browse/SONAR-8075
   */
  @Test
  public void should_heavily_parallelize_on_96_cores_if_large_indexing() {
    assertConcurrentRequests(BulkIndexer.Size.LARGE, cores(96))
      .isEqualTo(18);
  }

  private AbstractIntegerAssert<?> assertConcurrentRequests(BulkIndexer.Size size, BulkIndexer.Runtime2 runtime2) {
    return assertThat(size.createHandler(runtime2).getConcurrentRequests());
  }

  private static BulkIndexer.Runtime2 cores(int cores) {
    BulkIndexer.Runtime2 runtime = mock(BulkIndexer.Runtime2.class);
    when(runtime.getCores()).thenReturn(cores);
    return runtime;
  }
}
