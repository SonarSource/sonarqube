/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.api.utils;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class WorkUnitTest {

  @Test
  public void create() {
    WorkUnit workUnit = WorkUnit.create(2.0, "mn");
    assertThat(workUnit.getUnit()).isEqualTo("mn");
    assertThat(workUnit.getValue()).isEqualTo(2.0);
  }

  @Test
  public void create_default() {
    WorkUnit workUnit = WorkUnit.create();
    assertThat(workUnit.getUnit()).isEqualTo("d");
    assertThat(workUnit.getValue()).isEqualTo(0.0);
  }

  @Test
  public void test_equals() throws Exception {
    assertThat(WorkUnit.create(2.0, "mn")).isEqualTo(WorkUnit.create(2.0, "mn"));
    assertThat(WorkUnit.create(3.0, "mn")).isNotEqualTo(WorkUnit.create(2.0, "mn"));
    assertThat(WorkUnit.create(2.0, "h")).isNotEqualTo(WorkUnit.create(2.0, "mn"));
  }

  @Test
  public void fail_with_bad_unit() {
    try {
      WorkUnit.create(2.0, "z");
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Test
  public void fail_with_bad_value() {
    try {
      WorkUnit.create(-2.0, "mn");
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class);
    }
  }

}
