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
package org.sonar.batch.analysis;

import org.sonar.batch.cache.WSLoader;
import org.sonar.batch.cache.WSLoader.LoadStrategy;
import org.picocontainer.injectors.ProviderAdapter;

import java.util.Map;

import org.sonar.batch.bootstrap.ServerClient;
import org.sonar.home.cache.PersistentCache;
import org.sonar.api.batch.AnalysisMode;

public class AnalysisWSLoaderProvider extends ProviderAdapter {
  private WSLoader wsLoader;

  public WSLoader provide(AnalysisProperties props, AnalysisMode mode, PersistentCache cache, ServerClient client) {
    if (wsLoader == null) {
      // recreate cache directory if needed for this analysis
      cache.reconfigure();
      wsLoader = new WSLoader(getStrategy(props.properties(), mode), cache, client);
    }
    return wsLoader;
  }

  private static LoadStrategy getStrategy(Map<String, String> props, AnalysisMode mode) {
    if (mode.isIssues()) {
      return LoadStrategy.CACHE_ONLY;
    }

    return LoadStrategy.SERVER_ONLY;
  }
}
