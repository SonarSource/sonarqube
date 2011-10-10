/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.batch.bootstrap;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.BatchComponent;
import org.sonar.api.CoreProperties;
import org.sonar.api.ExtensionProvider;
import org.sonar.api.Plugin;
import org.sonar.api.batch.AbstractCoverageExtension;
import org.sonar.api.batch.CoverageExtension;
import org.sonar.api.batch.InstantiationStrategy;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.Project;
import org.sonar.batch.bootstrapper.EnvironmentInformation;
import org.sonar.batch.config.ProjectSettings;

import java.util.List;
import java.util.Map;

public final class ProjectExtensionInstaller implements BatchComponent {

  private BatchPluginRepository pluginRepository;
  private EnvironmentInformation environment;
  private DryRun dryRun;
  private Project project;
  private ProjectSettings settings;

  public ProjectExtensionInstaller(BatchPluginRepository pluginRepository, EnvironmentInformation environment, DryRun dryRun, Project project, ProjectSettings settings) {
    this.pluginRepository = pluginRepository;
    this.environment = environment;
    this.dryRun = dryRun;
    this.project = project;
    this.settings = settings;
  }

  public void install(Module module) {
    for (Map.Entry<String, Plugin> entry : pluginRepository.getPluginsByKey().entrySet()) {
      for (Object extension : entry.getValue().getExtensions()) {
        installExtension(module, extension, entry.getKey());
      }
    }
    installExtensionProviders(module);
  }

  void installExtensionProviders(Module module) {
    List<ExtensionProvider> providers = module.getComponents(ExtensionProvider.class);
    for (ExtensionProvider provider : providers) {
      Object obj = provider.provide();
      if (obj instanceof Iterable) {
        for (Object extension : (Iterable) obj) {
          installExtension(module, extension, "");
        }
      } else {
        installExtension(module, obj, "");
      }
    }
  }

  private Object installExtension(Module module, Object extension, String pluginKey) {
    if (ExtensionUtils.isBatchExtension(extension) &&
        ExtensionUtils.isSupportedEnvironment(extension, environment) &&
        ExtensionUtils.isInstantiationStrategy(extension, InstantiationStrategy.PER_PROJECT) &&
        ExtensionUtils.checkDryRun(extension, dryRun.isEnabled()) &&
        !isDeactivatedCoverageExtension(extension, pluginKey, project, settings) &&
        !isMavenExtensionOnEmulatedMavenProject(extension, project)) {
      module.addCoreSingleton(extension);
      return extension;
    }
    return null;
  }

  /**
   * Special use-case: the extension point ProjectBuilder is used in a Maven environment to define some
   * new sub-projects without pom.
   * Example : C# plugin adds sub-projects at runtime, even if they are not defined in root pom.
   */
  static boolean isMavenExtensionOnEmulatedMavenProject(Object extension, Project project) {
    return ExtensionUtils.isMavenExtensionOnly(extension) && project.getPom() == null;
  }

  /**
   * TODO this code is specific to Java projects and should be moved somewhere else
   */
  static boolean isDeactivatedCoverageExtension(Object extension, String pluginKey, Project project, Settings settings) {
    if (!ExtensionUtils.isType(extension, CoverageExtension.class)) {
      return false;
    }

    if (!project.getAnalysisType().isDynamic(true)) {
      // not dynamic and not reuse reports
      return true;
    }

    if (StringUtils.equals(project.getLanguageKey(), Java.KEY)) {
      String[] selectedPluginKeys = settings.getStringArray(CoreProperties.CORE_COVERAGE_PLUGIN_PROPERTY);
      if (ArrayUtils.isEmpty(selectedPluginKeys)) {
        selectedPluginKeys = new String[]{AbstractCoverageExtension.DEFAULT_PLUGIN};
      }
      return !ArrayUtils.contains(selectedPluginKeys, pluginKey);
    }
    return false;
  }
}
