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

package org.sonar.core.computation.dbcleaner.period;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import org.junit.Test;
import org.sonar.api.utils.DateUtils;
import org.sonar.core.purge.PurgeableSnapshotDto;
import org.sonar.core.computation.dbcleaner.DbCleanerTestUtils;

import javax.annotation.Nullable;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;

public class KeepOneFilterTest {

  private static List<Long> snapshotIds(List<PurgeableSnapshotDto> snapshotDtos) {
    return newArrayList(Iterables.transform(snapshotDtos, new Function<PurgeableSnapshotDto, Long>() {
      @Override
      public Long apply(@Nullable PurgeableSnapshotDto input) {
        return input != null ? input.getSnapshotId() : null;
      }
    }));
  }

  @Test
  public void shouldOnlyOneSnapshotPerInterval() {
    Filter filter = new KeepOneFilter(DateUtils.parseDate("2011-03-25"), DateUtils.parseDate("2011-08-25"), Calendar.MONTH, "month");

    List<PurgeableSnapshotDto> toDelete = filter.filter(Arrays.<PurgeableSnapshotDto>asList(
      DbCleanerTestUtils.createSnapshotWithDate(1L, "2010-01-01"), // out of scope -> keep
      DbCleanerTestUtils.createSnapshotWithDate(2L, "2011-05-01"), // may -> keep
      DbCleanerTestUtils.createSnapshotWithDate(3L, "2011-05-02"), // may -> to be deleted
      DbCleanerTestUtils.createSnapshotWithDate(4L, "2011-05-19"), // may -> to be deleted
      DbCleanerTestUtils.createSnapshotWithDate(5L, "2011-06-01"), // june -> keep
      DbCleanerTestUtils.createSnapshotWithDate(6L, "2012-01-01") // out of scope -> keep
      ));

    assertThat(toDelete).hasSize(2);

    List<Long> snapshotIds = snapshotIds(toDelete);
    assertThat(snapshotIds).contains(3L);
    assertThat(snapshotIds.contains(4L));
  }

  @Test
  public void shouldKeepNonDeletableSnapshots() {
    Filter filter = new KeepOneFilter(DateUtils.parseDate("2011-03-25"), DateUtils.parseDate("2011-08-25"), Calendar.MONTH, "month");

    List<PurgeableSnapshotDto> toDelete = filter.filter(Arrays.<PurgeableSnapshotDto>asList(
      DbCleanerTestUtils.createSnapshotWithDate(1L, "2011-05-01"), // to be deleted
      DbCleanerTestUtils.createSnapshotWithDate(2L, "2011-05-02").setLast(true),
      DbCleanerTestUtils.createSnapshotWithDate(3L, "2011-05-19").setHasEvents(true).setLast(false),
      DbCleanerTestUtils.createSnapshotWithDate(4L, "2011-05-23") // to be deleted
      ));

    assertThat(toDelete).hasSize(2);

    List<Long> snapshotIds = snapshotIds(toDelete);
    assertThat(snapshotIds).contains(1L);
    assertThat(snapshotIds.contains(4L));
  }

  @Test
  public void test_isDeletable() {
    assertThat(KeepOneFilter.isDeletable(DbCleanerTestUtils.createSnapshotWithDate(1L, "2011-05-01"))).isTrue();
    assertThat(KeepOneFilter.isDeletable(DbCleanerTestUtils.createSnapshotWithDate(1L, "2011-05-01").setLast(true))).isFalse();
    assertThat(KeepOneFilter.isDeletable(DbCleanerTestUtils.createSnapshotWithDate(1L, "2011-05-01").setHasEvents(true))).isFalse();
  }

}
