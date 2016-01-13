/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import org.apache.commons.lang.reflect.FieldUtils;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.Test;
import org.sonar.server.exceptions.BadRequestException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

public class SortingTest {

  @Test
  public void test_definition() throws Exception {
    Sorting sorting = new Sorting();
    sorting.add("fileLine", "file");
    sorting.add("fileLine", "line").missingLast().reverse();

    List<Sorting.Field> fields = sorting.getFields("fileLine");
    assertThat(fields).hasSize(2);
    assertThat(fields.get(0).getName()).isEqualTo("file");
    assertThat(fields.get(0).isReverse()).isFalse();
    assertThat(fields.get(0).isMissingLast()).isFalse();

    assertThat(fields.get(1).getName()).isEqualTo("line");
    assertThat(fields.get(1).isReverse()).isTrue();
    assertThat(fields.get(1).isMissingLast()).isTrue();
  }

  @Test
  public void ascending_sort_on_single_field() throws Exception {
    Sorting sorting = new Sorting();
    sorting.add("updatedAt");

    SearchRequestBuilder request = new SearchRequestBuilder(mock(Client.class));
    sorting.fill(request, "updatedAt", true);
    List<SortBuilder> fields = fields(request);
    assertThat(fields).hasSize(1);
    expectField(fields.get(0), "updatedAt", "_first", SortOrder.ASC);
  }

  @Test
  public void descending_sort_on_single_field() throws Exception {
    Sorting sorting = new Sorting();
    sorting.add("updatedAt");

    SearchRequestBuilder request = new SearchRequestBuilder(mock(Client.class));
    sorting.fill(request, "updatedAt", false);
    List<SortBuilder> fields = fields(request);
    assertThat(fields).hasSize(1);
    expectField(fields.get(0), "updatedAt", "_last", SortOrder.DESC);
  }

  @Test
  public void ascending_sort_on_single_field_with_missing_in_last_position() throws Exception {
    Sorting sorting = new Sorting();
    sorting.add("updatedAt").missingLast();

    SearchRequestBuilder request = new SearchRequestBuilder(mock(Client.class));
    sorting.fill(request, "updatedAt", true);
    List<SortBuilder> fields = fields(request);
    assertThat(fields).hasSize(1);
    expectField(fields.get(0), "updatedAt", "_last", SortOrder.ASC);
  }

  @Test
  public void descending_sort_on_single_field_with_missing_in_last_position() throws Exception {
    Sorting sorting = new Sorting();
    sorting.add("updatedAt").missingLast();

    SearchRequestBuilder request = new SearchRequestBuilder(mock(Client.class));
    sorting.fill(request, "updatedAt", false);
    List<SortBuilder> fields = fields(request);
    assertThat(fields).hasSize(1);
    expectField(fields.get(0), "updatedAt", "_first", SortOrder.DESC);
  }

  @Test
  public void sort_on_multiple_fields() throws Exception {
    // asc => file asc, line asc, severity desc, key asc
    Sorting sorting = new Sorting();
    sorting.add("fileLine", "file");
    sorting.add("fileLine", "line");
    sorting.add("fileLine", "severity").reverse();
    sorting.add("fileLine", "key").missingLast();

    SearchRequestBuilder request = new SearchRequestBuilder(mock(Client.class));
    sorting.fill(request, "fileLine", true);
    List<SortBuilder> fields = fields(request);
    assertThat(fields).hasSize(4);
    expectField(fields.get(0), "file", "_first", SortOrder.ASC);
    expectField(fields.get(1), "line", "_first", SortOrder.ASC);
    expectField(fields.get(2), "severity", "_first", SortOrder.DESC);
    expectField(fields.get(3), "key", "_last", SortOrder.ASC);
  }

  @Test
  public void fail_if_unknown_field() {
    Sorting sorting = new Sorting();
    sorting.add("file");

    try {
      sorting.fill(new SearchRequestBuilder(mock(Client.class)), "unknown", true);
      fail();
    } catch (BadRequestException e) {
      assertThat(e.getMessage()).isEqualTo("Bad sort field: unknown");
    }
  }

  @Test
  public void default_sorting() throws Exception {
    Sorting sorting = new Sorting();
    sorting.addDefault("file");

    SearchRequestBuilder request = new SearchRequestBuilder(mock(Client.class));
    sorting.fillDefault(request);
    List<SortBuilder> fields = fields(request);
    assertThat(fields).hasSize(1);
  }

  private void expectField(SortBuilder field, String expectedField, String expectedMissing, SortOrder expectedSort) throws IllegalAccessException {
    assertThat(fieldName(field)).isEqualTo(expectedField);
    assertThat(missing(field)).isEqualTo(expectedMissing);
    assertThat(order(field)).isEqualTo(expectedSort);
  }

  private static List<SortBuilder> fields(SearchRequestBuilder request) throws IllegalAccessException {
    SearchSourceBuilder source = request.internalBuilder();
    return (List<SortBuilder>) FieldUtils.readField(source, "sorts", true);
  }

  private static String fieldName(SortBuilder sortBuilder) throws IllegalAccessException {
    return (String) FieldUtils.readField(sortBuilder, "fieldName", true);
  }

  private static String missing(SortBuilder sortBuilder) throws IllegalAccessException {
    return (String) FieldUtils.readField(sortBuilder, "missing", true);
  }

  private static SortOrder order(SortBuilder sortBuilder) throws IllegalAccessException {
    return (SortOrder) FieldUtils.readField(sortBuilder, "order", true);
  }
}
