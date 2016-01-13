/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.batch.analysis;

import org.picocontainer.injectors.ProviderAdapter;
import org.sonar.api.batch.AnalysisMode;
import org.sonar.batch.bootstrap.BatchWsClient;
import org.sonar.batch.cache.WSLoader;
import org.sonar.batch.cache.WSLoader.LoadStrategy;
import org.sonar.home.cache.PersistentCache;

public class AnalysisWSLoaderProvider extends ProviderAdapter {
  static final String SONAR_USE_WS_CACHE = "sonar.useWsCache";
  private WSLoader wsLoader;

  public WSLoader provide(AnalysisMode mode, PersistentCache cache, BatchWsClient client, AnalysisProperties props) {
    if (wsLoader == null) {
      // recreate cache directory if needed for this analysis
      cache.reconfigure();
      wsLoader = new WSLoader(getStrategy(mode, props), cache, client);
    }
    return wsLoader;
  }

  private static LoadStrategy getStrategy(AnalysisMode mode, AnalysisProperties props) {
    if (mode.isIssues() && "true".equals(props.property(SONAR_USE_WS_CACHE))) {
      return LoadStrategy.CACHE_ONLY;
    }

    return LoadStrategy.SERVER_ONLY;
  }
}
