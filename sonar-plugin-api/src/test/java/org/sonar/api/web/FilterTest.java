/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.api.web;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.fest.assertions.Assertions.assertThat;

public class FilterTest {
  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Test
  public void should_create_filter() {
    Filter filter = Filter.create()
        .setDisplayAs("list")
        .setFavouritesOnly(true)
        .setPageSize(100);

    assertThat(filter.getDisplayAs()).isEqualTo("list");
    assertThat(filter.isFavouritesOnly()).isTrue();
    assertThat(filter.getPageSize()).isEqualTo(100);
  }

  @Test
  public void should_add_criteria() {
    Criterion criterion1 = Criterion.createForQualifier("A");
    Criterion criterion2 = Criterion.createForQualifier("A");
    Filter filter = Filter.create()
        .add(criterion1)
        .add(criterion2);

    assertThat(filter.getCriteria()).containsExactly(criterion1, criterion2);
  }

  @Test
  public void should_add_columns() {
    FilterColumn column1 = FilterColumn.create("", "", "ASC", false);
    FilterColumn column2 = FilterColumn.create("", "", "DESC", false);
    Filter filter = Filter.create()
        .add(column1)
        .add(column2);

    assertThat(filter.getColumns()).containsExactly(column1, column2);
  }

  @Test
  public void should_accept_valid_periods() {
    Filter.create().setDisplayAs("list");
    Filter.create().setDisplayAs("treemap");
  }

  @Test
  public void should_fail_on_invalid_display() {
    exception.expect(IllegalArgumentException.class);
    exception.expectMessage("Default display should be either list or treemap, not <invalid>");

    Filter.create().setDisplayAs("<invalid>");
  }

  @Test
  public void should_accept_valid_pageSize() {
    Filter.create().setPageSize(20);
    Filter.create().setPageSize(200);
  }

  @Test
  public void should_fail_on_pageSize_too_small() {
    exception.expect(IllegalArgumentException.class);
    exception.expectMessage("page size should be between 20 and 200");

    Filter.create().setPageSize(19);
  }

  @Test
  public void should_fail_on_pageSize_too_high() {
    exception.expect(IllegalArgumentException.class);
    exception.expectMessage("page size should be between 20 and 200");

    Filter.create().setPageSize(201);
  }
}
