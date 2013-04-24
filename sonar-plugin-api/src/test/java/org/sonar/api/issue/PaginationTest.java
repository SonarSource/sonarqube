/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

package org.sonar.api.issue;

import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

public class PaginationTest {

  @Test
  public void test_pagination(){
    Pagination pagination = new Pagination(5, 1, 20);

    assertThat(pagination.limit()).isEqualTo(5);
    assertThat(pagination.page()).isEqualTo(1);
    assertThat(pagination.size()).isEqualTo(20);
    assertThat(pagination.empty()).isFalse();

    assertThat(pagination.offset()).isEqualTo(0);
    assertThat(pagination.pages()).isEqualTo(4);
  }

  @Test
  public void test_pagination_on_second_page(){
    Pagination pagination = new Pagination(5, 2, 20);

    assertThat(pagination.offset()).isEqualTo(5);
    assertThat(pagination.pages()).isEqualTo(4);
  }

}
