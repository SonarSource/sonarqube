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

import org.sonar.home.cache.PersistentCacheBuilder;
import org.picocontainer.injectors.ProviderAdapter;

import java.nio.file.Paths;

import org.sonar.home.cache.PersistentCache;

public class PersistentCacheProvider extends ProviderAdapter {
  private PersistentCache cache;

  public PersistentCache provide(BootstrapProperties props) {
    if (cache == null) {
      PersistentCacheBuilder builder = new PersistentCacheBuilder();

      String forceUpdate = props.property("sonar.forceUpdate");

      if ("true".equals(forceUpdate)) {
        builder.forceUpdate(true);
      }

      String home = props.property("sonar.userHome");
      if (home != null) {
        builder.setSonarHome(Paths.get(home));
      }

      cache = builder.build();
    }
    return cache;
  }
}
