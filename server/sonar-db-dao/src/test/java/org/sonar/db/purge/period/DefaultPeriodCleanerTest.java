/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.db.purge.period;

import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.purge.PurgeDao;
import org.sonar.db.purge.PurgeProfiler;
import org.sonar.db.purge.PurgeableAnalysisDto;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

class DefaultPeriodCleanerTest {

  @Test
  void doClean() {
    PurgeDao dao = mock(PurgeDao.class);
    DbSession session = mock(DbSession.class);
    when(dao.selectProcessedAnalysisByComponentUuid("uuid_123", session)).thenReturn(Arrays.asList(
      new PurgeableAnalysisDto().setAnalysisUuid("u999").setDate(System2.INSTANCE.now()),
      new PurgeableAnalysisDto().setAnalysisUuid("u456").setDate(System2.INSTANCE.now())
    ));
    Filter filter1 = newFirstSnapshotInListFilter();
    Filter filter2 = newFirstSnapshotInListFilter();

    PurgeProfiler profiler = new PurgeProfiler();
    DefaultPeriodCleaner cleaner = new DefaultPeriodCleaner(dao, profiler);
    cleaner.doClean("uuid_123", Arrays.asList(filter1, filter2), session);

    InOrder inOrder = Mockito.inOrder(dao, filter1, filter2);
    inOrder.verify(filter1).log();
    inOrder.verify(dao, times(1)).deleteAnalyses(session, profiler, ImmutableList.of("u999"));
    inOrder.verify(filter2).log();
    inOrder.verify(dao, times(1)).deleteAnalyses(session, profiler, ImmutableList.of("u456"));
    inOrder.verifyNoMoreInteractions();
  }

  private Filter newFirstSnapshotInListFilter() {
    Filter filter1 = mock(Filter.class);
    when(filter1.filter(anyList())).thenAnswer(invocation -> Collections.singletonList(((List) invocation.getArguments()[0]).iterator().next()));
    return filter1;
  }
}
