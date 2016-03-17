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
package org.sonar.batch.cache;

import org.sonar.home.cache.PersistentCache;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

import static org.junit.Assert.*;
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
    HashMap<String, String> map = new HashMap<>();
    map.put("sonar.userHome", temp.getRoot().getAbsolutePath());
    globalProperties = new GlobalProperties(map);
    provider = new GlobalPersistentCacheProvider();
  }

  @Test
  public void test_path() {
    PersistentCache cache = provider.provide(globalProperties);
    assertThat(cache.getDirectory()).isEqualTo(temp.getRoot().toPath()
      .resolve("ws_cache")
      .resolve("http%3A%2F%2Flocalhost%3A9000")
      .resolve("global"));
  }

  @Test
  public void test_singleton() {
    assertTrue(provider.provide(globalProperties) == provider.provide(globalProperties));
  }

  @Test
  public void test_without_sonar_home() {
    globalProperties = new GlobalProperties(new HashMap<String, String>());
    PersistentCache cache = provider.provide(globalProperties);
    assertThat(cache.getDirectory().toAbsolutePath().toString()).startsWith(findHome().toAbsolutePath().toString());

  }

  private static Path findHome() {
    String home = System.getenv("SONAR_USER_HOME");

    if (home != null) {
      return Paths.get(home);
    }

    home = System.getProperty("user.home");
    return Paths.get(home, ".sonar");
  }
}
