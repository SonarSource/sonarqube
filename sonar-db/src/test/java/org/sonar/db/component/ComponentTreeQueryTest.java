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
package org.sonar.db.component;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

public class ComponentTreeQueryTest {

  private static final String AN_UUID = "u1";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void convert_sorts_in_sql_representation() {
    ComponentTreeQuery result = ComponentTreeQuery.builder()
      .setBaseUuid(AN_UUID)
      .setSortFields(newArrayList("name", "path", "qualifier"))
      .build();

    assertThat(result.getSqlSort()).isEqualTo("LOWER(p.name) ASC, p.name ASC, LOWER(p.path) ASC, p.path ASC, LOWER(p.qualifier) ASC, p.qualifier ASC");
  }

  @Test
  public void fail_if_no_base_uuid() {
    expectedException.expect(NullPointerException.class);

    ComponentTreeQuery.builder()
      .setSortFields(singletonList("name"))
      .build();
  }

  @Test
  public void fail_if_no_sort() {
    expectedException.expect(NullPointerException.class);

    ComponentTreeQuery.builder()
      .setBaseUuid(AN_UUID)
      .build();
  }
}
