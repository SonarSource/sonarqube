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
package org.sonar.home.cache;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;

public class PersistentCacheBuilder {
  private static final long DEFAULT_EXPIRE_DURATION = TimeUnit.MILLISECONDS.convert(1L, TimeUnit.DAYS);
  private static final String DIR_NAME = "ws_cache";

  private Path cachePath;
  private final Logger logger;
  private String version;

  public PersistentCacheBuilder(Logger logger) {
    this.logger = logger;
  }

  public PersistentCache build() {
    if (cachePath == null) {
      setSonarHome(findHome());
    }

    return new PersistentCache(cachePath, DEFAULT_EXPIRE_DURATION, logger, version);
  }

  public PersistentCacheBuilder setVersion(String version) {
    this.version = version;
    return this;
  }

  public PersistentCacheBuilder setSonarHome(@Nullable Path p) {
    if (p != null) {
      this.cachePath = p.resolve(DIR_NAME);
    }
    return this;
  }

  private static Path findHome() {
    String home = System.getenv("SONAR_USER_HOME");

    if (home != null) {
      return Paths.get(home);
    }

    home = System.getProperty("user.home");
    return Paths.get(home, ".sonar");
  }
}
