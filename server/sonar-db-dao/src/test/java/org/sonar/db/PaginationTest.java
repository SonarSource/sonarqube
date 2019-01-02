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
package org.sonar.db;

import java.util.Random;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.Pagination.forPage;


public class PaginationTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void all_is_page_1_with_MAX_INTEGER_page_size() {
    Pagination pagination = Pagination.all();

    assertThat(pagination.getPage()).isEqualTo(1);
    assertThat(pagination.getPageSize()).isEqualTo(Integer.MAX_VALUE);
  }

  @Test
  public void all_returns_a_constant() {
    assertThat(Pagination.all()).isSameAs(Pagination.all());
  }

  @Test
  public void forPage_fails_with_IAE_if_page_is_0() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("page index must be >= 1");

    forPage(0);
  }

  @Test
  public void forPage_fails_with_IAE_if_page_is_less_than_0() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("page index must be >= 1");

    forPage(-Math.abs(new Random().nextInt()) - 1);
  }

  @Test
  public void andSize_fails_with_IAE_if_size_is_0() {
    Pagination.Builder builder = forPage(1);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("page size must be >= 1");

    builder.andSize(0);
  }

  @Test
  public void andSize_fails_with_IAE_if_size_is_less_than_0() {
    Pagination.Builder builder = forPage(1);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("page size must be >= 1");

    builder.andSize(-Math.abs(new Random().nextInt()) - 1);
  }

  @Test
  public void offset_is_computed_from_page_and_size() {
    assertThat(forPage(2).andSize(3).getOffset()).isEqualTo(3);
    assertThat(forPage(5).andSize(3).getOffset()).isEqualTo(12);
    assertThat(forPage(5).andSize(1).getOffset()).isEqualTo(4);
  }

  @Test
  public void startRowNumber_is_computed_from_page_and_size() {
    assertThat(forPage(2).andSize(3).getStartRowNumber()).isEqualTo(4);
    assertThat(forPage(5).andSize(3).getStartRowNumber()).isEqualTo(13);
    assertThat(forPage(5).andSize(1).getStartRowNumber()).isEqualTo(5);
  }

  @Test
  public void endRowNumber_is_computed_from_page_and_size() {
    assertThat(forPage(2).andSize(3).getEndRowNumber()).isEqualTo(6);
    assertThat(forPage(5).andSize(3).getEndRowNumber()).isEqualTo(15);
    assertThat(forPage(5).andSize(1).getEndRowNumber()).isEqualTo(5);
  }
}
