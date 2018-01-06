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
package org.sonar.scanner.scan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.picocontainer.injectors.ProviderAdapter;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.scanner.bootstrap.GlobalAnalysisMode;
import org.sonar.scanner.bootstrap.GlobalConfiguration;
import org.sonar.scanner.report.AnalysisContextReportPublisher;
import org.sonar.scanner.repository.ProjectRepositories;

public class ModuleSettingsProvider extends ProviderAdapter {

  private ModuleSettings projectSettings;

  public ModuleSettings provide(GlobalConfiguration globalSettings, DefaultInputModule module, ProjectRepositories projectRepos,
    GlobalAnalysisMode analysisMode, AnalysisContextReportPublisher contextReportPublisher) {
    if (projectSettings == null) {

      Map<String, String> settings = new LinkedHashMap<>();
      settings.putAll(globalSettings.getProperties());
      settings.putAll(addServerSidePropertiesIfModuleExists(projectRepos, module.definition()));
      addScannerSideProperties(settings, module.definition());
      contextReportPublisher.dumpModuleSettings(module);

      projectSettings = new ModuleSettings(globalSettings.getDefinitions(), globalSettings.getEncryption(), analysisMode, settings);
    }
    return projectSettings;
  }

  private static Map<String, String> addServerSidePropertiesIfModuleExists(ProjectRepositories projectRepos, ProjectDefinition def) {
    if (projectRepos.moduleExists(def.getKeyWithBranch())) {
      return projectRepos.settings(def.getKeyWithBranch());
    } else {
      // Module doesn't exist on server. Try to add parent server settings as inheritance.
      ProjectDefinition parentDef = def.getParent();
      if (parentDef != null) {
        return addServerSidePropertiesIfModuleExists(projectRepos, parentDef);
      }
      return Collections.emptyMap();
    }
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
