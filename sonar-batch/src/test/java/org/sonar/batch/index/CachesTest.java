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
package org.sonar.batch.index;

import com.google.common.collect.ImmutableMap;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.CoreProperties;
import org.sonar.batch.bootstrap.BootstrapProperties;
import org.sonar.batch.bootstrap.TempFolderProvider;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class CachesTest {

  @ClassRule
  public static TemporaryFolder temp = new TemporaryFolder();

  public static Caches createCacheOnTemp(TemporaryFolder temp) {
    try {
      BootstrapProperties bootstrapProps = new BootstrapProperties(ImmutableMap.of(CoreProperties.WORKING_DIRECTORY, temp.newFolder().getAbsolutePath()));
      return new Caches(new TempFolderProvider().provide(bootstrapProps));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  Caches caches;

  @Before
  public void prepare() throws Exception {
    caches = createCacheOnTemp(temp);
  }

  @After
  public void stop() {
    caches.stop();
  }

  @Test
  public void should_stop_and_clean_temp_dir() throws Exception {
    File tempDir = caches.tempDir();
    assertThat(tempDir).isDirectory().exists();
    assertThat(caches.persistit()).isNotNull();
    assertThat(caches.persistit().isInitialized()).isTrue();

    caches.stop();

    assertThat(tempDir).doesNotExist();
    assertThat(caches.tempDir()).isNull();
    assertThat(caches.persistit()).isNull();
  }

  @Test
  public void should_create_cache() throws Exception {
    caches.start();
    Cache<Element> cache = caches.createCache("foo");
    assertThat(cache).isNotNull();
  }

  @Test
  public void should_not_create_cache_twice() throws Exception {
    caches.start();
    caches.<Element>createCache("foo");
    try {
      caches.<Element>createCache("foo");
      fail();
    } catch (IllegalStateException e) {
      // ok
    }
  }

  static class Element implements Serializable {

  }
}
