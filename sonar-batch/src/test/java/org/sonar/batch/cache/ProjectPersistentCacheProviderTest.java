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

import org.sonar.api.batch.bootstrap.ProjectKey;

import org.sonar.batch.util.BatchUtils;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.batch.analysis.DefaultAnalysisMode;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.sonar.batch.bootstrap.GlobalProperties;
import org.sonar.batch.cache.ProjectPersistentCacheProvider;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import org.junit.Before;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Test;

public class ProjectPersistentCacheProviderTest {
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private ProjectPersistentCacheProvider provider = null;
  private GlobalProperties props = null;
  private DefaultAnalysisMode mode = null;
  private ProjectKey key = null;

  @Before
  public void prepare() {
    key = new ProjectKeySupplier("proj");
    props = new GlobalProperties(Collections.<String, String>emptyMap());
    mode = mock(DefaultAnalysisMode.class);
    provider = new ProjectPersistentCacheProvider();
  }

  @Test
  public void test_singleton() {
    assertThat(provider.provide(props, mode, key)).isEqualTo(provider.provide(props, mode, key));
  }

  @Test
  public void test_cache_dir() {
    assertThat(provider.provide(props, mode, key).getDirectory().toFile()).exists().isDirectory();
  }

  @Test
  public void test_home() {
    File f = temp.getRoot();
    props.properties().put("sonar.userHome", f.getAbsolutePath());
    Path expected = f.toPath()
      .resolve("ws_cache")
      .resolve("http%3A%2F%2Flocalhost%3A9000-" + BatchUtils.getServerVersion())
      .resolve("projects")
      .resolve("proj");

    assertThat(provider.provide(props, mode, key).getDirectory()).isEqualTo(expected);
  }
}
