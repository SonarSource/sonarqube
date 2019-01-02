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

import java.io.StringWriter;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.test.JsonAssert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class SearchOptionsTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private SearchOptions underTest = new SearchOptions();

  @Test
  public void defaults() {
    SearchOptions options = new SearchOptions();

    assertThat(options.getFacets()).isEmpty();
    assertThat(options.getFields()).isEmpty();
    assertThat(options.getOffset()).isEqualTo(0);
    assertThat(options.getLimit()).isEqualTo(10);
    assertThat(options.getPage()).isEqualTo(1);
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
    assertThat(options.getOffset()).isEqualTo(0);
    assertThat(options.getPage()).isEqualTo(1);
  }

  @Test
  public void with_zero_page_size() {
    SearchOptions options = new SearchOptions().setPage(1, 0);
    assertThat(options.getLimit()).isEqualTo(SearchOptions.MAX_LIMIT);
    assertThat(options.getOffset()).isEqualTo(0);
    assertThat(options.getPage()).isEqualTo(1);
  }

  @Test
  public void page_must_be_strictly_positive() {
    try {
      new SearchOptions().setPage(0, 10);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Page must be greater or equal to 1 (got 0)");
    }
  }

  @Test
  public void use_max_limit_if_negative() {
    SearchOptions options = new SearchOptions().setPage(2, -1);
    assertThat(options.getLimit()).isEqualTo(SearchOptions.MAX_LIMIT);
  }

  @Test
  public void max_limit() {
    SearchOptions options = new SearchOptions().setLimit(42);
    assertThat(options.getLimit()).isEqualTo(42);

    options.setLimit(SearchOptions.MAX_LIMIT + 10);
    assertThat(options.getLimit()).isEqualTo(SearchOptions.MAX_LIMIT);
  }

  @Test
  public void max_page_size() {
    SearchOptions options = new SearchOptions().setPage(3, SearchOptions.MAX_LIMIT + 10);
    assertThat(options.getOffset()).isEqualTo(SearchOptions.MAX_LIMIT * 2);
    assertThat(options.getLimit()).isEqualTo(SearchOptions.MAX_LIMIT);
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

  @Test
  public void fail_if_result_after_first_10_000() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Can return only the first 10000 results. 10500th result asked.");

    underTest.setPage(21, 500);
  }
}
