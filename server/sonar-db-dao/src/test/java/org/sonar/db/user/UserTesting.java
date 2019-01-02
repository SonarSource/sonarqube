/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import org.sonar.core.util.Uuids;

import static java.util.Collections.singletonList;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang.math.RandomUtils.nextBoolean;
import static org.apache.commons.lang.math.RandomUtils.nextInt;
import static org.apache.commons.lang.math.RandomUtils.nextLong;

public class UserTesting {

  public static UserDto newUserDto() {
    return new UserDto()
      .setId(nextInt())
      .setUuid(randomAlphanumeric(40))
      .setActive(true)
      .setLocal(nextBoolean())
      .setLogin(randomAlphanumeric(30))
      .setName(randomAlphanumeric(30))
      .setEmail(randomAlphanumeric(30))
      .setOnboarded(nextBoolean())
      .setScmAccounts(singletonList(randomAlphanumeric(40)))
      .setExternalId(randomAlphanumeric(40))
      .setExternalLogin(randomAlphanumeric(40))
      .setExternalIdentityProvider(randomAlphanumeric(40))
      .setSalt(randomAlphanumeric(40))
      .setCryptedPassword(randomAlphanumeric(40))
      .setCreatedAt(nextLong())
      .setUpdatedAt(nextLong());
  }

  public static UserDto newUserDto(String login, String name, @Nullable String email) {
    return newUserDto()
      .setName(name)
      .setEmail(email)
      .setLogin(login);
  }

  public static UserDto newLocalUser(String login, String name, @Nullable String email) {
    return newUserDto()
      .setLocal(true)
      .setName(name)
      .setEmail(email)
      .setLogin(login)
      .setExternalId(login)
      .setExternalLogin(login)
      .setExternalIdentityProvider("sonarqube");
  }

  public static UserDto newExternalUser(String login, String name, @Nullable String email) {
    return newUserDto()
      .setLocal(false)
      .setName(name)
      .setEmail(email)
      .setLogin(login)
      .setExternalId(randomAlphanumeric(40))
      .setExternalLogin(randomAlphanumeric(40))
      .setExternalIdentityProvider(randomAlphanumeric(40));
  }

  public static UserDto newDisabledUser() {
    return newUserDto()
      .setActive(false)
      // All these fields are reset when disabling a user
      .setScmAccounts((String) null)
      .setEmail(null)
      .setCryptedPassword(null)
      .setSalt(null);
  }

  public static UserPropertyDto newUserSettingDto(UserDto user) {
    return new UserPropertyDto()
      .setUuid(Uuids.createFast())
      .setUserUuid(user.getUuid())
      .setKey(randomAlphanumeric(20))
      .setValue(randomAlphanumeric(100));
  }
}
