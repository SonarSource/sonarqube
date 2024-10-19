/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.scanner.bootstrap;

import java.util.Collection;
import javax.annotation.Nullable;
import org.sonar.api.Plugin;
import org.sonar.api.SonarRuntime;
import org.sonar.api.config.Configuration;
import org.sonar.api.internal.PluginContextImpl;
import org.sonar.core.platform.ExtensionContainer;
import org.sonar.core.platform.PluginInfo;
import org.sonar.core.platform.PluginRepository;

public class ExtensionInstaller {

  private final SonarRuntime sonarRuntime;
  private final PluginRepository pluginRepository;
  private final Configuration bootConfiguration;

  public ExtensionInstaller(SonarRuntime sonarRuntime, PluginRepository pluginRepository, Configuration bootConfiguration) {
    this.sonarRuntime = sonarRuntime;
    this.pluginRepository = pluginRepository;
    this.bootConfiguration = bootConfiguration;
  }

  public ExtensionInstaller install(ExtensionContainer container, ExtensionMatcher matcher) {
    // core components
    for (Object o : BatchComponents.all()) {
      doInstall(container, matcher, null, o);
    }

    // plugin extensions
    installExtensionsForPlugins(container, matcher, pluginRepository.getPluginInfos());

    return this;
  }

  public void installExtensionsForPlugins(ExtensionContainer container, ExtensionMatcher matcher, Collection<PluginInfo> pluginInfos) {
    for (PluginInfo pluginInfo : pluginInfos) {
      Plugin plugin = pluginRepository.getPluginInstance(pluginInfo.getKey());
      Plugin.Context context = new PluginContextImpl.Builder()
        .setSonarRuntime(sonarRuntime)
        .setBootConfiguration(bootConfiguration)
        .build();

      plugin.define(context);
      for (Object extension : context.getExtensions()) {
        doInstall(container, matcher, pluginInfo, extension);
      }
    }
  }

  private static void doInstall(ExtensionContainer container, ExtensionMatcher matcher, @Nullable PluginInfo pluginInfo, Object extension) {
    if (matcher.accept(extension)) {
      container.addExtension(pluginInfo, extension);
    } else {
      container.declareExtension(pluginInfo, extension);
    }
  }

}
