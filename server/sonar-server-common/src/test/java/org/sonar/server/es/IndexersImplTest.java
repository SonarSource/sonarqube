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

import java.util.List;
import java.util.Set;
import org.junit.Test;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.es.EsQueueDto;
import org.sonar.db.project.ProjectDto;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.server.es.Indexers.BranchEvent.DELETION;
import static org.sonar.server.es.Indexers.EntityEvent.CREATION;

public class IndexersImplTest {

  @Test
  public void commitAndIndexOnEntityEvent_shouldCallIndexerWithSupportedItems() {
    List<EsQueueDto> items1 = List.of(EsQueueDto.create("fake/fake1", "P1"), EsQueueDto.create("fake/fake1", "P1"));
    List<EsQueueDto> items2 = List.of(EsQueueDto.create("fake/fake2", "P1"));

    EventIndexer indexer1 = mock(EventIndexer.class);
    EventIndexer indexer2 = mock(EventIndexer.class);

    DbSession dbSession = mock(DbSession.class);

    IndexersImpl underTest = new IndexersImpl(indexer1, indexer2);
    when(indexer1.prepareForRecoveryOnEntityEvent(dbSession, Set.of("P1"), CREATION)).thenReturn(items1);
    when(indexer2.prepareForRecoveryOnEntityEvent(dbSession, Set.of("P1"), CREATION)).thenReturn(items2);

    underTest.commitAndIndexOnEntityEvent(dbSession, Set.of("P1"), CREATION);

    verify(indexer1).index(dbSession, items1);
    verify(indexer2).index(dbSession, items2);
  }

  @Test
  public void commitAndIndexOnBranchEvent_shouldCallIndexerWithSupportedItems() {
    List<EsQueueDto> items1 = List.of(EsQueueDto.create("fake/fake1", "P1"), EsQueueDto.create("fake/fake1", "P1"));
    List<EsQueueDto> items2 = List.of(EsQueueDto.create("fake/fake2", "P1"));

    EventIndexer indexer1 = mock(EventIndexer.class);
    EventIndexer indexer2 = mock(EventIndexer.class);

    DbSession dbSession = mock(DbSession.class);

    IndexersImpl underTest = new IndexersImpl(indexer1, indexer2);
    when(indexer1.prepareForRecoveryOnBranchEvent(dbSession, Set.of("P1"), DELETION)).thenReturn(items1);
    when(indexer2.prepareForRecoveryOnBranchEvent(dbSession, Set.of("P1"), DELETION)).thenReturn(items2);

    underTest.commitAndIndexOnBranchEvent(dbSession, Set.of("P1"), DELETION);

    verify(indexer1).index(dbSession, items1);
    verify(indexer2).index(dbSession, items2);
  }

  @Test
  public void commitAndIndexEntities_shouldIndexAllUuids() {
    EventIndexer indexer1 = mock(EventIndexer.class);
    DbSession dbSession = mock(DbSession.class);

    IndexersImpl underTest = new IndexersImpl(indexer1);

    ProjectDto p1 = ComponentTesting.newProjectDto();
    underTest.commitAndIndexEntities(dbSession, Set.of(p1), CREATION);

    verify(indexer1).prepareForRecoveryOnEntityEvent(dbSession, Set.of(p1.getUuid()), CREATION);
  }

  @Test
  public void commitAndIndexBranches_shouldIndexAllBranchUuids() {
    EventIndexer indexer1 = mock(EventIndexer.class);
    DbSession dbSession = mock(DbSession.class);

    IndexersImpl underTest = new IndexersImpl(indexer1);

    BranchDto b1 = new BranchDto().setUuid("b1");
    underTest.commitAndIndexBranches(dbSession, Set.of(b1), DELETION);
    verify(indexer1).prepareForRecoveryOnBranchEvent(dbSession, Set.of(b1.getUuid()), DELETION);
  }
}
