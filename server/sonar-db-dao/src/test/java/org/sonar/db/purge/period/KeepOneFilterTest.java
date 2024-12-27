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
package org.sonar.db.purge.period;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.event.Level;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.api.utils.DateUtils;
import org.sonar.db.purge.DbCleanerTestUtils;
import org.sonar.db.purge.PurgeableAnalysisDto;

import static org.assertj.core.api.Assertions.assertThat;

class KeepOneFilterTest {

  private static List<String> analysisUuids(List<PurgeableAnalysisDto> snapshotDtos) {
    return snapshotDtos.stream().map(PurgeableAnalysisDto::getAnalysisUuid).toList();
  }

  @RegisterExtension
  private final LogTesterJUnit5 logs = new LogTesterJUnit5();

  @Test
  void shouldOnlyOneSnapshotPerInterval() {
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
  void shouldKeepNonDeletableSnapshots() {
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
  void test_isDeletable() {
    assertThat(KeepOneFilter.isDeletable(DbCleanerTestUtils.createAnalysisWithDate("u1", "2011-05-01"))).isTrue();
    assertThat(KeepOneFilter.isDeletable(DbCleanerTestUtils.createAnalysisWithDate("u1", "2011-05-01").setLast(true))).isFalse();
    assertThat(KeepOneFilter.isDeletable(DbCleanerTestUtils.createAnalysisWithDate("u1", "2011-05-01").setHasEvents(true))).isFalse();
  }

  @Test
  void log_should_log_debug_message_when_debug_enabled() {
    KeepOneFilter filter = new KeepOneFilter(DateUtils.parseDate("2011-03-25"), DateUtils.parseDate("2011-08-25"), Calendar.MONTH, "month");
    logs.setLevel(Level.DEBUG);
    filter.log();
    assertThat(logs.logs(Level.DEBUG)).contains("-> Keep one snapshot per month between 2011-03-25 and 2011-08-25");
  }

  @Test
  void log_should_not_log_debug_message_when_debug_disabled() {
    KeepOneFilter filter = new KeepOneFilter(DateUtils.parseDate("2011-03-25"), DateUtils.parseDate("2011-08-25"), Calendar.MONTH, "month");
    logs.setLevel(Level.INFO);
    filter.log();
    assertThat(logs.logs()).isEmpty();
  }

}
