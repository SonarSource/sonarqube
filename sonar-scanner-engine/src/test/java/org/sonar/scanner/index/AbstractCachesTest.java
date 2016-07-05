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
package org.sonar.scanner.index;

import org.junit.After;

import org.junit.Before;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import com.google.common.collect.ImmutableMap;
import org.sonar.api.CoreProperties;
import org.sonar.scanner.bootstrap.GlobalProperties;
import org.sonar.scanner.bootstrap.GlobalTempFolderProvider;
import org.sonar.scanner.index.Caches;
import org.sonar.scanner.index.CachesManager;
import java.util.Map;

import org.junit.ClassRule;
import org.junit.rules.TemporaryFolder;

public abstract class AbstractCachesTest {
  @ClassRule
  public static TemporaryFolder temp = new TemporaryFolder();

  protected static CachesManager cachesManager;
  protected Caches caches;

  private static CachesManager createCacheOnTemp() {
    Map<String, String> props = ImmutableMap.of(CoreProperties.WORKING_DIRECTORY, temp.getRoot().getAbsolutePath(),
      CoreProperties.GLOBAL_WORKING_DIRECTORY, temp.getRoot().getAbsolutePath());

    return new CachesManager(new GlobalTempFolderProvider().provide(new GlobalProperties(props)));
  }

  @BeforeClass
  public static void startClass() {
    cachesManager = createCacheOnTemp();
    cachesManager.start();
  }

  @Before
  public void start() {
    caches = new Caches(cachesManager);
    caches.start();
  }

  @After
  public void stop() {
    if (caches != null) {
      caches.stop();
      caches = null;
    }
  }

  @AfterClass
  public static void stopClass() {
    if (cachesManager != null) {
      cachesManager.stop();
    }
  }
}
