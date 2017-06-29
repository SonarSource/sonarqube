/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.sonar.server.es.FakeIndexDefinition.INDEX_TYPE_FAKE;

public class IndexerStartupTaskTest {

  private System2 system2 = System2.INSTANCE;
  private MapSettings settings = new MapSettings();

  @Rule
  public DbTester db = DbTester.create(system2);

  @Rule
  public EsTester es = new EsTester(new FakeIndexDefinition());

  @Test
  public void only_index_once() throws Exception {
    insertDocumentIntoIndex();

    StartupIndexer indexer1 = createIndexer();
    emulateStartup(indexer1);

    // do index on first run
    verify(indexer1).getIndexTypes();
    verify(indexer1).indexOnStartup(Mockito.eq(ImmutableSet.of(INDEX_TYPE_FAKE)));

    StartupIndexer indexer2 = createIndexer();
    emulateStartup(indexer2);

    // do not index on second run
    verify(indexer2).getIndexTypes();
    verifyNoMoreInteractions(indexer2);
  }

  @Test
  public void do_not_index_if_indexes_are_disabled() throws Exception {
    settings.setProperty("sonar.internal.es.disableIndexes", "true");

    insertDocumentIntoIndex();

    StartupIndexer indexer = createIndexer();
    emulateStartup(indexer);

    // do not index
    verifyNoMoreInteractions(indexer);
  }

  private void insertDocumentIntoIndex() {
    es.putDocuments(INDEX_TYPE_FAKE, new FakeDoc());
  }

  private StartupIndexer createIndexer() {
    StartupIndexer indexer = mock(StartupIndexer.class);
    doReturn(ImmutableSet.of(INDEX_TYPE_FAKE)).when(indexer).getIndexTypes();
    return indexer;
  }

  private void emulateStartup(StartupIndexer indexer) {
    new IndexerStartupTask(es.client(), settings.asConfig(), indexer).execute();
  }
}
