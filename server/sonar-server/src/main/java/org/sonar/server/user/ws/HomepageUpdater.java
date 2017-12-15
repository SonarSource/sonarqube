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
package org.sonar.server.user.ws;

import java.util.Optional;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.user.UserDto;

import static com.google.common.base.Preconditions.checkState;
import static org.sonarqube.ws.Users.CurrentWsResponse.HomepageType.ORGANIZATION;
import static org.sonarqube.ws.Users.CurrentWsResponse.HomepageType.PROJECT;

public class HomepageUpdater {

  private final DbClient dbClient;

  public HomepageUpdater(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  public void update(String userLogin, String type, String value) {

    String uuid = "";

    try (DbSession dbSession = dbClient.openSession(false)) {

      if (PROJECT.toString().equals(type)) {
        com.google.common.base.Optional<ComponentDto> dto = dbClient.componentDao().selectByKey(dbSession, value);
        if (dto.isPresent()) {
          uuid = dto.get().uuid();
        } else {
          throw new IllegalStateException("No Project found for homepage key " + uuid);
        }

      }

      if (ORGANIZATION.toString().equals(type)) {
        Optional<OrganizationDto> dto = dbClient.organizationDao().selectByKey(dbSession, value);
        if (dto.isPresent()) {
          uuid = dto.get().getUuid();
        } else {
          throw new IllegalStateException("No Organization found for homepage key " + uuid);
        }

      }

      UserDto userDto = dbClient.userDao().selectActiveUserByLogin(dbSession, userLogin);
      checkState(userDto != null, "User login '%s' cannot be found", userLogin);

      userDto.setHomepageType(type);
      userDto.setHomepageKey(uuid);

      dbClient.userDao().update(dbSession, userDto);
      dbSession.commit();
    }

  }
}
