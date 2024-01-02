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
package org.sonar.server.platform;

import org.junit.Test;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StatisticsSupportTest {

  private final DbClient dbClient = mock(DbClient.class, RETURNS_DEEP_STUBS);
  private final StatisticsSupport statisticsSupport = new StatisticsSupport(dbClient);

  @Test
  public void should_return_metric_from_liveMeasureDao() {
    when(dbClient.projectDao().getNclocSum(any(DbSession.class))).thenReturn(1800999L);

    long linesOfCode = statisticsSupport.getLinesOfCode();

    assertThat(linesOfCode).isEqualTo(1800999L);
  }

}