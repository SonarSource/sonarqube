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
package org.sonar.plugins.dbcleaner.period;

import org.junit.Test;
import org.junit.internal.matchers.IsCollectionContaining;
import org.sonar.api.utils.DateUtils;
import org.sonar.core.purge.PurgeableSnapshotDto;
import org.sonar.plugins.dbcleaner.DbCleanerTestUtils;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.sonar.plugins.dbcleaner.DbCleanerTestUtils.createSnapshotWithDate;

public class KeepOneFilterTest {

  @Test
  public void shouldOnlyOneSnapshotPerInterval() {
    Filter filter = new KeepOneFilter(DateUtils.parseDate("2011-03-25"), DateUtils.parseDate("2011-08-25"), Calendar.MONTH, "month");

    List<PurgeableSnapshotDto> toDelete = filter.filter(Arrays.<PurgeableSnapshotDto>asList(
      createSnapshotWithDate(1L, "2010-01-01"), // out of scope -> keep
      createSnapshotWithDate(2L, "2011-05-01"), // may -> keep
      createSnapshotWithDate(3L, "2011-05-02"), // may -> to be deleted
      createSnapshotWithDate(4L, "2011-05-19"), // may -> to be deleted
      createSnapshotWithDate(5L, "2011-06-01"), // june -> keep
      createSnapshotWithDate(6L, "2012-01-01") // out of scope -> keep
    ));

    assertThat(toDelete.size(), is(2));
    assertThat(toDelete, IsCollectionContaining.hasItem(new DbCleanerTestUtils.SnapshotMatcher(3L)));
    assertThat(toDelete, IsCollectionContaining.hasItem(new DbCleanerTestUtils.SnapshotMatcher(4L)));
  }

  @Test
  public void shouldKeepNonDeletableSnapshots() {
    Filter filter = new KeepOneFilter(DateUtils.parseDate("2011-03-25"), DateUtils.parseDate("2011-08-25"), Calendar.MONTH, "month");

    List<PurgeableSnapshotDto> toDelete = filter.filter(Arrays.<PurgeableSnapshotDto>asList(
      createSnapshotWithDate(1L, "2011-05-01"), // to be deleted
      createSnapshotWithDate(2L, "2011-05-02").setLast(true),
      createSnapshotWithDate(3L, "2011-05-19").setHasEvents(true).setLast(false),
      createSnapshotWithDate(4L, "2011-05-23") // to be deleted
    ));

    assertThat(toDelete.size(), is(2));
    assertThat(toDelete, IsCollectionContaining.hasItem(new DbCleanerTestUtils.SnapshotMatcher(1L)));
    assertThat(toDelete, IsCollectionContaining.hasItem(new DbCleanerTestUtils.SnapshotMatcher(4L)));
  }

  @Test
  public void test_isDeletable() {
    assertThat(KeepOneFilter.isDeletable(createSnapshotWithDate(1L, "2011-05-01")), is(true));
    assertThat(KeepOneFilter.isDeletable(createSnapshotWithDate(1L, "2011-05-01").setLast(true)), is(false));
    assertThat(KeepOneFilter.isDeletable(createSnapshotWithDate(1L, "2011-05-01").setHasEvents(true)), is(false));
  }
}
