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
package org.sonar.server.platform.web;

import org.junit.jupiter.api.Test;
import org.sonar.api.web.HttpFilter;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RegisterServletFiltersTest {
  @Test
  void should_not_fail_if_master_filter_is_not_up() {
    MasterServletFilter.setInstance(null);

    RegisterServletFilters underTest = new RegisterServletFilters(new HttpFilter[2]);

    assertThatCode(underTest::start)
      .doesNotThrowAnyException();
  }

  @Test
  void filters_should_be_optional() {
    MasterServletFilter.setInstance(mock(MasterServletFilter.class));

    RegisterServletFilters underTest = new RegisterServletFilters();
    assertThatCode(underTest::start)
      .doesNotThrowAnyException();

    verify(MasterServletFilter.getInstance()).initHttpFilters(anyList());
  }
}
