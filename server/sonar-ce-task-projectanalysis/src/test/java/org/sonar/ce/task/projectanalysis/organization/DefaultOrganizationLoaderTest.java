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
package org.sonar.ce.task.projectanalysis.organization;

import org.junit.Test;
import org.sonar.server.organization.DefaultOrganizationCache;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class DefaultOrganizationLoaderTest {
  private DefaultOrganizationCache defaultOrganizationCache = mock(DefaultOrganizationCache.class);
  private DefaultOrganizationLoader underTest = new DefaultOrganizationLoader(defaultOrganizationCache);

  @Test
  public void start_calls_cache_load_method() {
    underTest.start();

    verify(defaultOrganizationCache).load();
    verifyNoMoreInteractions(defaultOrganizationCache);
  }

  @Test
  public void stop_calls_cache_unload_method() {
    underTest.stop();

    verify(defaultOrganizationCache).unload();
    verifyNoMoreInteractions(defaultOrganizationCache);
  }
}
