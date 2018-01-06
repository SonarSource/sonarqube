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
package org.sonar.api.utils;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.utils.Paging.forPageIndex;

public class PagingTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void test_pagination() {
    Paging paging = forPageIndex(1).withPageSize(5).andTotal(20);

    assertThat(paging.pageSize()).isEqualTo(5);
    assertThat(paging.pageIndex()).isEqualTo(1);
    assertThat(paging.total()).isEqualTo(20);

    assertThat(paging.offset()).isEqualTo(0);
    assertThat(paging.pages()).isEqualTo(4);
  }

  @Test
  public void test_offset() {
    assertThat(forPageIndex(1).withPageSize(5).andTotal(20).offset()).isEqualTo(0);
    assertThat(forPageIndex(2).withPageSize(5).andTotal(20).offset()).isEqualTo(5);
  }

  @Test
  public void test_number_of_pages() {
    assertThat(forPageIndex(2).withPageSize(5).andTotal(20).pages()).isEqualTo(4);
    assertThat(forPageIndex(2).withPageSize(5).andTotal(21).pages()).isEqualTo(5);
    assertThat(forPageIndex(2).withPageSize(5).andTotal(25).pages()).isEqualTo(5);
    assertThat(forPageIndex(2).withPageSize(5).andTotal(26).pages()).isEqualTo(6);
  }

  @Test
  public void page_size_should_be_strictly_positive() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Page size must be strictly positive. Got 0");

    forPageIndex(5).withPageSize(0).andTotal(5);
  }

  @Test
  public void page_index_should_be_strictly_positive() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Page index must be strictly positive. Got 0");

    forPageIndex(0).withPageSize(5).andTotal(5);
  }

  @Test
  public void total_items_should_be_positive() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Total items must be positive. Got -1");

    forPageIndex(5).withPageSize(5).andTotal(-1);
  }
}
