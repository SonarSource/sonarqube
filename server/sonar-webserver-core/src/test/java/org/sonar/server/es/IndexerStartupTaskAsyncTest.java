/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.server.es.metadata.MetadataIndex;
import org.sonar.server.es.metadata.MetadataIndexImpl;
import org.sonar.server.es.newindex.FakeIndexDefinition;

import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.server.es.newindex.FakeIndexDefinition.TYPE_FAKE;

public class IndexerStartupTaskAsyncTest {

  @Rule
  public EsTester es = EsTester.createCustom(new FakeIndexDefinition());

  private final MapSettings settings = new MapSettings();
  private final MetadataIndex metadataIndex = mock(MetadataIndexImpl.class);
  private final StartupIndexer indexer = mock(StartupIndexer.class);
  private final IndexerStartupTask underTest = new IndexerStartupTask(es.client(), settings.asConfig(), metadataIndex, indexer);

  @Before
  public void setUp() {
    when(indexer.getType()).thenReturn(StartupIndexer.Type.ASYNCHRONOUS);
    doReturn(ImmutableSet.of(TYPE_FAKE)).when(indexer).getIndexTypes();
  }

  @Test
  public void test(){
    doReturn(false).when(metadataIndex).getInitialized(TYPE_FAKE);

    underTest.execute();

    verify(indexer, times(1)).triggerAsyncIndexOnStartup(anySet());
  }

  @Test
  public void set_initialized_after_indexation() {
    doReturn(false).when(metadataIndex).getInitialized(TYPE_FAKE);

    underTest.execute();

    verify(metadataIndex).setInitialized(TYPE_FAKE, true);
  }

  @Test
  public void do_not_index_if_already_initialized() {
    doReturn(true).when(metadataIndex).getInitialized(TYPE_FAKE);

    underTest.execute();

    verify(indexer).getIndexTypes();
    verifyNoMoreInteractions(indexer);
  }

  @Test
  public void do_not_index_if_indexes_are_disabled() {
    settings.setProperty("sonar.internal.es.disableIndexes", "true");
    es.putDocuments(TYPE_FAKE, new FakeDoc());

    underTest.execute();

    // do not index
    verifyNoMoreInteractions(indexer);
  }
}
