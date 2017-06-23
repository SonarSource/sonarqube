/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.concurrent.Immutable;

import org.sonar.api.batch.bootstrap.ImmutableProjectDefinition;
import org.sonar.api.config.ImmutableSettings;
import org.sonar.api.utils.MessageException;
import org.sonar.scanner.analysis.DefaultAnalysisMode;
import org.sonar.scanner.bootstrap.GlobalSettings;
import org.sonar.scanner.report.AnalysisContextReportPublisher;
import org.sonar.scanner.repository.ProjectRepositories;

/**
 * @since 2.12
 */
@Immutable
public class ModuleSettings extends ImmutableSettings {

  private final ProjectRepositories projectRepos;
  private final DefaultAnalysisMode analysisMode;
  private final Map<String, String> properties;

  public ModuleSettings(GlobalSettings batchSettings, ImmutableProjectDefinition moduleDefinition, ProjectRepositories projectSettingsRepo,
    DefaultAnalysisMode analysisMode, AnalysisContextReportPublisher contextReportPublisher) {
    super(batchSettings.getDefinitions(), batchSettings.getEncryption());
    this.projectRepos = projectSettingsRepo;
    this.analysisMode = analysisMode;

    Map<String, String> props = init(moduleDefinition, batchSettings);
    contextReportPublisher.dumpModuleSettings(moduleDefinition);
    this.properties = Collections.unmodifiableMap(props);
  }

  private Map<String, String> init(ImmutableProjectDefinition moduleDefinition, GlobalSettings batchSettings) {
    Map<String, String> props = new HashMap<>();
    addProjectProperties(moduleDefinition, batchSettings, props);
    addBuildProperties(moduleDefinition, props);
    return props;
  }

  private void addProjectProperties(ImmutableProjectDefinition def, GlobalSettings batchSettings, Map<String, String> props) {
    addProperties(batchSettings.getProperties(), props);
    do {
      if (projectRepos.moduleExists(def.getKeyWithBranch())) {
        addProperties(projectRepos.settings(def.getKeyWithBranch()), props);
        break;
      }
      def = def.getParent();
    } while (def != null);
  }

  private void addBuildProperties(ImmutableProjectDefinition project, Map<String, String> props) {
    List<ImmutableProjectDefinition> orderedProjects = getTopDownParentProjects(project);
    for (ImmutableProjectDefinition p : orderedProjects) {
      addProperties(p.properties(), props);
    }
  }

  /**
   * From root to given project
   */
  static List<ImmutableProjectDefinition> getTopDownParentProjects(ImmutableProjectDefinition project) {
    List<ImmutableProjectDefinition> result = new ArrayList<>();
    ImmutableProjectDefinition p = project;
    while (p != null) {
      result.add(0, p);
      p = p.getParent();
    }
    return result;
  }

  @Override
  protected Optional<String> get(String key) {
    if (analysisMode.isIssues() && key.endsWith(".secured") && !key.contains(".license")) {
      throw MessageException.of("Access to the secured property '" + key
        + "' is not possible in issues mode. The SonarQube plugin which requires this property must be deactivated in issues mode.");
    }
    return Optional.ofNullable(properties.get(key));
  }

  @Override
  public Map<String, String> getProperties() {
    return properties;
  }
}
