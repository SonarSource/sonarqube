/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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

import java.util.List;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SortingTest {

  @Test
  public void test_definition() {
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
  public void getFields_returns_registered_fields_for_name() {
    Sorting sorting = new Sorting();
    sorting.add("updatedAt");

    List<Sorting.Field> fields = sorting.getFields("updatedAt");
    assertThat(fields).hasSize(1);
    assertThat(fields.get(0).getName()).isEqualTo("updatedAt");
    assertThat(fields.get(0).isReverse()).isFalse();
    assertThat(fields.get(0).isMissingLast()).isFalse();
  }

  @Test
  public void getFields_returns_empty_list_for_unknown_field() {
    Sorting sorting = new Sorting();
    sorting.add("file");

    assertThat(sorting.getFields("unknown")).isEmpty();
  }

  @Test
  public void getFields_returns_fields_with_reverse_and_missingLast_flags() {
    Sorting sorting = new Sorting();
    sorting.add("fileLine", "file");
    sorting.add("fileLine", "line");
    sorting.add("fileLine", "severity").reverse();
    sorting.add("fileLine", "key").missingLast();

    List<Sorting.Field> fields = sorting.getFields("fileLine");
    assertThat(fields).hasSize(4);
    assertThat(fields.get(0).getName()).isEqualTo("file");
    assertThat(fields.get(0).isReverse()).isFalse();
    assertThat(fields.get(1).getName()).isEqualTo("line");
    assertThat(fields.get(2).getName()).isEqualTo("severity");
    assertThat(fields.get(2).isReverse()).isTrue();
    assertThat(fields.get(3).getName()).isEqualTo("key");
    assertThat(fields.get(3).isMissingLast()).isTrue();
  }

  @Test
  public void getDefaultFields_returns_registered_default_fields() {
    Sorting sorting = new Sorting();
    sorting.addDefault("file");
    sorting.addDefault("key");

    List<Sorting.Field> fields = sorting.getDefaultFields();
    assertThat(fields).hasSize(2);
    assertThat(fields.get(0).getName()).isEqualTo("file");
    assertThat(fields.get(1).getName()).isEqualTo("key");
  }
}