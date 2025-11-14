/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.plugins;

import com.google.common.base.Strings;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.core.platform.PluginInfo;
import org.sonar.updatecenter.common.Version;

/**
 * Checks plugins dependency requirements
 * @param <T>
 */
public class PluginRequirementsValidator<T extends PluginInfo> {
  private static final Logger LOG = LoggerFactory.getLogger(PluginRequirementsValidator.class);

  private final Map<String, T> allPluginsByKeys;

  PluginRequirementsValidator(Map<String, T> allPluginsByKeys) {
    this.allPluginsByKeys = allPluginsByKeys;
  }

  /**
   * Utility method that removes the plugins that are not compatible with current environment.
   */
  public static <T extends PluginInfo> void unloadIncompatiblePlugins(Map<String, T> pluginsByKey) {
    // loop as long as the previous loop ignored some plugins. That allows to support dependencies
    // on many levels, for example D extends C, which extends B, which requires A. If A is not installed,
    // then B, C and D must be ignored. That's not possible to achieve this algorithm with a single iteration over plugins.
    var validator = new PluginRequirementsValidator<>(pluginsByKey);
    Set<String> removedKeys = new HashSet<>();
    do {
      removedKeys.clear();
      for (T plugin : pluginsByKey.values()) {
        if (!validator.isCompatible(plugin)) {
          removedKeys.add(plugin.getKey());
        }
      }
      for (String removedKey : removedKeys) {
        pluginsByKey.remove(removedKey);
      }
    } while (!removedKeys.isEmpty());
  }

  boolean isCompatible(T plugin) {
    if (!Strings.isNullOrEmpty(plugin.getBasePlugin()) && !allPluginsByKeys.containsKey(plugin.getBasePlugin())) {
      // it extends a plugin that is not installed
      LOG.warn("Plugin {} [{}] is ignored because its base plugin [{}] is not installed", plugin.getName(), plugin.getKey(), plugin.getBasePlugin());
      return false;
    }

    for (PluginInfo.RequiredPlugin requiredPlugin : plugin.getRequiredPlugins()) {
      PluginInfo installedRequirement = allPluginsByKeys.get(requiredPlugin.getKey());
      if (installedRequirement == null) {
        // it requires a plugin that is not installed
        LOG.warn("Plugin {} [{}] is ignored because the required plugin [{}] is not installed", plugin.getName(), plugin.getKey(), requiredPlugin.getKey());
        return false;
      }
      Version installedRequirementVersion = installedRequirement.getVersion();
      if (installedRequirementVersion != null && requiredPlugin.getMinimalVersion().compareToIgnoreQualifier(installedRequirementVersion) > 0) {
        // it requires a more recent version
        LOG.warn("Plugin {} [{}] is ignored because the version {} of required plugin [{}] is not installed", plugin.getName(), plugin.getKey(),
            requiredPlugin.getMinimalVersion(), requiredPlugin.getKey());
        return false;
      }
    }
    return true;
  }

}
