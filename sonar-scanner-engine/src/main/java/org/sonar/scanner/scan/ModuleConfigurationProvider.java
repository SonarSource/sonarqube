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
package org.sonar.scanner.scan;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.picocontainer.injectors.ProviderAdapter;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.scanner.bootstrap.GlobalConfiguration;
import org.sonar.scanner.bootstrap.GlobalServerSettings;

public class ModuleConfigurationProvider extends ProviderAdapter {

  private ModuleConfiguration moduleConfiguration;

  public ModuleConfiguration provide(GlobalConfiguration globalConfig, DefaultInputModule module, GlobalServerSettings globalServerSettings,
    ProjectServerSettings projectServerSettings) {
    if (moduleConfiguration == null) {

      Map<String, String> settings = new LinkedHashMap<>();
      settings.putAll(globalServerSettings.properties());
      settings.putAll(projectServerSettings.properties());
      addScannerSideProperties(settings, module.definition());

      moduleConfiguration = new ModuleConfiguration(globalConfig.getDefinitions(), globalConfig.getEncryption(), settings);
    }
    return moduleConfiguration;
  }

  private static void addScannerSideProperties(Map<String, String> settings, ProjectDefinition project) {
    List<ProjectDefinition> orderedProjects = getTopDownParentProjects(project);
    for (ProjectDefinition p : orderedProjects) {
      settings.putAll(p.properties());
    }
  }

  /**
   * From root to given project
   */
  static List<ProjectDefinition> getTopDownParentProjects(ProjectDefinition project) {
    List<ProjectDefinition> result = new ArrayList<>();
    ProjectDefinition p = project;
    while (p != null) {
      result.add(0, p);
      p = p.getParent();
    }
    return result;
  }
}
