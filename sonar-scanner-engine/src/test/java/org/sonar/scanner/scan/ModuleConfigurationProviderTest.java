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
package org.sonar.scanner.scan;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.utils.System2;
import org.sonar.scanner.bootstrap.GlobalConfiguration;
import org.sonar.scanner.bootstrap.GlobalServerSettings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ModuleConfigurationProviderTest {

  private static final String GLOBAL_KEY_PROPERTIES_1 = "sonar.global.key1";
  private static final String NON_GLOBAL_KEY_PROPERTIES_1 = "sonar.key1";
  private static final String DEFAULT_KEY_PROPERTIES_1 = "default.key1";
  private static final String GLOBAL_VALUE_PROPERTIES_1 = "Value for " + GLOBAL_KEY_PROPERTIES_1;
  private static final String NON_GLOBAL_VALUE_PROPERTIES_1 = "Value for " + NON_GLOBAL_KEY_PROPERTIES_1;
  private static final String DEFAULT_VALUE_1 = "Value for " + DEFAULT_KEY_PROPERTIES_1;

  private static final Map<String, String> GLOBAL_SERVER_PROPERTIES = Map.of(GLOBAL_KEY_PROPERTIES_1, GLOBAL_VALUE_PROPERTIES_1);
  private static final Map<String, String> PROJECT_SERVER_PROPERTIES = Map.of(NON_GLOBAL_KEY_PROPERTIES_1, NON_GLOBAL_VALUE_PROPERTIES_1);
  private static final Map<String, String> DEFAULT_PROJECT_PROPERTIES = Map.of(DEFAULT_KEY_PROPERTIES_1, DEFAULT_VALUE_1);

  private static final Map<String, String> ALL_PROPERTIES_MAP =
    Stream.of(GLOBAL_SERVER_PROPERTIES, PROJECT_SERVER_PROPERTIES)
      .flatMap(map -> map.entrySet().stream())
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

  private static final Map<String, String> PROPERTIES_AFTER_FILTERING = Map.of("aKey", "aValue");

  @Mock
  private GlobalServerSettings globalServerSettings;
  @Mock
  private ProjectServerSettings projectServerSettings;
  @Mock
  private GlobalConfiguration globalConfiguration;
  @Mock
  private DefaultInputModule defaultInputProject;


  @Mock
  private SonarGlobalPropertiesFilter sonarGlobalPropertiesFilter;

  @InjectMocks
  private ModuleConfigurationProvider provider;

  @Before
  public void init() {
    when(globalConfiguration.getDefinitions()).thenReturn(new PropertyDefinitions(System2.INSTANCE));
  }

  @Test
  public void should_concatAllPropertiesForCallFilterAndApplyFilterChanges() {
    when(globalServerSettings.properties()).thenReturn(GLOBAL_SERVER_PROPERTIES);
    when(projectServerSettings.properties()).thenReturn(PROJECT_SERVER_PROPERTIES);
    when(sonarGlobalPropertiesFilter.enforceOnlyServerSideSonarGlobalPropertiesAreUsed(ALL_PROPERTIES_MAP, GLOBAL_SERVER_PROPERTIES))
      .thenReturn(PROPERTIES_AFTER_FILTERING);

    ModuleConfiguration provide = provider.provide(globalConfiguration, defaultInputProject, globalServerSettings, projectServerSettings);

    verify(sonarGlobalPropertiesFilter).enforceOnlyServerSideSonarGlobalPropertiesAreUsed(ALL_PROPERTIES_MAP, GLOBAL_SERVER_PROPERTIES);
    assertThat(provide.getOriginalProperties()).containsExactlyEntriesOf(PROPERTIES_AFTER_FILTERING);
  }


}