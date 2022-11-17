/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.server.platform.monitoring;

import org.junit.Test;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.measure.SumNclocDbQuery;
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.process.systeminfo.SystemInfoUtils.attribute;


public class StatisticsSystemSectionTest {

  private DbClient dbClient = mock(DbClient.class, RETURNS_DEEP_STUBS);

  private final StatisticsSystemSection statisticsSystemSection = new StatisticsSystemSection(dbClient);

  @Test
  public void shouldWriteProtobuf() {

    when(dbClient.liveMeasureDao().sumNclocOfBiggestBranch(any(DbSession.class), any(SumNclocDbQuery.class))).thenReturn(1800999L);

    ProtobufSystemInfo.Section protobuf = statisticsSystemSection.toProtobuf();
    long value = attribute(protobuf, "loc").getLongValue();

    assertThat(value).isEqualTo(1800999L);

  }


}