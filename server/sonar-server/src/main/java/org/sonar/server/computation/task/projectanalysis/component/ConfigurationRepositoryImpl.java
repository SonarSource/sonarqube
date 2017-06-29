/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.computation.task.projectanalysis.component;

import java.util.Collection;
import java.util.Map;
import org.sonar.api.config.Configuration;
import org.sonar.ce.settings.ProjectConfigurationFactory;
import org.sonar.server.util.cache.CacheLoader;
import org.sonar.server.util.cache.MemoryCache;

/**
 * Repository of component settings implementation based on a memory cache.
 */
public class ConfigurationRepositoryImpl implements ConfigurationRepository {

  private final ProjectConfigurationFactory projectConfigurationFactory;
  private final MemoryCache<String, Configuration> cache = new MemoryCache<>(new CacheLoader<String, Configuration>() {
    @Override
    public Configuration load(String key) {
      return projectConfigurationFactory.newProjectConfiguration(key);
    }

    @Override
    public Map<String, Configuration> loadAll(Collection<? extends String> keys) {
      throw new UnsupportedOperationException("loadAll is not supported");
    }
  });

  public ConfigurationRepositoryImpl(ProjectConfigurationFactory projectSettingsFactory) {
    this.projectConfigurationFactory = projectSettingsFactory;
  }

  @Override
  public Configuration getConfiguration(Component component) {
    return cache.get(component.getKey());
  }

}
