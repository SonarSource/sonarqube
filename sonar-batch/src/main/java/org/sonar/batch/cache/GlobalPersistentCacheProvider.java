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
package org.sonar.batch.cache;

import org.apache.commons.lang.StringUtils;
import org.sonar.batch.bootstrap.Slf4jLogger;
import org.sonar.batch.util.BatchUtils;
import org.sonar.home.cache.PersistentCacheBuilder;

import java.nio.file.Paths;

import org.sonar.batch.bootstrap.GlobalProperties;
import org.sonar.home.cache.PersistentCache;
import org.picocontainer.injectors.ProviderAdapter;

public class GlobalPersistentCacheProvider extends ProviderAdapter {
  private PersistentCache cache;

  public PersistentCache provide(GlobalProperties props) {
    if (cache == null) {
      PersistentCacheBuilder builder = new PersistentCacheBuilder(new Slf4jLogger());
      String home = props.property("sonar.userHome");
      String serverUrl = getServerUrl(props);

      if (home != null) {
        builder.setSonarHome(Paths.get(home));
      }
      
      builder.setAreaForGlobal(serverUrl, BatchUtils.getServerVersion());
      cache = builder.build();
    }

    return cache;
  }

  private String getServerUrl(GlobalProperties props) {
    return StringUtils.removeEnd(StringUtils.defaultIfBlank(props.property("sonar.host.url"), "http://localhost:9000"), "/");
  }
}
