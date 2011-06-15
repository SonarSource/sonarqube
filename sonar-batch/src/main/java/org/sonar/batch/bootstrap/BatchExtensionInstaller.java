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

import org.sonar.api.BatchComponent;
import org.sonar.api.ExtensionProvider;
import org.sonar.api.Plugin;
import org.sonar.api.batch.CoverageExtension;
import org.sonar.api.batch.InstantiationStrategy;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.Metrics;
import org.sonar.batch.bootstrapper.EnvironmentInformation;

import java.util.List;

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
    for (Plugin plugin : pluginRepository.getPlugins()) {
      for (Object extension : plugin.getExtensions()) {
        installExtension(module, extension);
      }
    }
    installExtensionProviders(module);
    installMetrics(module);
  }

  private void installMetrics(Module module) {
    for (Metrics metrics : module.getComponents(Metrics.class)) {
      for (Metric metric : metrics.getMetrics()) {
        module.addComponent(metric.getKey(), metric);
      }
    }
  }

  void installExtensionProviders(Module module) {
    List<ExtensionProvider> providers = module.getComponents(ExtensionProvider.class);
    for (ExtensionProvider provider : providers) {
      Object obj = provider.provide();
      if (obj instanceof Iterable) {
        for (Object extension : (Iterable) obj) {
          installExtension(module, extension);
        }
      } else {
        installExtension(module, obj);
      }
    }
  }

  void installExtension(Module module, Object extension) {
    if (ExtensionUtils.isBatchExtension(extension) &&
        ExtensionUtils.isSupportedEnvironment(extension, environment) &&
        ExtensionUtils.checkDryRun(extension, dryRun.isEnabled()) &&
        ExtensionUtils.isInstantiationStrategy(extension, InstantiationStrategy.PER_BATCH)) {
      if (ExtensionUtils.isType(extension, CoverageExtension.class)) {
        throw new IllegalArgumentException("Instantiation strategy " + InstantiationStrategy.PER_BATCH + " is not supported on CoverageExtension components: " + extension);
      }
      module.addComponent(extension);
    }
  }
}
