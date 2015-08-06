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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;

import org.junit.Test;
import org.sonar.home.cache.Logger;
import org.junit.rules.TemporaryFolder;
import org.junit.Rule;
import org.junit.Before;
import org.sonar.batch.bootstrap.ServerClient;
import org.sonar.home.cache.PersistentCache;

public class ProjectCacheStatusTest {
  private static final String PROJ_KEY = "project1";
  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();

  ProjectCacheStatus cacheStatus;
  PersistentCache cache;
  ServerClient client;

  @Before
  public void setUp() {
    cache = new PersistentCache(tmp.getRoot().toPath(), Long.MAX_VALUE, mock(Logger.class), null);
    client = mock(ServerClient.class);
    when(client.getURL()).thenReturn("localhost");
    cacheStatus = new ProjectCacheStatus(cache, client);
  }

  @Test
  public void testSave() {
    cacheStatus.save(PROJ_KEY);
    assertThat(cacheStatus.getSyncStatus(PROJ_KEY)).isNotNull();
    assertThat(age(cacheStatus.getSyncStatus(PROJ_KEY))).isLessThan(2000);
    assertThat(cacheStatus.getSyncStatus(PROJ_KEY+"1")).isNull();
  }

  @Test
  public void testDelete() {
    cacheStatus.save(PROJ_KEY);
    cacheStatus.delete(PROJ_KEY);
    assertThat(cacheStatus.getSyncStatus(PROJ_KEY)).isNull();
  }

  private long age(Date date) {
    return (new Date().getTime()) - date.getTime();
  }
}
