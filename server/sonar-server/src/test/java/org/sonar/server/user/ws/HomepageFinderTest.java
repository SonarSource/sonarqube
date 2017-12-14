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

import org.junit.Ignore;
import org.junit.Test;
import org.sonar.db.user.UserDto;
import org.sonarqube.ws.Users;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonarqube.ws.Users.CurrentWsResponse.HomepageType.*;

public class HomepageFinderTest {

  @Test
  public void find_homepage_for_users_that_have_one() throws Exception {
    UserDto userDto = new UserDto().setHomepageType("PROJECT").setHomepageKey("pipo");

    HomepageFinder underTest = new HomepageFinder();

    Users.CurrentWsResponse.Homepage homepage = underTest.findFor(userDto);
    assertThat(homepage).isNotNull();
    assertThat(homepage.getType()).isEqualTo(PROJECT);
    assertThat(homepage.getValue()).isEqualTo("pipo");
  }

  @Test
  @Ignore // Default WIP implementation to be done in SONAR-10185
  public void find_default_homepage_when_users_does_not_have_one() throws Exception {

  }


}