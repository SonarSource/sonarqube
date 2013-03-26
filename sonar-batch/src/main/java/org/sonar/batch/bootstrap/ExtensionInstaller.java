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

import org.sonar.api.CoreProperties;
import org.sonar.api.ExtensionProvider;
import org.sonar.api.Plugin;
import org.sonar.api.config.Settings;
import org.sonar.api.platform.ComponentContainer;
import org.sonar.api.platform.PluginMetadata;
import org.sonar.batch.bootstrapper.EnvironmentInformation;

import javax.annotation.Nullable;

import java.util.List;
import java.util.Map;

public class ExtensionInstaller {

  private final BatchPluginRepository pluginRepository;
  private final EnvironmentInformation env;
  private final Settings settings;

  public ExtensionInstaller(BatchPluginRepository pluginRepository, EnvironmentInformation env, Settings settings) {
    this.pluginRepository = pluginRepository;
    this.env = env;
    this.settings = settings;
  }

  public ExtensionInstaller install(ComponentContainer container, ExtensionMatcher matcher) {
    boolean dryRun = isDryRun();
    for (Map.Entry<PluginMetadata, Plugin> entry : pluginRepository.getPluginsByMetadata().entrySet()) {
      PluginMetadata metadata = entry.getKey();
      Plugin plugin = entry.getValue();
      for (Object extension : plugin.getExtensions()) {
        doInstall(container, matcher, metadata, dryRun, extension);
      }
    }
    List<ExtensionProvider> providers = container.getComponentsByType(ExtensionProvider.class);
    for (ExtensionProvider provider : providers) {
      Object object = provider.provide();
      if (object instanceof Iterable) {
        for (Object extension : (Iterable) object) {
          doInstall(container, matcher, null, dryRun, extension);
        }
      } else {
        doInstall(container, matcher, null, dryRun, object);
      }
    }
    return this;
  }

  private void doInstall(ComponentContainer container, ExtensionMatcher matcher, @Nullable PluginMetadata metadata, boolean dryRun, Object extension) {
    if (ExtensionUtils.supportsEnvironment(extension, env)
      && (!dryRun || ExtensionUtils.supportsDryRun(extension))
      && matcher.accept(extension)) {
      container.addExtension(metadata, extension);
    } else {
      container.declareExtension(metadata, extension);
    }
  }

  private boolean isDryRun() {
    return settings.getBoolean(CoreProperties.DRY_RUN);
  }
}
