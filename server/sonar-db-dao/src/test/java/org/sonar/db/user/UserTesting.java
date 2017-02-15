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
package org.sonar.db.user;

import javax.annotation.Nullable;

import static java.util.Collections.singletonList;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang.math.RandomUtils.nextInt;
import static org.apache.commons.lang.math.RandomUtils.nextLong;

public class UserTesting {

  public static UserDto newUserDto() {
    return newUserDto(randomAlphanumeric(30), randomAlphanumeric(30), randomAlphanumeric(30));
  }

  public static UserDto newUserDto(String login, String name, @Nullable String email) {
    return new UserDto()
      .setId(nextInt())
      .setActive(true)
      .setLocal(true)
      .setName(name)
      .setEmail(email)
      .setLogin(login)
      .setScmAccounts(singletonList(randomAlphanumeric(40)))
      .setExternalIdentity(login)
      .setExternalIdentityProvider("sonarqube")
      .setSalt(randomAlphanumeric(40))
      .setCryptedPassword(randomAlphanumeric(40))
      .setCreatedAt(nextLong())
      .setUpdatedAt(nextLong());
  }

  public static UserDto newLocalUser(String login, String name, @Nullable String email) {
    return new UserDto()
      .setId(nextInt())
      .setActive(true)
      .setLocal(true)
      .setName(name)
      .setEmail(email)
      .setLogin(login)
      .setScmAccounts(singletonList(randomAlphanumeric(40)))
      .setExternalIdentity(login)
      .setExternalIdentityProvider("sonarqube")
      .setSalt(randomAlphanumeric(40))
      .setCryptedPassword(randomAlphanumeric(40))
      .setCreatedAt(nextLong())
      .setUpdatedAt(nextLong());
  }

  public static UserDto newExternalUser(String login, String name, @Nullable String email) {
    return new UserDto()
      .setId(nextInt())
      .setActive(true)
      .setLocal(false)
      .setName(name)
      .setEmail(email)
      .setLogin(login)
      .setScmAccounts(singletonList(randomAlphanumeric(40)))
      .setExternalIdentity(randomAlphanumeric(40))
      .setExternalIdentityProvider(randomAlphanumeric(40))
      .setCreatedAt(nextLong())
      .setUpdatedAt(nextLong());
  }

  public static UserDto newDisabledUser(String login) {
    return new UserDto()
      .setId(nextInt())
      .setLogin(login)
      .setActive(false)
      .setCreatedAt(nextLong())
      .setUpdatedAt(nextLong());
  }
}
