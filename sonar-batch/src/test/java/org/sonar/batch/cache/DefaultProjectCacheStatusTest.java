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

import org.sonar.home.cache.PersistentCacheLoader;

import org.junit.internal.runners.statements.ExpectException;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.util.Date;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.any;
import org.junit.Test;
import org.sonar.home.cache.Logger;
import org.junit.rules.TemporaryFolder;
import org.junit.Rule;
import org.junit.Before;
import org.sonar.batch.bootstrap.ServerClient;
import org.sonar.home.cache.PersistentCache;

public class DefaultProjectCacheStatusTest {
  private static final String PROJ_KEY = "project1";
  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();
  
  @Rule
  public ExpectedException exception = ExpectedException.none();

  ProjectCacheStatus cacheStatus;
  PersistentCache cache;
  ServerClient client;

  @Before
  public void setUp() {
    cache = new PersistentCache(tmp.getRoot().toPath(), Long.MAX_VALUE, mock(Logger.class), null);
    client = mock(ServerClient.class);
    when(client.getURL()).thenReturn("localhost");
    cacheStatus = new DefaultProjectCacheStatus(cache, client);
  }

  @Test
  public void errorDelete() throws IOException {
    cache = mock(PersistentCache.class);
    doThrow(IOException.class).when(cache).put(anyString(), any(byte[].class));
    cacheStatus = new DefaultProjectCacheStatus(cache, client);

    exception.expect(IllegalStateException.class);
    exception.expectMessage("Failed to delete cache sync status");
    cacheStatus.delete(PROJ_KEY);
  }
  
  @Test
  public void errorSave() throws IOException {
    cache = mock(PersistentCache.class);
    doThrow(IOException.class).when(cache).put(anyString(), any(byte[].class));
    cacheStatus = new DefaultProjectCacheStatus(cache, client);

    exception.expect(IllegalStateException.class);
    exception.expectMessage("Failed to write cache sync status");
    cacheStatus.save(PROJ_KEY);
  }
  
  @Test
  public void errorStatus() throws IOException {
    cache = mock(PersistentCache.class);
    doThrow(IOException.class).when(cache).get(anyString(), any(PersistentCacheLoader.class));
    cacheStatus = new DefaultProjectCacheStatus(cache, client);

    exception.expect(IllegalStateException.class);
    exception.expectMessage("Failed to read cache sync status");
    cacheStatus.getSyncStatus(PROJ_KEY);
  }
  
  @Test
  public void testSave() {
    cacheStatus.save(PROJ_KEY);
    assertThat(cacheStatus.getSyncStatus(PROJ_KEY)).isNotNull();
    assertThat(age(cacheStatus.getSyncStatus(PROJ_KEY))).isLessThan(2000);
    assertThat(cacheStatus.getSyncStatus(PROJ_KEY + "1")).isNull();
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
