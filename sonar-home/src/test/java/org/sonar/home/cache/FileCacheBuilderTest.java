/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
package org.sonar.home.cache;

import java.io.File;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class FileCacheBuilderTest {
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void setUserHome() throws Exception {
    File userHome = temp.newFolder();
    FileCache cache = new FileCacheBuilder(mock(Logger.class)).setUserHome(userHome).build();

    assertThat(cache.getDir()).isDirectory().exists();
    assertThat(cache.getDir().getName()).isEqualTo("cache");
    assertThat(cache.getDir().getParentFile()).isEqualTo(userHome);
  }

  @Test
  public void user_home_property_can_be_null() {
    FileCache cache = new FileCacheBuilder(mock(Logger.class)).setUserHome((String) null).build();

    // does not fail. It uses default path or env variable
    assertThat(cache.getDir()).isDirectory().exists();
    assertThat(cache.getDir().getName()).isEqualTo("cache");
  }

  @Test
  public void use_default_path_or_env_variable() {
    FileCache cache = new FileCacheBuilder(mock(Logger.class)).build();

    assertThat(cache.getDir()).isDirectory().exists();
    assertThat(cache.getDir().getName()).isEqualTo("cache");
  }
}
