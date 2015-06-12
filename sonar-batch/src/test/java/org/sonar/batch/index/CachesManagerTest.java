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

import org.sonar.batch.bootstrap.TempFolderProvider;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.CoreProperties;
import org.sonar.batch.bootstrap.BootstrapProperties;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class CachesManagerTest {

  @ClassRule
  public static TemporaryFolder temp = new TemporaryFolder();

  public static CachesManager createCacheOnTemp(TemporaryFolder temp) {
    try {
      BootstrapProperties bootstrapProps = new BootstrapProperties(ImmutableMap.of(CoreProperties.WORKING_DIRECTORY, temp.newFolder().getAbsolutePath()));
      return new CachesManager(new TempFolderProvider().provide(bootstrapProps));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  CachesManager cachesMgr;

  @Before
  public void prepare() {
    cachesMgr = createCacheOnTemp(temp);
    cachesMgr.start();
  }

  @Test
  public void should_stop_and_clean_temp_dir() {
    File tempDir = cachesMgr.tempDir();
    assertThat(tempDir).isDirectory().exists();
    assertThat(cachesMgr.persistit()).isNotNull();
    assertThat(cachesMgr.persistit().isInitialized()).isTrue();

    cachesMgr.stop();

    assertThat(tempDir).doesNotExist();
    assertThat(cachesMgr.tempDir()).isNull();
    assertThat(cachesMgr.persistit()).isNull();
  }
}
