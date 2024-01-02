/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CeTaskDtoLightTest {
  private final CeTaskDtoLight task1 = new CeTaskDtoLight();
  private final CeTaskDtoLight task2 = new CeTaskDtoLight();
  private final CeTaskDtoLight task3 = new CeTaskDtoLight();
  private final CeTaskDtoLight task4 = new CeTaskDtoLight();

  @Before
  public void setUp() {
    task1.setCeTaskUuid("id1");
    task1.setCreatedAt(1);
    task2.setCeTaskUuid("id1");
    task2.setCreatedAt(1);
    task3.setCeTaskUuid("id2");
    task3.setCreatedAt(1);
    task4.setCeTaskUuid("id1");
    task4.setCreatedAt(2);
  }

  @Test
  public void equals_is_based_on_created_date_and_uuid() {
    assertThat(task1)
      .isEqualTo(task2)
      .isNotEqualTo(task3)
      .isNotEqualTo(task4);
  }

  @Test
  public void hashCode_is_based_on_created_date_and_uuid() {
    assertThat(task1).hasSameHashCodeAs(task2);
  }

  @Test
  public void compareTo_is_based_on_created_date_and_uuid() {
    assertThat(task1)
      .isEqualByComparingTo(task2)
      .isLessThan(task3)
      .isLessThan(task4);
  }
}
