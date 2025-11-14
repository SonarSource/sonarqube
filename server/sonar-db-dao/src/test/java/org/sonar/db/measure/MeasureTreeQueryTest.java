/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.db.measure;

import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.sonar.db.component.ComponentTesting;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.db.measure.MeasureTreeQuery.Strategy.CHILDREN;
import static org.sonar.db.measure.MeasureTreeQuery.Strategy.LEAVES;

class MeasureTreeQueryTest {

  @Test
  void create_query() {
    MeasureTreeQuery query = MeasureTreeQuery.builder()
      .setStrategy(CHILDREN)
      .setQualifiers(asList("FIL", "DIR"))
      .setNameOrKeyQuery("teSt")
      .build();

    assertThat(query.getStrategy()).isEqualTo(CHILDREN);
    assertThat(query.getQualifiers()).containsOnly("FIL", "DIR");
    assertThat(query.getNameOrKeyQuery()).isEqualTo("teSt");
  }

  @Test
  void create_minimal_query() {
    MeasureTreeQuery query = MeasureTreeQuery.builder()
      .setStrategy(CHILDREN)
      .build();

    assertThat(query.getStrategy()).isEqualTo(CHILDREN);
    assertThat(query.getQualifiers()).isNull();
    assertThat(query.getNameOrKeyQuery()).isNull();
  }

  @Test
  void test_getNameOrKeyUpperLikeQuery() {
    assertThat(MeasureTreeQuery.builder()
      .setNameOrKeyQuery("like-\\_%/-value")
      .setStrategy(CHILDREN)
      .build().getNameOrKeyUpperLikeQuery()).isEqualTo("%LIKE-\\/_/%//-VALUE%");

    assertThat(MeasureTreeQuery.builder()
      .setStrategy(CHILDREN)
      .build().getNameOrKeyUpperLikeQuery()).isNull();
  }

  @Test
  void test_getUuidPath() {
    assertThat(MeasureTreeQuery.builder().setStrategy(CHILDREN)
      .build().getUuidPath(ComponentTesting.newPrivateProjectDto("PROJECT_UUID"))).isEqualTo(".PROJECT_UUID.");

    assertThat(MeasureTreeQuery.builder().setStrategy(LEAVES)
      .build().getUuidPath(ComponentTesting.newPrivateProjectDto("PROJECT_UUID"))).isEqualTo(".PROJECT/_UUID.%");
  }

  @Test
  void return_empty_when_qualifiers_is_empty() {
    assertThat(MeasureTreeQuery.builder()
      .setStrategy(CHILDREN)
      .setQualifiers(Collections.emptyList())
      .build().returnsEmpty()).isTrue();

    assertThat(MeasureTreeQuery.builder()
      .setStrategy(CHILDREN)
      .setQualifiers(asList("FIL", "DIR"))
      .build().returnsEmpty()).isFalse();
  }

  @Test
  void fail_when_no_strategy() {
    assertThatThrownBy(() -> {
      MeasureTreeQuery.builder()
        .build();
    })
      .isInstanceOf(NullPointerException.class);
  }

}
