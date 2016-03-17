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

import com.google.common.collect.ImmutableMap;
import org.assertj.core.util.Maps;
import org.junit.Test;
import org.sonar.api.batch.AnalysisMode;
import org.sonar.batch.bootstrap.BatchWsClient;
import org.sonar.batch.cache.WSLoader;
import org.sonar.batch.cache.WSLoader.LoadStrategy;
import org.sonar.home.cache.PersistentCache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AnalysisWSLoaderProviderTest {

  PersistentCache cache = mock(PersistentCache.class);
  BatchWsClient wsClient = mock(BatchWsClient.class);
  AnalysisMode mode = mock(AnalysisMode.class);

  AnalysisWSLoaderProvider underTest = new AnalysisWSLoaderProvider();

  @Test
  public void testDefault() {
    WSLoader loader = underTest.provide(mode, cache, wsClient, new AnalysisProperties(Maps.<String, String>newHashMap()));
    assertThat(loader.getDefaultStrategy()).isEqualTo(LoadStrategy.SERVER_ONLY);
  }

  @Test
  public void no_cache_by_default_in_issues_mode() {
    when(mode.isIssues()).thenReturn(true);
    WSLoader loader = underTest.provide(mode, cache, wsClient, new AnalysisProperties(Maps.<String, String>newHashMap()));
    assertThat(loader.getDefaultStrategy()).isEqualTo(LoadStrategy.SERVER_ONLY);
  }

  @Test
  public void enable_cache_in_issues_mode() {
    when(mode.isIssues()).thenReturn(true);
    WSLoader loader = underTest.provide(mode, cache, wsClient, new AnalysisProperties(ImmutableMap.of(AnalysisWSLoaderProvider.SONAR_USE_WS_CACHE, "true")));
    assertThat(loader.getDefaultStrategy()).isEqualTo(LoadStrategy.CACHE_ONLY);
  }
}
