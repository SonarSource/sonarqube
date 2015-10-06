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

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sonar.batch.bootstrap.GlobalProperties;
import org.sonar.batch.bootstrap.ServerClient;
import org.sonar.batch.cache.WSLoader.LoadStrategy;
import org.sonar.home.cache.PersistentCache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class StrategyWSLoaderProviderTest {
  @Mock
  private PersistentCache cache;

  @Mock
  private ServerClient client;

  private GlobalProperties globalProps;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    globalProps = mock(GlobalProperties.class);
  }

  @Test
  public void testStrategy() {
    StrategyWSLoaderProvider provider = new StrategyWSLoaderProvider(LoadStrategy.CACHE_FIRST);
    WSLoader wsLoader = provider.provide(cache, client, globalProps);

    assertThat(wsLoader.getDefaultStrategy()).isEqualTo(LoadStrategy.CACHE_FIRST);
  }

  @Test
  public void testSingleton() {
    StrategyWSLoaderProvider provider = new StrategyWSLoaderProvider(LoadStrategy.CACHE_FIRST);
    WSLoader wsLoader = provider.provide(cache, client, globalProps);

    assertThat(provider.provide(null, null, null)).isEqualTo(wsLoader);
  }
}
