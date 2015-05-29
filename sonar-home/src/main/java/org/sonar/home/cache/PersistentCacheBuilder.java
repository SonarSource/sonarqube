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

import org.sonar.home.log.StandardLog;

import org.sonar.home.log.Log;

import javax.annotation.Nullable;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

public class PersistentCacheBuilder {
  private boolean forceUpdate = false;
  private Path cachePath = null;
  private Log log = new StandardLog();
  private String name = "ws_cache";

  public PersistentCache build() {
    if (cachePath == null) {
      setSonarHome(findHome());
    }

    return new PersistentCache(cachePath, TimeUnit.MILLISECONDS.convert(1L, TimeUnit.DAYS), log, forceUpdate);
  }

  public PersistentCacheBuilder setLog(Log log) {
    this.log = log;
    return this;
  }

  public PersistentCacheBuilder setSonarHome(@Nullable Path p) {
    if (p != null) {
      this.cachePath = p.resolve(name);
    }
    return this;
  }

  public PersistentCacheBuilder forceUpdate(boolean update) {
    this.forceUpdate = update;
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
