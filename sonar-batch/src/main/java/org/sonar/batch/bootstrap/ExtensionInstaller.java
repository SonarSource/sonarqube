/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchComponent;
import org.sonar.api.CoreProperties;
import org.sonar.api.ExtensionProvider;
import org.sonar.api.Plugin;
import org.sonar.api.batch.InstantiationStrategy;
import org.sonar.api.config.Settings;
import org.sonar.api.platform.ComponentContainer;
import org.sonar.api.platform.PluginMetadata;
import org.sonar.api.resources.Project;
import org.sonar.batch.tasks.TaskDefinition;
import org.sonar.batch.bootstrapper.EnvironmentInformation;

import javax.annotation.Nullable;

import java.util.List;
import java.util.Map;

public class ExtensionInstaller implements BatchComponent {

  private static final Logger LOG = LoggerFactory.getLogger(ExtensionInstaller.class);

  private BatchPluginRepository pluginRepository;
  private EnvironmentInformation environment;
  private Settings settings;

  public ExtensionInstaller(BatchPluginRepository pluginRepository, EnvironmentInformation environment, Settings settings) {
    this.pluginRepository = pluginRepository;
    this.environment = environment;
    this.settings = settings;
  }

  public void installTaskDefinitionExtensions(ComponentContainer container) {
    for (Map.Entry<PluginMetadata, Plugin> entry : pluginRepository.getPluginsByMetadata().entrySet()) {
      PluginMetadata metadata = entry.getKey();
      Plugin plugin = entry.getValue();

      container.addExtension(metadata, plugin);
      for (Object extension : plugin.getExtensions()) {
        installTaskDefinition(container, metadata, extension);
      }
    }
  }

  boolean installTaskDefinition(ComponentContainer container, @Nullable PluginMetadata plugin, Object extension) {
    boolean installed;
    if (ExtensionUtils.isType(extension, TaskDefinition.class)
      && ExtensionUtils.supportsEnvironment(extension, environment)) {
      LOG.debug("Installing task definition extension {} from plugin {}", extension.toString(), plugin.getKey());
      container.addExtension(plugin, extension);
      installed = true;
    } else {
      container.declareExtension(plugin, extension);
      installed = false;
    }
    return installed;
  }

  public void installTaskExtensions(ComponentContainer container, boolean projectPresent) {
    boolean dryRun = settings.getBoolean(CoreProperties.DRY_RUN);
    for (Map.Entry<PluginMetadata, Plugin> entry : pluginRepository.getPluginsByMetadata().entrySet()) {
      PluginMetadata metadata = entry.getKey();
      Plugin plugin = entry.getValue();

      container.addExtension(metadata, plugin);
      for (Object extension : plugin.getExtensions()) {
        installTaskExtension(container, metadata, extension, projectPresent);
        if (projectPresent) {
          installBatchExtension(container, metadata, extension, dryRun, InstantiationStrategy.PER_BATCH);
        }
      }
    }

    List<ExtensionProvider> providers = container.getComponentsByType(ExtensionProvider.class);
    for (ExtensionProvider provider : providers) {
      executeTaskExtensionProvider(container, provider, projectPresent);
      if (projectPresent) {
        executeBatchExtensionProvider(container, InstantiationStrategy.PER_BATCH, dryRun, provider);
      }
    }
  }

  private void executeTaskExtensionProvider(ComponentContainer container, ExtensionProvider provider, boolean projectPresent) {
    Object obj = provider.provide();
    if (obj instanceof Iterable) {
      for (Object extension : (Iterable) obj) {
        installTaskExtension(container, null, extension, projectPresent);
      }
    } else {
      installTaskExtension(container, null, obj, projectPresent);
    }
  }

  boolean installTaskExtension(ComponentContainer container, @Nullable PluginMetadata plugin, Object extension, boolean projectPresent) {
    boolean installed;
    if (ExtensionUtils.isTaskExtension(extension) &&
      (projectPresent || !ExtensionUtils.requiresProject(extension)) &&
      ExtensionUtils.supportsEnvironment(extension, environment)) {
      logInstallExtension("task", plugin, extension);
      container.addExtension(plugin, extension);
      installed = true;
    } else {
      container.declareExtension(plugin, extension);
      installed = false;
    }
    return installed;
  }

  public void installInspectionExtensions(ComponentContainer container) {
    installBatchExtensions(container, InstantiationStrategy.PER_PROJECT);
  }

  @VisibleForTesting
  void installBatchExtensions(ComponentContainer container, String instantiationStrategy) {
    boolean dryRun = settings.getBoolean(CoreProperties.DRY_RUN);
    for (Map.Entry<PluginMetadata, Plugin> entry : pluginRepository.getPluginsByMetadata().entrySet()) {
      PluginMetadata metadata = entry.getKey();
      Plugin plugin = entry.getValue();

      container.addExtension(metadata, plugin);
      for (Object extension : plugin.getExtensions()) {
        installBatchExtension(container, metadata, extension, dryRun, instantiationStrategy);
      }
    }

    List<ExtensionProvider> providers = container.getComponentsByType(ExtensionProvider.class);
    for (ExtensionProvider provider : providers) {
      executeBatchExtensionProvider(container, instantiationStrategy, dryRun, provider);
    }
  }

  private void executeBatchExtensionProvider(ComponentContainer container, String instantiationStrategy, boolean dryRun, ExtensionProvider provider) {
    Object obj = provider.provide();
    if (obj instanceof Iterable) {
      for (Object extension : (Iterable) obj) {
        installBatchExtension(container, null, extension, dryRun, instantiationStrategy);
      }
    } else {
      installBatchExtension(container, null, obj, dryRun, instantiationStrategy);
    }
  }

  boolean installBatchExtension(ComponentContainer container, @Nullable PluginMetadata plugin, Object extension, boolean dryRun, String instantiationStrategy) {
    boolean installed;
    if (ExtensionUtils.isBatchExtension(extension) &&
      ExtensionUtils.supportsEnvironment(extension, environment) &&
      (!dryRun || ExtensionUtils.supportsDryRun(extension)) &&
      ExtensionUtils.isInstantiationStrategy(extension, instantiationStrategy) &&
      !isMavenExtensionOnEmulatedMavenProject(extension, instantiationStrategy, container)) {
      logInstallExtension("batch", plugin, extension);
      container.addExtension(plugin, extension);
      installed = true;
    } else {
      container.declareExtension(plugin, extension);
      installed = false;
    }
    return installed;
  }

  /**
   * Special use-case: the extension point ProjectBuilder is used in a Maven environment to define some
   * new sub-projects without pom.
   * Example : C# plugin adds sub-projects at runtime, even if they are not defined in root pom.
   */
  static boolean isMavenExtensionOnEmulatedMavenProject(Object extension, String instantiationStrategy, ComponentContainer container) {
    if (InstantiationStrategy.PER_PROJECT.equals(instantiationStrategy) && ExtensionUtils.isMavenExtensionOnly(extension)) {
      Project project = container.getComponentByType(Project.class);
      return project != null && project.getPom() == null;
    }
    return false;
  }

  private void logInstallExtension(String extensionType, PluginMetadata plugin, Object extension) {
    if (plugin != null) {
      LOG.debug("Installing {} extension {} from plugin {}", new String[] {extensionType, extension.toString(), plugin.getKey()});
    }
    else {
      LOG.debug("Installing {} extension {}", extensionType, extension.toString());
    }
  }

}
