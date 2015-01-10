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

package org.sonar.api.utils;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class PagingTest {

  @Test
  public void test_pagination() {
    Paging paging = Paging.create(5, 1, 20);

    assertThat(paging.pageSize()).isEqualTo(5);
    assertThat(paging.pageIndex()).isEqualTo(1);
    assertThat(paging.total()).isEqualTo(20);

    assertThat(paging.offset()).isEqualTo(0);
    assertThat(paging.pages()).isEqualTo(4);
  }

  @Test
  public void test_offset() {
    assertThat(Paging.create(5, 1, 20).offset()).isEqualTo(0);
    assertThat(Paging.create(5, 2, 20).offset()).isEqualTo(5);
  }

  @Test
  public void test_number_of_pages() {
    assertThat(Paging.create(5, 2, 20).pages()).isEqualTo(4);
    assertThat(Paging.create(5, 2, 21).pages()).isEqualTo(5);
    assertThat(Paging.create(5, 2, 25).pages()).isEqualTo(5);
    assertThat(Paging.create(5, 2, 26).pages()).isEqualTo(6);
  }

  @Test
  public void page_size_should_be_strictly_positive() throws Exception {
    try {
      Paging.create(0, 5, 5);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Page size must be strictly positive. Got 0");
    }
  }

  @Test
  public void page_index_should_be_strictly_positive() throws Exception {
    try {
      Paging.create(5, 0, 5);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Page index must be strictly positive. Got 0");
    }
  }

  @Test
  public void total_items_should_be_positive() throws Exception {
    try {
      Paging.create(5, 5, -1);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Total items must be positive. Got -1");
    }
  }
}
