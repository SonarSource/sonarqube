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

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.junit.Before;

import static org.mockito.Mockito.when;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Test;

public class PersistentCacheProviderTest {
  private PersistentCacheProvider provider = null;

  @Mock
  private BootstrapProperties props = null;

  @Before
  public void prepare() {
    MockitoAnnotations.initMocks(this);
    provider = new PersistentCacheProvider();
  }

  @Test
  public void test_singleton() {
    assertThat(provider.provide(props)).isEqualTo(provider.provide(props));
  }

  @Test
  public void test_cache_dir() {
    assertThat(provider.provide(props).getBaseDirectory().toFile()).exists().isDirectory();
  }

  @Test
  public void test_enableCache() {
    // normally force update (cache disabled)
    assertThat(provider.provide(props).isForceUpdate()).isTrue();

    when(props.property("sonar.enableHttpCache")).thenReturn("true");
    provider = new PersistentCacheProvider();
    assertThat(provider.provide(props).isForceUpdate()).isFalse();
  }
}
