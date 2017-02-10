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
import org.sonar.api.config.Settings;
import org.sonar.ce.settings.ProjectSettingsFactory;
import org.sonar.server.util.cache.CacheLoader;
import org.sonar.server.util.cache.MemoryCache;

/**
 * Repository of component settings implementation based on a memory cache.
 */
public class SettingsRepositoryImpl implements SettingsRepository {

  private final ProjectSettingsFactory projectSettingsFactory;
  private final MemoryCache<String, Settings> cache = new MemoryCache<>(new CacheLoader<String, Settings>() {
    @Override
    public Settings load(String key) {
      return projectSettingsFactory.newProjectSettings(key);
    }

    @Override
    public Map<String, Settings> loadAll(Collection<? extends String> keys) {
      throw new UnsupportedOperationException("loadAll is not supported");
    }
  });

  public SettingsRepositoryImpl(ProjectSettingsFactory projectSettingsFactory) {
    this.projectSettingsFactory = projectSettingsFactory;
  }

  @Override
  public Settings getSettings(Component component){
    return cache.get(component.getKey());
  }

}
