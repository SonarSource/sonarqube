/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.dbcleaner.period;

import org.apache.commons.lang.ObjectUtils;
import org.hamcrest.BaseMatcher;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sonar.api.config.Settings;
import org.sonar.core.purge.PurgeDao;
import org.sonar.core.purge.PurgeSnapshotQuery;
import org.sonar.core.purge.PurgeableSnapshotDto;

import java.util.Arrays;
import java.util.Date;

import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    verify(dao, times(2)).deleteSnapshots(argThat(newSnapshotIdQuery()));
  }

  private BaseMatcher<PurgeSnapshotQuery> newRootSnapshotQuery() {
    return new ArgumentMatcher<PurgeSnapshotQuery>() {
      @Override
      public boolean matches(Object o) {
        PurgeSnapshotQuery query = (PurgeSnapshotQuery) o;
        return ObjectUtils.equals(query.getRootSnapshotId(), 999L);
      }
    };
  }

  private BaseMatcher<PurgeSnapshotQuery> newSnapshotIdQuery() {
    return new ArgumentMatcher<PurgeSnapshotQuery>() {
      @Override
      public boolean matches(Object o) {
        PurgeSnapshotQuery query = (PurgeSnapshotQuery) o;
        return ObjectUtils.equals(query.getId(), 999L);
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
