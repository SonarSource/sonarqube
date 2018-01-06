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
package org.sonar.server.organization;

import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class DefaultOrganizationEnforcerTest {
  private DefaultOrganizationProvider defaultOrganizationProvider = mock(DefaultOrganizationProvider.class);
  private DefaultOrganizationEnforcer underTest = new DefaultOrganizationEnforcer(defaultOrganizationProvider);

  @Test
  public void start_calls_provider_get_method() {
    underTest.start();

    verify(defaultOrganizationProvider).get();
    verifyNoMoreInteractions(defaultOrganizationProvider);
  }

  @Test
  public void stop_does_nothing() {
    underTest.stop();

    verifyNoMoreInteractions(defaultOrganizationProvider);
  }
}
