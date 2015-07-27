/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.batch.bootstrap;

import java.util.List;
import javax.annotation.Nullable;
import org.sonar.api.ExtensionProvider;
import org.sonar.api.SonarPlugin;
import org.sonar.batch.bootstrapper.EnvironmentInformation;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.core.platform.PluginInfo;
import org.sonar.core.platform.PluginRepository;

public class ExtensionInstaller {

  private final PluginRepository pluginRepository;
  private final EnvironmentInformation env;

  public ExtensionInstaller(PluginRepository pluginRepository, EnvironmentInformation env) {
    this.pluginRepository = pluginRepository;
    this.env = env;
  }

  public ExtensionInstaller install(ComponentContainer container, ExtensionMatcher matcher) {

    // core components
    for (Object o : BatchComponents.all()) {
      doInstall(container, matcher, null, o);
    }

    // plugin extensions
    for (PluginInfo pluginInfo : pluginRepository.getPluginInfos()) {
      SonarPlugin plugin = pluginRepository.getPluginInstance(pluginInfo.getKey());
      for (Object extension : plugin.getExtensions()) {
        doInstall(container, matcher, pluginInfo, extension);
      }
    }
    List<ExtensionProvider> providers = container.getComponentsByType(ExtensionProvider.class);
    for (ExtensionProvider provider : providers) {
      Object object = provider.provide();
      if (object instanceof Iterable) {
        for (Object extension : (Iterable) object) {
          doInstall(container, matcher, null, extension);
        }
      } else {
        doInstall(container, matcher, null, object);
      }
    }
    return this;
  }

  private void doInstall(ComponentContainer container, ExtensionMatcher matcher, @Nullable PluginInfo pluginInfo, Object extension) {
    if (ExtensionUtils.supportsEnvironment(extension, env)
      && matcher.accept(extension)) {
      container.addExtension(pluginInfo, extension);
    } else {
      container.declareExtension(pluginInfo, extension);
    }
  }

}
