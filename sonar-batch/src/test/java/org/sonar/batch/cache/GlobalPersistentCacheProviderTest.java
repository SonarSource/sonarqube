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

import org.sonar.batch.util.BatchUtils;

import org.sonar.home.cache.PersistentCache;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import org.sonar.batch.bootstrap.GlobalProperties;
import org.junit.Before;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

public class GlobalPersistentCacheProviderTest {
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  
  private GlobalPersistentCacheProvider provider;
  private GlobalProperties globalProperties;
  
  @Before
  public void setUp() {
    HashMap<String, String> map = new HashMap<String, String>();
    map.put("sonar.userHome", temp.getRoot().getAbsolutePath());
    globalProperties = new GlobalProperties(map);
    provider = new GlobalPersistentCacheProvider();
  }
  
  @Test
  public void test_path() {
    PersistentCache cache = provider.provide(globalProperties);
    assertThat(cache.getDirectory()).isEqualTo(temp.getRoot().toPath()
      .resolve("ws_cache")
      .resolve("http%3A%2F%2Flocalhost%3A9000-" + BatchUtils.getServerVersion())
      .resolve("global"));
  }
}
