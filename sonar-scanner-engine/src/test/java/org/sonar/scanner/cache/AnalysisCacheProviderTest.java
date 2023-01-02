/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
package org.sonar.scanner.cache;

import org.junit.Test;
import org.sonar.api.batch.sensor.cache.ReadCache;
import org.sonar.scanner.scan.branch.BranchConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AnalysisCacheProviderTest {

  private final AnalysisCacheEnabled analysisCacheEnabled = mock(AnalysisCacheEnabled.class);
  private final AnalysisCacheMemoryStorage storage = mock(AnalysisCacheMemoryStorage.class);
  private final ReadCache readCache = mock(ReadCache.class);
  private final AnalysisCacheProvider cacheProvider = new AnalysisCacheProvider();
  private final BranchConfiguration branchConfiguration = mock(BranchConfiguration.class);

  @Test
  public void provide_noop_reader_cache_when_disable() {
    when(analysisCacheEnabled.isEnabled()).thenReturn(false);
    var cache = cacheProvider.provideReader(analysisCacheEnabled, storage);
    assertThat(cache).isInstanceOf(AnalysisCacheProvider.NoOpReadCache.class);
  }

  @Test
  public void provide_noop_writer_cache_when_disable() {
    when(analysisCacheEnabled.isEnabled()).thenReturn(false);
    var cache = cacheProvider.provideWriter(analysisCacheEnabled, readCache, branchConfiguration);
    assertThat(cache).isInstanceOf(AnalysisCacheProvider.NoOpWriteCache.class);
  }

  @Test
  public void provide_real_reader_cache_when_enable() {
    when(analysisCacheEnabled.isEnabled()).thenReturn(true);
    var cache = cacheProvider.provideReader(analysisCacheEnabled, storage);
    verify(storage).load();
    assertThat(cache).isInstanceOf(ReadCacheImpl.class);
  }

  @Test
  public void provide_real_writer_cache_when_enable() {
    when(analysisCacheEnabled.isEnabled()).thenReturn(true);
    var cache = cacheProvider.provideWriter(analysisCacheEnabled, readCache, branchConfiguration);
    assertThat(cache).isInstanceOf(WriteCacheImpl.class);
  }
}
