/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.ce.queue;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.ce.CeActivityDto;
import org.sonar.db.ce.CeQueueDto;
import org.sonar.db.ce.CeTaskTypes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class PurgeCeActivitiesTest {

  private System2 system2 = spy(System2.INSTANCE);

  @Rule
  public DbTester dbTester = DbTester.create(system2);

  private PurgeCeActivities underTest = new PurgeCeActivities(dbTester.getDbClient(), system2);

  @Test
  public void delete_older_than_6_months() throws Exception {
    insertWithDate("VERY_OLD", 1_000_000_000_000L);
    insertWithDate("RECENT", 1_500_000_000_000L);
    when(system2.now()).thenReturn(1_500_000_000_100L);

    underTest.start();

    assertThat(dbTester.getDbClient().ceActivityDao().selectByUuid(dbTester.getSession(), "VERY_OLD").isPresent()).isFalse();
    assertThat(dbTester.getDbClient().ceActivityDao().selectByUuid(dbTester.getSession(), "RECENT").isPresent()).isTrue();
  }

  private void insertWithDate(String uuid, long date) {
    CeQueueDto queueDto = new CeQueueDto();
    queueDto.setUuid(uuid);
    queueDto.setTaskType(CeTaskTypes.REPORT);

    CeActivityDto dto = new CeActivityDto(queueDto);
    dto.setStatus(CeActivityDto.Status.SUCCESS);
    when(system2.now()).thenReturn(date);
    dbTester.getDbClient().ceActivityDao().insert(dbTester.getSession(), dto);
    dbTester.getSession().commit();
  }
}
