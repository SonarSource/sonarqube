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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.sonar.api.BatchComponent;
import org.sonar.api.Extension;
import org.sonar.api.ExtensionProvider;
import org.sonar.api.Plugin;
import org.sonar.api.batch.CoverageExtension;
import org.sonar.api.batch.InstantiationStrategy;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.Metrics;
import org.sonar.api.platform.PluginMetadata;
import org.sonar.batch.bootstrapper.EnvironmentInformation;

import java.util.Map;

public final class BatchExtensionInstaller implements BatchComponent {

  private BatchPluginRepository pluginRepository;
  private EnvironmentInformation environment;
  private DryRun dryRun;

  public BatchExtensionInstaller(BatchPluginRepository pluginRepository, EnvironmentInformation environment, DryRun dryRun) {
    this.pluginRepository = pluginRepository;
    this.environment = environment;
    this.dryRun = dryRun;
  }

  public void install(Module module) {
    ListMultimap<PluginMetadata, Object> installedExtensionsByPlugin = ArrayListMultimap.create();
    for (Map.Entry<PluginMetadata, Plugin> entry : pluginRepository.getPluginsByMetadata().entrySet()) {
      PluginMetadata metadata = entry.getKey();
      Plugin plugin = entry.getValue();
      
      module.addExtension(metadata, plugin);

      for (Object extension : plugin.getExtensions()) {
        if (installExtension(module, metadata, extension)) {
          installedExtensionsByPlugin.put(metadata, extension);
        } else {
          module.declareExtension(metadata, extension);
        }
      }
    }
    for (Map.Entry<PluginMetadata, Object> entry : installedExtensionsByPlugin.entries()) {
      PluginMetadata plugin = entry.getKey();
      Object extension = entry.getValue();
      if (isExtensionProvider(extension)) {
        ExtensionProvider provider = (ExtensionProvider) module.getComponentByKey(extension);
        installProvider(module, plugin, provider);
      }
    }
    installMetrics(module);
  }

  static boolean isExtensionProvider(Object extension) {
    return isType(extension, ExtensionProvider.class) || extension instanceof ExtensionProvider;
  }

  static boolean isType(Object extension, Class<? extends Extension> extensionClass) {
    Class clazz = (extension instanceof Class ? (Class) extension : extension.getClass());
    return extensionClass.isAssignableFrom(clazz);
  }

  private void installMetrics(Module module) {
    for (Metrics metrics : module.getComponents(Metrics.class)) {
      for (Metric metric : metrics.getMetrics()) {
        module.addCoreSingleton(metric);
      }
    }
  }

  private void installProvider(Module module, PluginMetadata plugin, ExtensionProvider provider) {
    Object obj = provider.provide();
    if (obj != null) {
      if (obj instanceof Iterable) {
        for (Object ext : (Iterable) obj) {
          installExtension(module, plugin, ext);
        }
      } else {
        installExtension(module, plugin, obj);
      }
    }
  }

  boolean installExtension(Module module, PluginMetadata plugin, Object extension) {
    if (ExtensionUtils.isBatchExtension(extension) &&
        ExtensionUtils.isSupportedEnvironment(extension, environment) &&
        ExtensionUtils.checkDryRun(extension, dryRun.isEnabled()) &&
        ExtensionUtils.isInstantiationStrategy(extension, InstantiationStrategy.PER_BATCH)) {
      if (ExtensionUtils.isType(extension, CoverageExtension.class)) {
        throw new IllegalArgumentException("Instantiation strategy " + InstantiationStrategy.PER_BATCH + " is not supported on CoverageExtension components: " + extension);
      }
      module.addExtension(plugin, extension);
      return true;
    }
    return false;
  }
}
