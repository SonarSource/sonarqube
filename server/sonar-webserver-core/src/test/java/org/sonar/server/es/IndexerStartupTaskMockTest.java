/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.es;

import co.elastic.clients.elasticsearch._types.HealthStatus;
import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.server.es.metadata.MetadataIndex;

import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class IndexerStartupTaskMockTest {

  private final EsClient esClient = mock(EsClient.class);
  private final MapSettings settings = new MapSettings();
  private final MetadataIndex metadataIndex = mock(MetadataIndex.class);
  private final StartupIndexer indexer = mock(StartupIndexer.class);
  private final IndexType indexType = mock(IndexType.class, RETURNS_DEEP_STUBS);
  private final IndexerStartupTask underTest = new IndexerStartupTask(esClient, settings.asConfig(), metadataIndex, indexer);

  @Test
  public void synchronous_indexing_waits_for_cluster_yellow_and_marks_initialized() {
    when(indexer.getType()).thenReturn(StartupIndexer.Type.SYNCHRONOUS);
    doReturn(ImmutableSet.of(indexType)).when(indexer).getIndexTypes();
    when(metadataIndex.getInitialized(indexType)).thenReturn(false);

    underTest.execute();

    verify(esClient).waitForStatusV2(HealthStatus.Yellow);
    verify(indexer).indexOnStartup(ImmutableSet.of(indexType));
    verify(metadataIndex).setInitialized(indexType, true);
  }

  @Test
  public void asynchronous_indexing_also_waits_for_cluster_yellow() {
    when(indexer.getType()).thenReturn(StartupIndexer.Type.ASYNCHRONOUS);
    doReturn(ImmutableSet.of(indexType)).when(indexer).getIndexTypes();
    when(metadataIndex.getInitialized(indexType)).thenReturn(false);

    underTest.execute();

    verify(esClient).waitForStatusV2(HealthStatus.Yellow);
    verify(indexer).triggerAsyncIndexOnStartup(ImmutableSet.of(indexType));
    verify(metadataIndex).setInitialized(indexType, true);
  }

  @Test
  public void already_initialized_types_are_skipped() {
    when(indexer.getType()).thenReturn(StartupIndexer.Type.SYNCHRONOUS);
    doReturn(ImmutableSet.of(indexType)).when(indexer).getIndexTypes();
    when(metadataIndex.getInitialized(indexType)).thenReturn(true);

    underTest.execute();

    verify(esClient, never()).waitForStatusV2(HealthStatus.Yellow);
    verify(indexer, never()).indexOnStartup(ImmutableSet.of(indexType));
  }

  @Test
  public void indexing_is_skipped_when_disabled_by_config() {
    settings.setProperty("sonar.internal.es.disableIndexes", "true");

    underTest.execute();

    verify(indexer, never()).getIndexTypes();
    verify(esClient, never()).waitForStatusV2(HealthStatus.Yellow);
  }
}