/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.ce.task.projectexport.steps;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

public class MutableMetricRepositoryImplTest {

  MutableMetricRepository underTest = new MutableMetricRepositoryImpl();

  @Test
  public void add_ref() {
    underTest.add("10");
    underTest.add("12");

    assertThat(underTest.getRefByUuid().entrySet()).containsOnly(entry("10", 0), entry("12", 1));
  }

  @Test
  public void add_multiple_times_the_same_ref() {
    underTest.add("10");
    underTest.add("10");

    assertThat(underTest.getRefByUuid().entrySet()).containsExactly(entry("10", 0));
  }

  @Test
  public void getAll_returns_empty_set() {
    assertThat(underTest.getRefByUuid()).isEmpty();
  }
}
