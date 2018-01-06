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
import java.util.List;
import org.junit.Test;
import org.sonar.api.utils.DateUtils;
import org.sonar.db.purge.DbCleanerTestUtils;
import org.sonar.db.purge.PurgeableAnalysisDto;

import static org.assertj.core.api.Assertions.assertThat;

public class DeleteAllFilterTest {

  @Test
  public void shouldDeleteAllSnapshotsPriorToDate() {
    Filter filter = new DeleteAllFilter(DateUtils.parseDate("2011-12-25"));

    List<PurgeableAnalysisDto> toDelete = filter.filter(Arrays.asList(
      DbCleanerTestUtils.createAnalysisWithDate("u1", "2010-01-01"),
      DbCleanerTestUtils.createAnalysisWithDate("u2", "2010-12-25"),
      DbCleanerTestUtils.createAnalysisWithDate("u3", "2012-01-01")
      ));

    assertThat(toDelete).extracting("analysisUuid").containsOnly("u1", "u2");
  }
}
