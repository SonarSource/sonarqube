/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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

import java.util.LinkedHashMap;
import java.util.Map;
import org.sonar.api.batch.fs.internal.DefaultInputProject;
import org.sonar.scanner.bootstrap.GlobalConfiguration;
import org.sonar.scanner.bootstrap.GlobalServerSettings;
import org.springframework.context.annotation.Bean;


public class ProjectConfigurationProvider {

  private final SonarGlobalPropertiesFilter sonarGlobalPropertiesFilter;

  public ProjectConfigurationProvider(SonarGlobalPropertiesFilter sonarGlobalPropertiesFilter) {
    this.sonarGlobalPropertiesFilter = sonarGlobalPropertiesFilter;
  }

  @Bean("ProjectConfiguration")
  public ProjectConfiguration provide(DefaultInputProject project, GlobalConfiguration globalConfig, GlobalServerSettings globalServerSettings,
    ProjectServerSettings projectServerSettings, MutableProjectSettings projectSettings) {
    Map<String, String> settings = new LinkedHashMap<>();
    settings.putAll(globalServerSettings.properties());
    settings.putAll(projectServerSettings.properties());
    settings.putAll(project.properties());

    settings = sonarGlobalPropertiesFilter.enforceOnlyServerSideSonarGlobalPropertiesAreUsed(settings, globalServerSettings.properties());

    ProjectConfiguration projectConfig = new ProjectConfiguration(globalConfig.getDefinitions(), globalConfig.getEncryption(), settings);
    projectSettings.complete(projectConfig);
    return projectConfig;
  }


}
