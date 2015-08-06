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
package org.sonar.batch.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.sonar.batch.bootstrap.WSLoader.LoadStrategy;
import org.junit.Test;
import org.junit.Before;
import org.sonar.home.cache.PersistentCache;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class GlobalWSLoaderProviderTest {
  @Mock
  private PersistentCache cache;

  @Mock
  private ServerClient client;

  @Mock
  private GlobalMode mode;

  private GlobalWSLoaderProvider loaderProvider;
  private Map<String, String> propMap;
  private BootstrapProperties props;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    loaderProvider = new GlobalWSLoaderProvider();
  }

  @Test
  public void testDefault() {
    propMap = new HashMap<>();
    props = new BootstrapProperties(propMap);

    WSLoader wsLoader = loaderProvider.provide(props, mode, cache, client);
    assertThat(wsLoader.getStrategy()).isEqualTo(LoadStrategy.SERVER_ONLY);
  }

}
