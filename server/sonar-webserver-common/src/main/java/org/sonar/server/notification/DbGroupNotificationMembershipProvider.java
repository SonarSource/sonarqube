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

import java.util.Set;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.jspecify.annotations.Nullable;
import com.sonarsource.groupnotifications.api.GroupNotificationMembershipProvider;

/** Server adapter reading the existing {@code groups_users} association. */
public class DbGroupNotificationMembershipProvider implements GroupNotificationMembershipProvider {

  private final DbClient dbClient;

  public DbGroupNotificationMembershipProvider(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  @Override
  public Set<String> selectGroupUuidsForUser(String userUuid, @Nullable String organizationUuid) {
    if (organizationUuid != null) {
      throw new IllegalArgumentException("Server group memberships are host-wide");
    }

    try (DbSession session = dbClient.openSession(false)) {
      return Set.copyOf(dbClient.groupMembershipDao().selectGroupUuidsByUserUuid(session, userUuid));
    }
  }
}
