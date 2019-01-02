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
package org.sonar.scanner.scan.branch;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProjectBranchesProviderTest {
  private ProjectBranchesProvider provider = new ProjectBranchesProvider();
  private ProjectBranchesLoader mockLoader;
  private ProjectBranches mockBranches;

  @Before
  public void setUp() {
    mockLoader = mock(ProjectBranchesLoader.class);
    mockBranches = mock(ProjectBranches.class);
  }

  @Test
  public void should_cache_branches() {
    ProjectBranches branches = provider.provide(null, () -> "project");
    assertThat(provider.provide(null, () -> "project")).isSameAs(branches);
  }

  @Test
  public void should_use_loader() {
    when(mockLoader.load("key")).thenReturn(mockBranches);
    ProjectBranches branches = provider.provide(mockLoader, () -> "key");

    assertThat(branches).isSameAs(mockBranches);
  }

  @Test
  public void should_return_default_if_no_loader() {
    ProjectBranches branches = provider.provide(null, () -> "project");
    assertThat(branches.isEmpty()).isTrue();
  }
}
