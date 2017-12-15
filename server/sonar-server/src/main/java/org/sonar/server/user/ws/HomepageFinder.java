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
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.user.UserDto;
import org.sonarqube.ws.Users.CurrentWsResponse.Homepage;
import org.sonarqube.ws.Users.CurrentWsResponse.HomepageType;

import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.sonarqube.ws.Users.CurrentWsResponse.HomepageType.MY_PROJECTS;

public class HomepageFinder {

  private static final Logger LOG = Loggers.get(HomepageFinder.class);

  private final DbClient dbClient;

  public HomepageFinder(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  public Homepage findFor(DbSession dbSession, UserDto user) {

    try {

      if (homepageIsSetFor(user)) {
        return homepageOf(dbSession, user);
      }

    } catch (IllegalStateException e) {
      LOG.warn("Inconsistent homepage data for user %s", user.getId());
    }

    return defaultHomepageOf();
  }

  private Homepage homepageOf(DbSession dbSession, UserDto user) {
    return Homepage.newBuilder()
      .setType(HomepageType.valueOf(user.getHomepageType()))
      .setValue(getPublicKey(dbSession, user.getHomepageType(), user.getHomepageKey()))
      .build();
  }

  private String getPublicKey(DbSession dbSession, String homepageType, String homepageKey) {

    if (HomepageType.PROJECT.toString().equals(homepageType)) {
      return getProjectPublicKeyOf(dbSession, homepageKey);
    }

    if (HomepageType.ORGANIZATION.toString().equals(homepageType)) {
      return getOrganisationPublicKeyOf(dbSession, homepageKey);
    }

    return EMPTY;
  }

  private String getOrganisationPublicKeyOf(DbSession dbSession, String homepageKey) {
    Optional<OrganizationDto> dto = dbClient.organizationDao().selectByUuid(dbSession, homepageKey);
    if (dto.isPresent()) {
      return dto.get().getKey();
    } else {
      throw new IllegalStateException("No Organization found for homepage key " + homepageKey);
    }
  }

  private String getProjectPublicKeyOf(DbSession dbSession, String homepageKey) {
    com.google.common.base.Optional<ComponentDto> dto = dbClient.componentDao().selectByUuid(dbSession, homepageKey);
    if (dto.isPresent()) {
      return dto.get().getKey();
    } else {
      throw new IllegalStateException("No Project found for homepage key " + homepageKey);
    }
  }

  // Default WIP implementation to be done in SONAR-10185
  private Homepage defaultHomepageOf() {
    return Homepage.newBuilder()
      .setType(MY_PROJECTS)
      .build();
  }

  private boolean homepageIsSetFor(UserDto user) {
    return isNotBlank(user.getHomepageType());
  }

}
