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
package org.sonar.db.pushevent;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

public class PushEventDaoTest {

  private final TestSystem2 system2 = new TestSystem2().setNow(1L);

  @Rule
  public DbTester db = DbTester.create(system2);

  private final DbSession session = db.getSession();
  private final PushEventDao underTest = db.getDbClient().pushEventDao();

  @Test
  public void insert_events() {
    assertThat(db.countRowsOfTable(session, "push_events")).isZero();

    PushEventDto eventDtoFirst = new PushEventDto()
      .setUuid("test-uuid")
      .setProjectUuid("project-uuid")
      .setPayload("some-event".getBytes(UTF_8));

    PushEventDto eventDtoSecond = new PushEventDto()
      .setProjectUuid("project-uuid")
      .setPayload("some-event".getBytes(UTF_8));

    underTest.insert(session, eventDtoFirst);
    var generatedUuid = underTest.insert(session, eventDtoSecond);

    assertThat(db.countRowsOfTable(session, "push_events"))
      .isEqualTo(2);

    assertThat(underTest.selectByUuid(session, "test-uuid"))
      .extracting(PushEventDto::getUuid, PushEventDto::getProjectUuid, PushEventDto::getPayload, PushEventDto::getCreatedAt)
      .containsExactly(eventDtoFirst.getUuid(), eventDtoFirst.getProjectUuid(), eventDtoFirst.getPayload(), eventDtoFirst.getCreatedAt());

    assertThat(underTest.selectByUuid(session, generatedUuid.getUuid()))
      .extracting(PushEventDto::getUuid, PushEventDto::getProjectUuid, PushEventDto::getPayload, PushEventDto::getCreatedAt)
      .containsExactly(eventDtoSecond.getUuid(), eventDtoSecond.getProjectUuid(), eventDtoSecond.getPayload(), eventDtoSecond.getCreatedAt());

  }

}
