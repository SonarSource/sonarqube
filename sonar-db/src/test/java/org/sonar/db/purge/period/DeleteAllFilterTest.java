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

package org.sonar.db.purge.period;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.sonar.api.utils.DateUtils;
import org.sonar.db.purge.DbCleanerTestUtils;
import org.sonar.db.purge.PurgeableSnapshotDto;

import static org.assertj.core.api.Assertions.assertThat;

public class DeleteAllFilterTest {

  @Test
  public void shouldDeleteAllSnapshotsPriorToDate() {
    Filter filter = new DeleteAllFilter(DateUtils.parseDate("2011-12-25"));

    List<PurgeableSnapshotDto> toDelete = filter.filter(Arrays.asList(
      DbCleanerTestUtils.createSnapshotWithDate(1L, "2010-01-01"),
      DbCleanerTestUtils.createSnapshotWithDate(2L, "2010-12-25"),
      DbCleanerTestUtils.createSnapshotWithDate(3L, "2012-01-01")
      ));

    assertThat(toDelete).extracting("snapshotId").containsOnly(1L, 2L);
  }
}
