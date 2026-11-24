/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.notification;

import java.util.List;
import org.junit.Test;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.GroupMembershipDao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DbGroupNotificationMembershipProviderTest {

  private static final String USER_UUID = "user-uuid";
  private static final List<String> GROUP_UUIDS = List.of("group-1", "group-2");

  @Test
  public void selectGroupUuidsForUser_returnsExistingServerMemberships() {
    DbClient dbClient = mock(DbClient.class);
    DbSession session = mock(DbSession.class);
    GroupMembershipDao groupMembershipDao = mock(GroupMembershipDao.class);
    when(dbClient.openSession(false)).thenReturn(session);
    when(dbClient.groupMembershipDao()).thenReturn(groupMembershipDao);
    when(groupMembershipDao.selectGroupUuidsByUserUuid(session, USER_UUID)).thenReturn(GROUP_UUIDS);

    assertThat(new DbGroupNotificationMembershipProvider(dbClient).selectGroupUuidsForUser(USER_UUID, null))
      .containsExactlyInAnyOrderElementsOf(GROUP_UUIDS);
  }

  @Test
  public void selectGroupUuidsForUser_rejectsOrganizationScopedQuery() {
    DbClient dbClient = mock(DbClient.class);
    DbGroupNotificationMembershipProvider underTest = new DbGroupNotificationMembershipProvider(dbClient);

    assertThatThrownBy(() -> underTest.selectGroupUuidsForUser(USER_UUID, "organization-uuid"))
      .isInstanceOf(IllegalArgumentException.class);
  }
}
