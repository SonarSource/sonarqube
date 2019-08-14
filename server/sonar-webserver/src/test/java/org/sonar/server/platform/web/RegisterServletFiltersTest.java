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
package org.sonar.server.platform.web;

import org.junit.Test;
import org.sonar.api.web.ServletFilter;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class RegisterServletFiltersTest {
  @Test
  public void should_not_fail_if_master_filter_is_not_up() {
    MasterServletFilter.setInstance(null);
    new RegisterServletFilters(new ServletFilter[2]).start();
  }

  @Test
  public void should_register_filters_if_master_filter_is_up() {
    MasterServletFilter.setInstance(mock(MasterServletFilter.class));
    new RegisterServletFilters(new ServletFilter[2]).start();

    verify(MasterServletFilter.getInstance()).initFilters(anyList());
  }

  @Test
  public void filters_should_be_optional() {
    MasterServletFilter.setInstance(mock(MasterServletFilter.class));
    new RegisterServletFilters().start();
    // do not fail
    verify(MasterServletFilter.getInstance()).initFilters(anyList());
  }
}
