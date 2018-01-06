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
package org.sonar.scanner.scan.branch;

import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.config.Configuration;
import org.sonar.scanner.bootstrap.GlobalConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProjectBranchesProviderTest {
  private ProjectBranchesProvider provider = new ProjectBranchesProvider();
  private ProjectBranchesLoader mockLoader;
  private ProjectBranches mockBranches;
  private GlobalConfiguration mockSettings;

  @Before
  public void setUp() {
    mockLoader = mock(ProjectBranchesLoader.class);
    mockBranches = mock(ProjectBranches.class);
    mockSettings = mock(GlobalConfiguration.class);
  }

  @Test
  public void should_cache_branches() {
    ProjectBranches branches = provider.provide(null, () -> "project", mockSettings);
    assertThat(provider.provide(null, () -> "project", mockSettings)).isSameAs(branches);
  }

  @Test
  public void should_use_loader() {
    when(mockLoader.load("key")).thenReturn(mockBranches);
    when(mockSettings.get(anyString())).thenReturn(Optional.of("somebranch"));
    ProjectBranches branches = provider.provide(mockLoader, () -> "key", mockSettings);

    assertThat(branches).isSameAs(mockBranches);
  }

  @Test
  public void should_return_default_if_no_loader() {
    ProjectBranches branches = provider.provide(null, () -> "project", mockSettings);
    assertThat(branches.isEmpty()).isTrue();
  }
}
