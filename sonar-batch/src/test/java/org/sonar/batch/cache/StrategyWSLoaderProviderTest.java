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
package org.sonar.batch.cache;

import static org.assertj.core.api.Assertions.assertThat;

import org.sonar.batch.cache.WSLoader.LoadStrategy;

import org.junit.Before;
import org.mockito.MockitoAnnotations;
import org.sonar.batch.bootstrap.ServerClient;
import org.mockito.Mock;
import org.sonar.home.cache.PersistentCache;
import org.junit.Test;

public class StrategyWSLoaderProviderTest {
  @Mock
  private PersistentCache cache;

  @Mock
  private ServerClient client;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testStrategy() {
    StrategyWSLoaderProvider provider = new StrategyWSLoaderProvider(LoadStrategy.CACHE_FIRST);
    WSLoader wsLoader = provider.provide(cache, client);

    assertThat(wsLoader.getDefaultStrategy()).isEqualTo(LoadStrategy.CACHE_FIRST);
  }

  @Test
  public void testSingleton() {
    StrategyWSLoaderProvider provider = new StrategyWSLoaderProvider(LoadStrategy.CACHE_FIRST);
    WSLoader wsLoader = provider.provide(cache, client);

    assertThat(provider.provide(null, null)).isEqualTo(wsLoader);
  }
}
