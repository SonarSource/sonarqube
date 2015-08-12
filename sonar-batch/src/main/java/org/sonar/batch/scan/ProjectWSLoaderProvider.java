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
package org.sonar.batch.scan;

import org.picocontainer.injectors.ProviderAdapter;

import java.util.Map;

import org.sonar.batch.bootstrap.AnalysisProperties;
import org.sonar.batch.bootstrap.ServerClient;
import org.sonar.batch.bootstrap.WSLoader;
import org.sonar.home.cache.PersistentCache;
import org.sonar.api.batch.AnalysisMode;
import org.sonar.batch.bootstrap.WSLoader.LoadStrategy;

public class ProjectWSLoaderProvider extends ProviderAdapter {
  private static final String OPTIMIZE_STRING_PROP = "sonar.optimizeForSpeed";
  private WSLoader wsLoader;

  public WSLoader provide(AnalysisProperties props, AnalysisMode mode, PersistentCache cache, ServerClient client) {
    if (wsLoader == null) {
      // recreate cache directory if needed for this analysis
      cache.reconfigure();
      wsLoader = new WSLoader(isCacheEnabled(props.properties(), mode), cache, client);
      wsLoader.setStrategy(getStrategy(props.properties(), mode));
    }
    return wsLoader;
  }

  private static LoadStrategy getStrategy(Map<String, String> props, AnalysisMode mode) {
    String optimizeForSpeed = props.get(OPTIMIZE_STRING_PROP);
    if (mode.isIssues() && "true".equals(optimizeForSpeed)) {
      return LoadStrategy.CACHE_FIRST;
    }

    return LoadStrategy.SERVER_FIRST;
  }

  private static boolean isCacheEnabled(Map<String, String> props, AnalysisMode mode) {
    return mode.isIssues();
  }
}
