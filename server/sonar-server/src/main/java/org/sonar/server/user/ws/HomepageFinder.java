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

import org.sonar.db.user.UserDto;
import org.sonarqube.ws.Users.CurrentWsResponse.Homepage;
import org.sonarqube.ws.Users.CurrentWsResponse.HomepageType;

import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.sonarqube.ws.Users.CurrentWsResponse.HomepageType.MY_PROJECTS;

public class HomepageFinder {

  public Homepage findFor(UserDto user) {
    if (homepageIsSetFor(user)) {
      return homepageOf(user);
    } else {
      return newDefaultHomepageOf();
    }
  }

  private Homepage homepageOf(UserDto user) {
    return Homepage.newBuilder()
      .setType(HomepageType.valueOf(user.getHomepageType()))
      .setValue(user.getHomepageKey())
      .build();
  }

  // Default WIP implementation to be done in SONAR-10185
  private Homepage newDefaultHomepageOf() {
    return Homepage.newBuilder()
      .setType(MY_PROJECTS)
      .build();
  }


  private boolean homepageIsSetFor(UserDto user) {
    return isNotBlank(user.getHomepageType());
  }

}
