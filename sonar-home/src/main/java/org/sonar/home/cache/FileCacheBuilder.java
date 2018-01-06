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
package org.sonar.home.cache;

import java.io.File;

import javax.annotation.Nullable;

public class FileCacheBuilder {
  private final Logger logger;
  private File userHome;

  public FileCacheBuilder(Logger logger) {
    this.logger = logger;
  }

  public FileCacheBuilder setUserHome(File d) {
    this.userHome = d;
    return this;
  }

  public FileCacheBuilder setUserHome(@Nullable String path) {
    this.userHome = (path == null) ? null : new File(path);
    return this;
  }

  public FileCache build() {
    if (userHome == null) {
      userHome = findHome();
    }
    File cacheDir = new File(userHome, "cache");
    return FileCache.create(cacheDir, logger);
  }
  
  private static File findHome() {
    String path = System.getenv("SONAR_USER_HOME");
    if (path == null) {
      // Default
      path = System.getProperty("user.home") + File.separator + ".sonar";
    }
    return new File(path);
  }
}
