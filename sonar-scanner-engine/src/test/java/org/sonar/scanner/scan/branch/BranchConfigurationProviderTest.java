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

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.bootstrap.ProjectKey;
import org.sonar.scanner.bootstrap.GlobalConfiguration;
import org.sonar.scanner.repository.settings.SettingsLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BranchConfigurationProviderTest {
  private BranchConfigurationProvider provider = new BranchConfigurationProvider();
  private GlobalConfiguration globalConfiguration;
  private BranchConfigurationLoader loader;
  private BranchConfiguration config;
  private ProjectBranches branches;
  private ProjectKey projectKey;
  private Map<String, String> globalPropertiesMap;
  private Map<String, String> remoteProjectSettings;
  private SettingsLoader settingsLoader;

  @Before
  public void setUp() {
    globalConfiguration = mock(GlobalConfiguration.class);
    loader = mock(BranchConfigurationLoader.class);
    config = mock(BranchConfiguration.class);
    branches = mock(ProjectBranches.class);
    settingsLoader = mock(SettingsLoader.class);
    projectKey = mock(ProjectKey.class);
    globalPropertiesMap = new HashMap<>();
    when(globalConfiguration.getProperties()).thenReturn(globalPropertiesMap);
    when(settingsLoader.load(anyString())).thenReturn(remoteProjectSettings);
  }

  @Test
  public void should_cache_config() {
    BranchConfiguration configuration = provider.provide(null, globalConfiguration, projectKey, settingsLoader, branches);
    assertThat(provider.provide(null, globalConfiguration, projectKey, settingsLoader, branches)).isSameAs(configuration);
  }

  @Test
  public void should_use_loader() {
    when(loader.load(eq(globalPropertiesMap), any(Supplier.class), eq(branches))).thenReturn(config);
    BranchConfiguration branchConfig = provider.provide(loader, globalConfiguration, projectKey, settingsLoader, branches);

    assertThat(branchConfig).isSameAs(config);
  }

  @Test
  public void should_return_default_if_no_loader() {
    BranchConfiguration configuration = provider.provide(null, globalConfiguration, projectKey, settingsLoader, branches);
    assertThat(configuration.branchTarget()).isNull();
    assertThat(configuration.branchType()).isEqualTo(BranchType.LONG);
  }
}
