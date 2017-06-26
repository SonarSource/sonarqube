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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.batch.fs.internal.InputModuleHierarchy;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.MessageException;
import org.sonar.scanner.analysis.DefaultAnalysisMode;
import org.sonar.scanner.bootstrap.GlobalSettings;
import org.sonar.scanner.report.AnalysisContextReportPublisher;
import org.sonar.scanner.repository.ProjectRepositories;

/**
 * @since 2.12
 */
public class ModuleSettings extends Settings {

  private final ProjectRepositories projectRepos;
  private final DefaultAnalysisMode analysisMode;
  private final Map<String, String> properties = new HashMap<>();
  private final InputModuleHierarchy hierarchy;
  private final DefaultInputModule module;
  private final GlobalSettings batchSettings;

  public ModuleSettings(GlobalSettings batchSettings, DefaultInputModule module, InputModuleHierarchy hierarchy, ProjectRepositories projectSettingsRepo,
    DefaultAnalysisMode analysisMode, AnalysisContextReportPublisher contextReportPublisher) {
    super(batchSettings.getDefinitions(), batchSettings.getEncryption());
    this.batchSettings = batchSettings;
    this.module = module;
    this.hierarchy = hierarchy;
    this.projectRepos = projectSettingsRepo;
    this.analysisMode = analysisMode;

    init();
    contextReportPublisher.dumpModuleSettings(module);
  }

  private ModuleSettings init() {
    addProjectProperties();
    addBuildProperties();
    return this;
  }

  private void addProjectProperties() {
    DefaultInputModule m = module;
    addProperties(batchSettings.getProperties());
    do {
      if (projectRepos.moduleExists(m.getKeyWithBranch())) {
        addProperties(projectRepos.settings(m.getKeyWithBranch()));
        break;
      }
      m = hierarchy.parent(m);
    } while (m != null);
  }

  private void addBuildProperties() {
    List<DefaultInputModule> orderedProjects = getTopDownParentProjects();
    for (DefaultInputModule p : orderedProjects) {
      addProperties(p.properties());
    }
  }

  /**
   * From root to current project
   */
  List<DefaultInputModule> getTopDownParentProjects() {
    LinkedList<DefaultInputModule> result = new LinkedList<>();
    DefaultInputModule m = module;
    while (m != null) {
      result.addFirst(m);
      m = hierarchy.parent(m);
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
  protected void set(String key, String value) {
    properties.put(key, value);
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
