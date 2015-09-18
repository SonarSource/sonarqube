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
package org.sonar.home.cache;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Mockito.mock;

public class PersistentCacheBuilderTest {
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void user_home_property_can_be_null() {
    PersistentCache cache = new PersistentCacheBuilder(mock(Logger.class)).setSonarHome(null).setAreaForGlobal("url", "0").build();
    assertTrue(Files.isDirectory(cache.getDirectory()));
    assertThat(cache.getDirectory()).endsWith(Paths.get("url-0", "global"));
  }

  @Test
  public void set_user_home() {
    PersistentCache cache = new PersistentCacheBuilder(mock(Logger.class)).setSonarHome(temp.getRoot().toPath()).setAreaForGlobal("url", "0").build();

    assertThat(cache.getDirectory()).isDirectory();
    assertThat(cache.getDirectory()).startsWith(temp.getRoot().toPath());
    assertTrue(Files.isDirectory(cache.getDirectory()));
  }

  @Test
  public void read_system_env() {
    assumeTrue(System.getenv("SONAR_USER_HOME") == null);

    System.setProperty("user.home", temp.getRoot().getAbsolutePath());

    PersistentCache cache = new PersistentCacheBuilder(mock(Logger.class)).setAreaForGlobal("url", "0").build();
    assertTrue(Files.isDirectory(cache.getDirectory()));
    assertThat(cache.getDirectory()).startsWith(temp.getRoot().toPath());
  }

  @Test
  public void directories() {
    System.setProperty("user.home", temp.getRoot().getAbsolutePath());

    PersistentCache cache = new PersistentCacheBuilder(mock(Logger.class)).setAreaForProject("url", "0", "proj").build();
    assertThat(cache.getDirectory()).endsWith(Paths.get(".sonar", "ws_cache", "url-0", "projects", "proj"));

    cache = new PersistentCacheBuilder(mock(Logger.class)).setAreaForLocalProject("url", "0").build();
    assertThat(cache.getDirectory()).endsWith(Paths.get(".sonar", "ws_cache", "url-0", "local"));

    cache = new PersistentCacheBuilder(mock(Logger.class)).setAreaForGlobal("url", "0").build();
    assertThat(cache.getDirectory()).endsWith(Paths.get(".sonar", "ws_cache", "url-0", "global"));
  }
}
