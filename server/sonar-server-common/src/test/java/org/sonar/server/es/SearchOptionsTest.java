/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import java.io.StringWriter;
import org.junit.Test;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.test.JsonAssert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SearchOptionsTest {


  private SearchOptions underTest = new SearchOptions();

  @Test
  public void defaults() {
    SearchOptions options = new SearchOptions();

    assertThat(options.getFacets()).isEmpty();
    assertThat(options.getFields()).isEmpty();
    assertThat(options.getOffset()).isZero();
    assertThat(options.getLimit()).isEqualTo(10);
    assertThat(options.getPage()).isOne();
  }

  @Test
  public void page_shortcut_for_limit_and_offset() {
    SearchOptions options = new SearchOptions().setPage(3, 10);

    assertThat(options.getLimit()).isEqualTo(10);
    assertThat(options.getOffset()).isEqualTo(20);
  }

  @Test
  public void page_starts_at_one() {
    SearchOptions options = new SearchOptions().setPage(1, 10);
    assertThat(options.getLimit()).isEqualTo(10);
    assertThat(options.getOffset()).isZero();
    assertThat(options.getPage()).isOne();
  }

  @Test
  public void fail_if_page_is_not_strictly_positive() {
    assertThatThrownBy(() -> new SearchOptions().setPage(0, 10))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Page must be greater or equal to 1 (got 0)");
  }

  @Test
  public void fail_if_ps_is_zero() {
    assertThatThrownBy(() -> new SearchOptions().setPage(1, 0))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Page size must be between 1 and 500 (got 0)");
  }

  @Test
  public void fail_if_ps_is_negative() {
    assertThatThrownBy(() -> new SearchOptions().setPage(2, -1))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Page size must be between 1 and 500 (got -1)");
  }

  @Test
  public void fail_if_ps_is_over_limit() {
    assertThatThrownBy(() -> new SearchOptions().setPage(3, SearchOptions.MAX_PAGE_SIZE + 10))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Page size must be between 1 and 500 (got 510)");
  }

  @Test
  public void fail_if_result_after_first_10_000() {
    assertThatThrownBy(() -> underTest.setPage(21, 500))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Can return only the first 10000 results. 10500th result asked.");
  }

  @Test
  public void max_limit() {
    SearchOptions options = new SearchOptions().setLimit(42);
    assertThat(options.getLimit()).isEqualTo(42);

    assertThatThrownBy(() -> options.setLimit(SearchOptions.MAX_PAGE_SIZE + 10))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Page size must be between 1 and 500 (got 510)");
  }

  @Test
  public void writeJson() {
    SearchOptions options = new SearchOptions().setPage(3, 10);
    StringWriter json = new StringWriter();
    JsonWriter jsonWriter = JsonWriter.of(json).beginObject();
    options.writeJson(jsonWriter, 42L);
    jsonWriter.endObject().close();

    JsonAssert.assertJson(json.toString()).isSimilarTo("{\"total\": 42, \"p\": 3, \"ps\": 10}");
  }
}
