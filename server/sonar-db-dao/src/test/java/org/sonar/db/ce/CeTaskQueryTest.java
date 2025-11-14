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
package org.sonar.db.ce;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

class CeTaskQueryTest {

  CeTaskQuery underTest = new CeTaskQuery();

  @Test
  void no_filter_on_entity_uuids_by_default() {
    assertThat(underTest.getEntityUuids()).isNull();
    assertThat(underTest.isShortCircuitedByEntityUuids()).isFalse();
  }

  @Test
  void filter_on_entity_uuid() {
    underTest.setEntityUuid("UUID1");
    assertThat(underTest.getEntityUuids()).containsOnly("UUID1");
    assertThat(underTest.isShortCircuitedByEntityUuids()).isFalse();
  }

  @Test
  void filter_on_multiple_entity_uuids() {
    underTest.setEntityUuids(asList("UUID1", "UUID2"));
    assertThat(underTest.getEntityUuids()).containsOnly("UUID1", "UUID2");
    assertThat(underTest.isShortCircuitedByEntityUuids()).isFalse();
  }

  /**
   * entityUuid is not null but is set to empty
   * --> no results
   */
  @Test
  void short_circuited_if_empty_entity_uuid_filter() {
    underTest.setEntityUuids(Collections.emptyList());
    assertThat(underTest.getEntityUuids()).isEmpty();
    assertThat(underTest.isShortCircuitedByEntityUuids()).isTrue();
  }

  /**
   * too many entityUuids for SQL request. Waiting for ES to improve this use-case
   * --> no results
   */
  @Test
  void short_circuited_if_too_many_entity_uuid_filters() {
    List<String> uuids = new ArrayList<>();
    for (int i = 0; i < CeTaskQuery.MAX_COMPONENT_UUIDS + 2; i++) {
      uuids.add(String.valueOf(i));
    }
    underTest.setEntityUuids(uuids);
    assertThat(underTest.getEntityUuids()).hasSize(CeTaskQuery.MAX_COMPONENT_UUIDS + 2);
    assertThat(underTest.isShortCircuitedByEntityUuids()).isTrue();
  }

  @Test
  void test_errorTypes() {
    assertThat(underTest.getErrorTypes()).isNull();

    underTest.setErrorTypes(asList("foo", "bar"));
    assertThat(underTest.getErrorTypes()).containsExactlyInAnyOrder("foo", "bar");
  }

  @Test
  void test_minExecutedAt() {
    assertThat(underTest.getMinExecutedAt()).isNull();

    underTest.setMinExecutedAt(1_000L);
    assertThat(underTest.getMinExecutedAt()).isEqualTo(1_000L);
  }
}
