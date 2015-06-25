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
    PersistentCache cache = new PersistentCacheBuilder(mock(Logger.class)).setSonarHome(null).build();
    assertTrue(Files.isDirectory(cache.getBaseDirectory()));
    assertThat(cache.getBaseDirectory().getFileName().toString()).isEqualTo("ws_cache");
  }

  @Test
  public void set_user_home() {
    PersistentCache cache = new PersistentCacheBuilder(mock(Logger.class)).setSonarHome(temp.getRoot().toPath()).build();

    assertThat(cache.getBaseDirectory().getParent().toString()).isEqualTo(temp.getRoot().toPath().toString());
    assertTrue(Files.isDirectory(cache.getBaseDirectory()));
  }

  @Test
  public void read_system_env() {
    assumeTrue(System.getenv("SONAR_USER_HOME") == null);

    System.setProperty("user.home", temp.getRoot().getAbsolutePath());

    PersistentCache cache = new PersistentCacheBuilder(mock(Logger.class)).build();
    assertTrue(Files.isDirectory(cache.getBaseDirectory()));
    assertThat(cache.getBaseDirectory().getFileName().toString()).isEqualTo("ws_cache");

    String expectedSonarHome = temp.getRoot().toPath().resolve(".sonar").toString();
    assertThat(cache.getBaseDirectory().getParent().toString()).isEqualTo(expectedSonarHome);
  }
}
