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
package org.sonar.server.v2.api.model;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RestPageTest {

  @Test
  public void constructor_whenNoValueProvided_setsDefaultValues() {
    RestPage restPage = new RestPage(null, null);
    assertThat(restPage.pageIndex()).asString().isEqualTo(RestPage.DEFAULT_PAGE_INDEX);
    assertThat(restPage.pageSize()).asString().isEqualTo(RestPage.DEFAULT_PAGE_SIZE);
  }

  @Test
  public void constructor_whenValuesProvided_useThem() {
    RestPage restPage = new RestPage(10, 100);
    assertThat(restPage.pageIndex()).isEqualTo(100);
    assertThat(restPage.pageSize()).isEqualTo(10);
  }

}
