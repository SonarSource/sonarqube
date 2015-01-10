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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

public class FileCacheBuilderTest {
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void setUserHome() throws Exception {
    File userHome = temp.newFolder();
    FileCache cache = new FileCacheBuilder().setUserHome(userHome).build();

    assertThat(cache.getDir()).isDirectory().exists();
    assertThat(cache.getDir().getName()).isEqualTo("cache");
    assertThat(cache.getDir().getParentFile()).isEqualTo(userHome);
  }

  @Test
  public void user_home_property_can_be_null() throws Exception {
    FileCache cache = new FileCacheBuilder().setUserHome((String) null).build();

    // does not fail. It uses default path or env variable
    assertThat(cache.getDir()).isDirectory().exists();
    assertThat(cache.getDir().getName()).isEqualTo("cache");
  }

  @Test
  public void use_default_path_or_env_variable() throws Exception {
    FileCache cache = new FileCacheBuilder().build();

    assertThat(cache.getDir()).isDirectory().exists();
    assertThat(cache.getDir().getName()).isEqualTo("cache");
  }
}
