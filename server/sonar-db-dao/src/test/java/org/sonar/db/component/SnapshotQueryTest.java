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
package org.sonar.db.component;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.component.SnapshotQuery.SORT_FIELD.BY_DATE;
import static org.sonar.db.component.SnapshotQuery.SORT_ORDER.ASC;

public class SnapshotQueryTest {

  @Test
  public void test_setters_and_getters() {
    SnapshotQuery query = new SnapshotQuery()
      .setComponentUuid("abcd")
      .setIsLast(true)
      .setStatus("P")
      .setProjectVersion("1.0")
      .setCreatedAfter(10L)
      .setCreatedBefore(20L)
      .setSort(BY_DATE, ASC);

    assertThat(query.getComponentUuid()).isEqualTo("abcd");
    assertThat(query.getIsLast()).isTrue();
    assertThat(query.getStatus()).isEqualTo("P");
    assertThat(query.getProjectVersion()).isEqualTo("1.0");
    assertThat(query.getCreatedAfter()).isEqualTo(10L);
    assertThat(query.getCreatedBefore()).isEqualTo(20L);
    assertThat(query.getSortField()).isEqualTo("created_at");
    assertThat(query.getSortOrder()).isEqualTo("asc");
  }
}
