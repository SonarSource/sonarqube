/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.server.startup;

import org.apache.commons.io.FileUtils;
import org.sonar.core.persistence.DryRunDatabaseFactory;
import org.sonar.server.platform.DefaultServerFileSystem;
import org.sonar.server.platform.PersistentSettings;

import java.io.File;
import java.util.Map;

/**
 * @since 4.0
 */
public class CleanDryRunCache {

  private DefaultServerFileSystem serverFileSystem;
  private PersistentSettings settings;

  public CleanDryRunCache(DefaultServerFileSystem serverFileSystem, PersistentSettings settings) {
    this.serverFileSystem = serverFileSystem;
    this.settings = settings;
  }

  private File getRootCacheLocation() {
    return new File(serverFileSystem.getTempDir(), "dryRun");
  }

  public void start() {
    clean();
  }

  public void clean() {
    // Delete folder where dryRun DB are stored
    FileUtils.deleteQuietly(getRootCacheLocation());
    // Delete all lastUpdate properties to force generation of new DB
    Map<String, String> properties = settings.getProperties();
    for (String propKey : properties.keySet()) {
      if (propKey.startsWith(DryRunDatabaseFactory.SONAR_DRY_RUN_CACHE_KEY_PREFIX)) {
        settings.deleteProperty(propKey);
      }
    }
  }
}
