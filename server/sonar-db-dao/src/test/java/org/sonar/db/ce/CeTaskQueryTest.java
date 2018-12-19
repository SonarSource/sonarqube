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
package org.sonar.db.ce;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

public class CeTaskQueryTest {

  CeTaskQuery underTest = new CeTaskQuery();

  @Test
  public void no_filter_on_component_uuids_by_default() {
    assertThat(underTest.getMainComponentUuids()).isNull();
    assertThat(underTest.isShortCircuitedByMainComponentUuids()).isFalse();
  }

  @Test
  public void filter_on_component_uuid() {
    underTest.setMainComponentUuid("UUID1");
    assertThat(underTest.getMainComponentUuids()).containsOnly("UUID1");
    assertThat(underTest.isShortCircuitedByMainComponentUuids()).isFalse();
  }

  @Test
  public void filter_on_multiple_component_uuids() {
    underTest.setMainComponentUuids(asList("UUID1", "UUID2"));
    assertThat(underTest.getMainComponentUuids()).containsOnly("UUID1", "UUID2");
    assertThat(underTest.isShortCircuitedByMainComponentUuids()).isFalse();
  }

  /**
   * componentUuid is not null but is set to empty
   * --> no results
   */
  @Test
  public void short_circuited_if_empty_component_uuid_filter() {
    underTest.setMainComponentUuids(Collections.emptyList());
    assertThat(underTest.getMainComponentUuids()).isEmpty();
    assertThat(underTest.isShortCircuitedByMainComponentUuids()).isTrue();
  }

  /**
   * too many componentUuids for SQL request. Waiting for ES to improve this use-case
   * --> no results
   */
  @Test
  public void short_circuited_if_too_many_component_uuid_filters() {
    List<String> uuids = new ArrayList<>();
    for (int i = 0; i < CeTaskQuery.MAX_COMPONENT_UUIDS + 2; i++) {
      uuids.add(String.valueOf(i));
    }
    underTest.setMainComponentUuids(uuids);
    assertThat(underTest.getMainComponentUuids()).hasSize(CeTaskQuery.MAX_COMPONENT_UUIDS + 2);
    assertThat(underTest.isShortCircuitedByMainComponentUuids()).isTrue();
  }

  @Test
  public void test_errorTypes() {
    assertThat(underTest.getErrorTypes()).isNull();

    underTest.setErrorTypes(asList("foo", "bar"));
    assertThat(underTest.getErrorTypes()).containsExactlyInAnyOrder("foo", "bar");
  }

  @Test
  public void test_minExecutedAt() {
    assertThat(underTest.getMinExecutedAt()).isNull();

    underTest.setMinExecutedAt(1_000L);
    assertThat(underTest.getMinExecutedAt()).isEqualTo(1_000L);
  }
}
