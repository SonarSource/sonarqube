/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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
package org.sonar.server.rules;

import org.sonar.api.Plugin;
import org.sonar.api.Plugins;
import org.sonar.api.rules.RulesRepository;
import org.sonar.server.platform.DefaultServerFileSystem;
import org.sonar.server.rules.DeprecatedRuleBridge;

import java.util.ArrayList;
import java.util.List;

public final class DeprecatedRuleBridges {

  private RulesRepository<?>[] repositories;
  private DefaultServerFileSystem fileSystem;
  private Plugins plugins;

  public DeprecatedRuleBridges(DefaultServerFileSystem fileSystem, Plugins plugins) {
    this.fileSystem = fileSystem;
    this.plugins = plugins;
    this.repositories = new RulesRepository[0];
  }

  public DeprecatedRuleBridges(DefaultServerFileSystem fileSystem, Plugins plugins, RulesRepository[] repositories) {
    this.fileSystem = fileSystem;
    this.plugins = plugins;
    this.repositories = repositories;
  }

  public List<DeprecatedRuleBridge> createBridges() {
    List<DeprecatedRuleBridge> bridges = new ArrayList<DeprecatedRuleBridge>();
    for (RulesRepository repository : repositories) {
      Plugin plugin = getPlugin(repository);
      bridges.add(new DeprecatedRuleBridge(plugin.getKey(), plugin.getName(), repository, fileSystem));
    }
    return bridges;
  }

  private Plugin getPlugin(RulesRepository repository) {
    return plugins.getPluginByExtension(repository);
  }

}
