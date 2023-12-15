/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.scanner.scan.filesystem;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class InputFileFilterRepositoryTest {

  @Test
  public void should_not_return_null_if_initialized_with_no_filters() {
    InputFileFilterRepository underTest = new InputFileFilterRepository();
    assertThat(underTest.getInputFileFilters()).isNotNull();
  }

  @Test
  public void should_return_filters_from_initialization() {
    InputFileFilterRepository underTest = new InputFileFilterRepository(f -> true);
    assertThat(underTest.getInputFileFilters()).isNotNull();
    assertThat(underTest.getInputFileFilters()).hasSize(1);
  }

}
