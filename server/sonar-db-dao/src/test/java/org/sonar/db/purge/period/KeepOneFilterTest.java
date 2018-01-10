/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;
import org.sonar.api.utils.DateUtils;
import org.sonar.db.purge.DbCleanerTestUtils;
import org.sonar.db.purge.PurgeableAnalysisDto;

import static org.assertj.core.api.Assertions.assertThat;

public class KeepOneFilterTest {

  private static List<String> analysisUuids(List<PurgeableAnalysisDto> snapshotDtos) {
    return snapshotDtos.stream().map(input -> input.getAnalysisUuid()).collect(Collectors.toList());
  }

  @Test
  public void shouldOnlyOneSnapshotPerInterval() {
    Filter filter = new KeepOneFilter(DateUtils.parseDate("2011-03-25"), DateUtils.parseDate("2011-08-25"), Calendar.MONTH, "month");

    List<PurgeableAnalysisDto> toDelete = filter.filter(Arrays.asList(
      DbCleanerTestUtils.createAnalysisWithDate("u1", "2010-01-01"), // out of scope -> keep
      DbCleanerTestUtils.createAnalysisWithDate("u2", "2011-05-01"), // may -> keep
      DbCleanerTestUtils.createAnalysisWithDate("u3", "2011-05-02"), // may -> to be deleted
      DbCleanerTestUtils.createAnalysisWithDate("u4", "2011-05-19"), // may -> to be deleted
      DbCleanerTestUtils.createAnalysisWithDate("u5", "2011-06-01"), // june -> keep
      DbCleanerTestUtils.createAnalysisWithDate("u6", "2012-01-01") // out of scope -> keep
      ));

    assertThat(toDelete).hasSize(2);

    assertThat(analysisUuids(toDelete)).containsOnly("u2", "u3");
  }

  @Test
  public void shouldKeepNonDeletableSnapshots() {
    Filter filter = new KeepOneFilter(DateUtils.parseDate("2011-03-25"), DateUtils.parseDate("2011-08-25"), Calendar.MONTH, "month");

    List<PurgeableAnalysisDto> toDelete = filter.filter(Arrays.asList(
      DbCleanerTestUtils.createAnalysisWithDate("u1", "2011-05-01"), // to be deleted
      DbCleanerTestUtils.createAnalysisWithDate("u2", "2011-05-02").setLast(true),
      DbCleanerTestUtils.createAnalysisWithDate("u3", "2011-05-19").setHasEvents(true).setLast(false),
      DbCleanerTestUtils.createAnalysisWithDate("u4", "2011-05-23") // to be deleted
      ));

    assertThat(toDelete).hasSize(2);

    assertThat(analysisUuids(toDelete)).contains("u1", "u4");
  }

  @Test
  public void test_isDeletable() {
    assertThat(KeepOneFilter.isDeletable(DbCleanerTestUtils.createAnalysisWithDate("u1", "2011-05-01"))).isTrue();
    assertThat(KeepOneFilter.isDeletable(DbCleanerTestUtils.createAnalysisWithDate("u1", "2011-05-01").setLast(true))).isFalse();
    assertThat(KeepOneFilter.isDeletable(DbCleanerTestUtils.createAnalysisWithDate("u1", "2011-05-01").setHasEvents(true))).isFalse();
  }

}
