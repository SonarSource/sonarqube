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
package org.sonar.server.computation;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.platform.Server;
import org.sonar.api.utils.internal.TestSystem2;
import org.sonar.db.DbTester;
import org.sonar.db.ce.CeActivityDto;
import org.sonar.db.ce.CeQueueDto;
import org.sonar.db.ce.CeTaskTypes;
import org.sonar.server.computation.log.CeLogging;
import org.sonar.server.computation.log.LogFileRef;
import org.sonar.server.computation.queue.PurgeCeActivities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class PurgeCeActivitiesTest {

  TestSystem2 system2 = new TestSystem2();

  @Rule
  public DbTester dbTester = DbTester.create(system2);
  CeLogging ceLogging = mock(CeLogging.class);
  PurgeCeActivities underTest = new PurgeCeActivities(dbTester.getDbClient(), system2, ceLogging);

  @Test
  public void delete_older_than_6_months() throws Exception {
    insertWithDate("VERY_OLD", 1_000_000_000_000L);
    insertWithDate("RECENT", 1_500_000_000_000L);
    system2.setNow(1_500_000_000_100L);

    underTest.onServerStart(mock(Server.class));

    assertThat(dbTester.getDbClient().ceActivityDao().selectByUuid(dbTester.getSession(), "VERY_OLD").isPresent()).isFalse();
    assertThat(dbTester.getDbClient().ceActivityDao().selectByUuid(dbTester.getSession(), "RECENT").isPresent()).isTrue();
    verify(ceLogging).deleteIfExists(new LogFileRef(CeTaskTypes.REPORT, "VERY_OLD", null));
    verify(ceLogging, never()).deleteIfExists(new LogFileRef(CeTaskTypes.REPORT, "RECENT", null));
  }

  private void insertWithDate(String uuid, long date) {
    CeQueueDto queueDto = new CeQueueDto();
    queueDto.setUuid(uuid);
    queueDto.setTaskType(CeTaskTypes.REPORT);

    CeActivityDto dto = new CeActivityDto(queueDto);
    dto.setStatus(CeActivityDto.Status.SUCCESS);
    system2.setNow(date);
    dbTester.getDbClient().ceActivityDao().insert(dbTester.getSession(), dto);
    dbTester.getSession().commit();
  }
}
