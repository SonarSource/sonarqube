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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

/**
 * Cache files will be placed in 3 areas:
 * <pre>
 *   &lt;sonar_home&gt;/ws_cache/&lt;server_url&gt;-&lt;version&gt;/projects/&lt;project&gt;/
 *   &lt;sonar_home&gt;/ws_cache/&lt;server_url&gt;-&lt;version&gt;/global/
 *   &lt;sonar_home&gt;/ws_cache/&lt;server_url&gt;-&lt;version&gt;/local/
 * </pre>
 */
public class PersistentCacheBuilder {
  private static final long DEFAULT_EXPIRE_DURATION = TimeUnit.MILLISECONDS.convert(1L, TimeUnit.DAYS);
  private static final String DIR_NAME = "ws_cache";

  private Path cacheBasePath;
  private Path relativePath;
  private final Logger logger;

  public PersistentCacheBuilder(Logger logger) {
    this.logger = logger;
  }

  public PersistentCacheBuilder setAreaForProject(String serverUrl, String serverVersion, String projectKey) {
    relativePath = Paths.get(sanitizeFilename(serverUrl + "-" + serverVersion))
      .resolve("projects")
      .resolve(sanitizeFilename(projectKey));
    return this;
  }
  
  public PersistentCacheBuilder setAreaForGlobal(String serverUrl, String serverVersion) {
    relativePath = Paths.get(sanitizeFilename(serverUrl + "-" + serverVersion))
      .resolve("global");
    return this;
  }
  
  public PersistentCacheBuilder setAreaForLocalProject(String serverUrl, String serverVersion) {
    relativePath = Paths.get(sanitizeFilename(serverUrl + "-" + serverVersion))
      .resolve("local");
    return this;
  }
  
  public PersistentCacheBuilder setSonarHome(@Nullable Path p) {
    if (p != null) {
      this.cacheBasePath = p.resolve(DIR_NAME);
    }
    return this;
  }

  public PersistentCache build() {
    if(relativePath == null) {
      throw new IllegalStateException("area must be set before building");
    }
    if (cacheBasePath == null) {
      setSonarHome(findHome());
    }
    Path cachePath = cacheBasePath.resolve(relativePath);
    DirectoryLock lock = new DirectoryLock(cacheBasePath, logger);
    return new PersistentCache(cachePath, DEFAULT_EXPIRE_DURATION, logger, lock);
  }

  private static Path findHome() {
    String home = System.getenv("SONAR_USER_HOME");

    if (home != null) {
      return Paths.get(home);
    }

    home = System.getProperty("user.home");
    return Paths.get(home, ".sonar");
  }

  private String sanitizeFilename(String name) {
    try {
      return URLEncoder.encode(name, StandardCharsets.UTF_8.name());
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException("Couldn't sanitize filename: " + name, e);
    }
  }
}
