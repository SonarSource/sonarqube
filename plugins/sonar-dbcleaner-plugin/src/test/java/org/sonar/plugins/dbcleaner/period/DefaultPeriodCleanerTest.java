/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.dbcleaner.period;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sonar.api.config.Settings;
import org.sonar.core.purge.PurgeDao;
import org.sonar.core.purge.PurgeSnapshotQuery;
import org.sonar.core.purge.PurgeableSnapshotDto;

import java.util.Arrays;
import java.util.Date;

import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;

public class DefaultPeriodCleanerTest {


  @Test
  public void doClean() {
    PurgeDao dao = mock(PurgeDao.class);
    when(dao.selectPurgeableSnapshots(123L)).thenReturn(Arrays.asList(
      new PurgeableSnapshotDto().setSnapshotId(999L).setDate(new Date())));
    Filter filter1 = newLazyFilter();
    Filter filter2 = newLazyFilter();

    DefaultPeriodCleaner cleaner = new DefaultPeriodCleaner(dao, mock(Settings.class));
    cleaner.doClean(123L, Arrays.asList(filter1, filter2));

    verify(filter1).log();
    verify(filter2).log();
    verify(dao, times(2)).deleteSnapshots(argThat(newRootSnapshotQuery()));
  }

  private BaseMatcher<PurgeSnapshotQuery> newRootSnapshotQuery() {
    return new BaseMatcher<PurgeSnapshotQuery>() {
      public boolean matches(Object o) {
        return ((PurgeSnapshotQuery) o).getRootSnapshotId() == 999L;
      }

      public void describeTo(Description description) {
      }
    };
  }

  private Filter newLazyFilter() {
    Filter filter1 = mock(Filter.class);
    when(filter1.filter(anyListOf(PurgeableSnapshotDto.class))).thenAnswer(new Answer<Object>() {
      public Object answer(InvocationOnMock invocation) throws Throwable {
        return invocation.getArguments()[0];
      }
    });
    return filter1;
  }
}
