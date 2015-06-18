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

import org.sonar.home.log.Slf4jLog;
import org.sonar.home.cache.PersistentCacheBuilder;
import org.picocontainer.injectors.ProviderAdapter;

import java.nio.file.Paths;
import java.util.Map;

import org.sonar.home.cache.PersistentCache;

public class PersistentCacheProvider extends ProviderAdapter {
  private PersistentCache cache;

  public PersistentCache provide(UserProperties props) {
    if (cache == null) {
      PersistentCacheBuilder builder = new PersistentCacheBuilder();

      builder.setLog(new Slf4jLog(PersistentCache.class));
      builder.forceUpdate(isForceUpdate(props.properties()));

      String home = props.property("sonar.userHome");
      if (home != null) {
        builder.setSonarHome(Paths.get(home));
      }

      cache = builder.build();
    }
    return cache;
  }

  public void reconfigure(UserProperties props) {
    if (cache != null) {
      cache.reconfigure(isForceUpdate(props.properties()));
    }
  }

  private static boolean isForceUpdate(Map<String, String> props) {
    String enableCache = props.get("sonar.enableHttpCache");
    return !"true".equals(enableCache);
  }
}
