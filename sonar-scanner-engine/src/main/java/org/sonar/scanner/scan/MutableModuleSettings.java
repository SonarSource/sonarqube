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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.sonar.api.batch.AnalysisMode;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.MessageException;
import org.sonar.scanner.bootstrap.MutableGlobalSettings;
import org.sonar.scanner.repository.ProjectRepositories;

import static java.util.Objects.requireNonNull;

/**
 * @deprecated since 6.5 {@link ModuleSettings} used to be mutable, so keep a mutable copy for backward compatibility.
 */
@Deprecated
public class MutableModuleSettings extends Settings {

  private final ProjectRepositories projectRepos;
  private final AnalysisMode analysisMode;
  private final Map<String, String> properties = new HashMap<>();

  public MutableModuleSettings(MutableGlobalSettings batchSettings, ProjectDefinition moduleDefinition, ProjectRepositories projectSettingsRepo,
    AnalysisMode analysisMode) {
    super(batchSettings.getDefinitions(), batchSettings.getEncryption());
    this.projectRepos = projectSettingsRepo;
    this.analysisMode = analysisMode;

    init(moduleDefinition, batchSettings);
  }

  private MutableModuleSettings init(ProjectDefinition moduleDefinition, MutableGlobalSettings batchSettings) {
    addProjectProperties(moduleDefinition, batchSettings);
    addBuildProperties(moduleDefinition);
    return this;
  }

  private void addProjectProperties(ProjectDefinition def, MutableGlobalSettings batchSettings) {
    addProperties(batchSettings.getProperties());
    do {
      if (projectRepos.moduleExists(def.getKeyWithBranch())) {
        addProperties(projectRepos.settings(def.getKeyWithBranch()));
        break;
      }
      def = def.getParent();
    } while (def != null);
  }

  private void addBuildProperties(ProjectDefinition project) {
    List<ProjectDefinition> orderedProjects = ModuleSettingsProvider.getTopDownParentProjects(project);
    for (ProjectDefinition p : orderedProjects) {
      addProperties(p.properties());
    }
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
  protected void set(String key, String value) {
    properties.put(
      requireNonNull(key, "key can't be null"),
      requireNonNull(value, "value can't be null").trim());
  }

  @Override
  protected void remove(String key) {
    properties.remove(key);
  }

  @Override
  public Map<String, String> getProperties() {
    return properties;
  }
}
