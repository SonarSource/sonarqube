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

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.rules.TemporaryFolder;

public abstract class AbstractCachesTest {
  @ClassRule
  public static TemporaryFolder temp = new TemporaryFolder();

  protected static final ThreadLocal<CachesManager> cachesManagers = new ThreadLocal<CachesManager>() {
    @Override
    protected CachesManager initialValue() {
      CachesManager cachesManager = CachesManagerTest.createCacheOnTemp(temp);
      cachesManager.start();
      return cachesManager;
    }
  };

  protected CachesManager cachesManager;
  protected Caches caches;

  @Before
  public void start() {
    cachesManager = cachesManagers.get();
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
}
