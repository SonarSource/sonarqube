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

import org.sonar.home.log.Log;
import org.sonar.home.log.StandardLog;

import javax.annotation.Nullable;

import java.io.File;

public class FileCacheBuilder {

  private File userHome;
  private Log log = new StandardLog();

  public FileCacheBuilder setUserHome(File d) {
    this.userHome = d;
    return this;
  }

  public FileCacheBuilder setLog(Log log) {
    this.log = log;
    return this;
  }

  public FileCacheBuilder setUserHome(@Nullable String path) {
    this.userHome = (path == null ? null : new File(path));
    return this;
  }

  public FileCache build() {
    if (userHome == null) {
      String path = System.getenv("SONAR_USER_HOME");
      if (path == null) {
        // Default
        path = System.getProperty("user.home") + File.separator + ".sonar";
      }
      userHome = new File(path);
    }
    File cacheDir = new File(userHome, "cache");
    return FileCache.create(cacheDir, log);
  }
}
