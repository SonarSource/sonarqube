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
import org.sonar.api.ExtensionProvider;
import org.sonar.api.Plugin;
import org.sonar.api.batch.AbstractCoverageExtension;
import org.sonar.api.batch.CoverageExtension;
import org.sonar.api.batch.InstantiationStrategy;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.Project;
import org.sonar.batch.bootstrapper.EnvironmentInformation;

import java.util.List;
import java.util.Map;

public final class ProjectExtensionInstaller implements BatchComponent {

  private BatchPluginRepository pluginRepository;
  private EnvironmentInformation environment;

  public ProjectExtensionInstaller(BatchPluginRepository pluginRepository, EnvironmentInformation environment) {
    this.pluginRepository = pluginRepository;
    this.environment = environment;
  }

  public void install(Module module, Project project) {
    for (Map.Entry<String, Plugin> entry : pluginRepository.getPluginsByKey().entrySet()) {
      for (Object extension : entry.getValue().getExtensions()) {
        installExtension(module, extension, project, entry.getKey());
      }
    }
    installExtensionProviders(module, project);
  }

  void installExtensionProviders(Module module, Project project) {
    List<ExtensionProvider> providers = module.getComponents(ExtensionProvider.class);
    for (ExtensionProvider provider : providers) {
      Object obj = provider.provide();
      if (obj instanceof Iterable) {
        for (Object extension : (Iterable) obj) {
          installExtension(module, extension, project, "");
        }
      } else {
        installExtension(module, obj, project, "");
      }
    }
  }

  private Object installExtension(Module module, Object extension, Project project, String pluginKey) {
    if (ExtensionUtils.isBatchExtension(extension) &&
        ExtensionUtils.isSupportedEnvironment(extension, environment) &&
        ExtensionUtils.isInstantiationStrategy(extension, InstantiationStrategy.PER_PROJECT) &&
        !isDeactivatedCoverageExtension(extension, project, pluginKey) &&
        !isMavenExtensionOnEmulatedMavenProject(extension, project)) {

      module.addComponent(extension);
      return extension;
    }
    return null;
  }

  boolean isMavenExtensionOnEmulatedMavenProject(Object extension, Project project) {
    return ExtensionUtils.isMavenExtensionOnly(extension) && project.getPom() == null;
  }

  /**
   * TODO this code is specific to Java projects and should be moved somewhere else
   */
  boolean isDeactivatedCoverageExtension(Object extension, Project project, String pluginKey) {
    if (!ExtensionUtils.isType(extension, CoverageExtension.class)) {
      return false;
    }

    if (!project.getAnalysisType().isDynamic(true)) {
      // not dynamic and not reuse reports
      return true;
    }

    if (StringUtils.equals(project.getLanguageKey(), Java.KEY)) {
      String[] selectedPluginKeys = project.getConfiguration().getStringArray(AbstractCoverageExtension.PARAM_PLUGIN);
      if (ArrayUtils.isEmpty(selectedPluginKeys)) {
        selectedPluginKeys = new String[]{AbstractCoverageExtension.DEFAULT_PLUGIN};
      }
      return !ArrayUtils.contains(selectedPluginKeys, pluginKey);
    }
    return false;
  }
}
