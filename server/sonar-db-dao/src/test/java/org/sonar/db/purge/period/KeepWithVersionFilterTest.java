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

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.event.Level;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.db.purge.DbCleanerTestUtils;
import org.sonar.db.purge.PurgeableAnalysisDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.utils.DateUtils.parseDate;

class KeepWithVersionFilterTest {

  @RegisterExtension
  private final LogTesterJUnit5 logs = new LogTesterJUnit5();

  @Test
  void keep_only_analyses_with_a_version() {
    Filter underTest = new KeepWithVersionFilter(parseDate("2015-10-18"));

    List<PurgeableAnalysisDto> result = underTest.filter(Arrays.asList(
      DbCleanerTestUtils.createAnalysisWithDate("u1", "2015-10-17").setVersion("V1"),
      DbCleanerTestUtils.createAnalysisWithDate("u2", "2015-10-17").setVersion(null),
      DbCleanerTestUtils.createAnalysisWithDate("u3", "2015-10-19").setVersion(null)));

    assertThat(result).extracting(PurgeableAnalysisDto::getAnalysisUuid).containsExactlyInAnyOrder("u2");
  }

  @Test
  void log_should_log_debug_message_when_debug_enabled() {
    KeepWithVersionFilter filter = new KeepWithVersionFilter(parseDate("2015-10-18"));
    logs.setLevel(Level.DEBUG);
    filter.log();
    assertThat(logs.logs(Level.DEBUG)).contains("-> Keep analyses with a version prior to 2015-10-18");
  }

  @Test
  void log_should_not_log_debug_message_when_debug_disabled() {
    KeepWithVersionFilter filter = new KeepWithVersionFilter(parseDate("2015-10-18"));
    logs.setLevel(Level.INFO);
    filter.log();
    assertThat(logs.logs()).isEmpty();
  }

}
