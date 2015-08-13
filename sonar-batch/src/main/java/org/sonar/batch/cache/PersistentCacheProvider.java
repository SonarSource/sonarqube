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

import org.sonar.batch.bootstrap.Slf4jLogger;
import org.sonar.batch.bootstrap.UserProperties;

import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

import org.picocontainer.injectors.ProviderAdapter;
import org.sonar.home.cache.PersistentCache;
import org.sonar.home.cache.PersistentCacheBuilder;

public class PersistentCacheProvider extends ProviderAdapter {
  private static final Logger LOG = Loggers.get(PersistentCacheProvider.class);
  private PersistentCache cache;

  public PersistentCache provide(UserProperties props) {
    if (cache == null) {
      PersistentCacheBuilder builder = new PersistentCacheBuilder(new Slf4jLogger());

      String home = props.property("sonar.userHome");
      if (home != null) {
        builder.setSonarHome(Paths.get(home));
      }

      builder.setVersion(getVersion());
      cache = builder.build();
    }

    return cache;
  }

  private String getVersion() {
    InputStream is = this.getClass().getClassLoader().getResourceAsStream("sq-version.txt");
    if (is == null) {
      LOG.warn("Failed to get SQ version");
      return null;
    }
    try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
      return br.readLine();
    } catch (IOException e) {
      LOG.warn("Failed to get SQ version", e);
      return null;
    }
  }
}
