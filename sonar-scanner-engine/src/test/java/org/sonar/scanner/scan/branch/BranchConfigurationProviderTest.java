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
package org.sonar.scanner.scan.branch;

import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.scanner.scan.ProjectConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BranchConfigurationProviderTest {
  private BranchConfigurationProvider provider = new BranchConfigurationProvider();
  private ProjectConfiguration projectConfiguration = mock(ProjectConfiguration.class);
  private BranchConfigurationLoader loader = mock(BranchConfigurationLoader.class);
  private BranchConfiguration config = mock(BranchConfiguration.class);
  private ProjectBranches branches = mock(ProjectBranches.class);
  private ProjectReactor reactor = mock(ProjectReactor.class);
  private Map<String, String> projectSettings = new HashMap<>();
  private ProjectDefinition root = mock(ProjectDefinition.class);

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    when(projectConfiguration.getProperties()).thenReturn(projectSettings);
    when(reactor.getRoot()).thenReturn(root);
  }

  @Test
  public void should_use_loader() {
    when(loader.load(eq(projectSettings), eq(branches))).thenReturn(config);

    BranchConfiguration result = provider.provide(loader, projectConfiguration, branches);

    assertThat(result).isSameAs(config);
  }

  @Test
  public void should_return_default_if_no_loader() {
    BranchConfiguration result = provider.provide(null, projectConfiguration, branches);

    assertThat(result.targetBranchName()).isNull();
    assertThat(result.branchType()).isEqualTo(BranchType.BRANCH);
  }
}
