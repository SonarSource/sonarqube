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
import com.google.common.io.Files;
import org.junit.rules.ExpectedException;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.Rule;
import org.junit.Before;
import org.sonar.batch.bootstrap.ServerClient;
import org.sonar.home.cache.PersistentCache;

public class DefaultProjectCacheStatusTest {
  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();

  @Rule
  public ExpectedException exception = ExpectedException.none();

  ProjectCacheStatus cacheStatus;
  PersistentCache cache;
  ServerClient client;

  @Before
  public void setUp() {
    client = mock(ServerClient.class);
    cache = mock(PersistentCache.class);
    when(cache.getDirectory()).thenReturn(tmp.getRoot().toPath());
    cacheStatus = new DefaultProjectCacheStatus(cache);
  }

  @Test
  public void errorSave() throws IOException {
    when(cache.getDirectory()).thenReturn(tmp.getRoot().toPath().resolve("unexistent_folder"));
    cacheStatus = new DefaultProjectCacheStatus(cache);

    exception.expect(IllegalStateException.class);
    exception.expectMessage("Failed to write cache sync status");
    cacheStatus.save();
  }

  @Test
  public void errorStatus() throws IOException {
    Files.write("trash".getBytes(StandardCharsets.UTF_8), new File(tmp.getRoot(), "cache-sync-status"));
    cacheStatus = new DefaultProjectCacheStatus(cache);

    exception.expect(IllegalStateException.class);
    exception.expectMessage("Failed to read cache sync status");
    cacheStatus.getSyncStatus();
  }

  @Test
  public void testSave() {
    cacheStatus.save();
    assertThat(cacheStatus.getSyncStatus()).isNotNull();
    assertThat(age(cacheStatus.getSyncStatus())).isLessThan(2000);
  }

  @Test
  public void testDelete() {
    cacheStatus.save();
    cacheStatus.delete();
    assertThat(cacheStatus.getSyncStatus()).isNull();
  }

  private long age(Date date) {
    return (new Date().getTime()) - date.getTime();
  }
}
