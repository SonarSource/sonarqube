/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import org.picocontainer.injectors.ProviderAdapter;
import org.sonar.api.config.Configuration;
import org.sonar.home.cache.FileCache;
import org.sonar.home.cache.FileCacheBuilder;

public class FileCacheProvider extends ProviderAdapter {
  private FileCache cache;

  public FileCache provide(Configuration settings) {
    if (cache == null) {
      String home = settings.get("sonar.userHome").orElse(null);
      cache = new FileCacheBuilder(new Slf4jLogger()).setUserHome(home).build();
    }
    return cache;
  }
}
