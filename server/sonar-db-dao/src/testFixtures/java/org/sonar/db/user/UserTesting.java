/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import java.security.SecureRandom;
import java.util.Collections;
import java.util.Locale;
import java.util.Random;
import javax.annotation.Nullable;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang.math.RandomUtils.nextBoolean;
import static org.apache.commons.lang.math.RandomUtils.nextInt;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;

public class UserTesting {

  private static final Random RANDOM = new SecureRandom();

  private static final String[] realisticIdentityProviders = {"github", "google", "microsoft"};

  public static UserDto newUserDto() {
    return new UserDto()
      .setUuid(randomAlphanumeric(40))
      .setActive(true)
      .setLocal(RANDOM.nextBoolean())
      .setLogin(randomAlphanumeric(30))
      .setName(randomAlphanumeric(30))
      .setEmail(randomAlphanumeric(30))
      .setScmAccounts(singletonList(randomAlphanumeric(40).toLowerCase(Locale.ENGLISH)))
      .setExternalId(randomAlphanumeric(40))
      .setExternalLogin(randomAlphanumeric(40))
      .setExternalIdentityProvider(randomAlphanumeric(40))
      .setSalt(randomAlphanumeric(40))
      .setCryptedPassword(randomAlphanumeric(40))
      .setCreatedAt(RANDOM.nextLong(Long.MAX_VALUE))
      .setUpdatedAt(RANDOM.nextLong(Long.MAX_VALUE));
  }

  public static UserDto newUserDtoRealistic() {
    long timeNow = System.currentTimeMillis();
    String loginAndAndId = randomAlphanumeric(30);
    String realisticIdentityProvider = realisticIdentityProviders[nextInt(realisticIdentityProviders.length)];
    boolean isExternal = nextBoolean();
    String externalIdAndLogin = isExternal ? loginAndAndId + "_" + realisticIdentityProvider : loginAndAndId;
    return new UserDto().setUuid(randomAlphanumeric(40))
      .setActive(nextBoolean())
      .setLocal(!isExternal)
      .setLogin(loginAndAndId)
      .setName(loginAndAndId + " " + loginAndAndId)
      .setEmail(loginAndAndId + "@" + loginAndAndId + ".com")
      .setScmAccounts(singletonList(loginAndAndId + "@github"))
      .setExternalId(externalIdAndLogin)
      .setExternalLogin(externalIdAndLogin)
      .setExternalIdentityProvider(isExternal ? realisticIdentityProvider : "sonarqube")
      .setSalt("ZLqSawNE/T7QNk+FLsSWiJ7D9qM=")
      .setHashMethod("PBKDF2")
      // password is "admin2"
      .setCryptedPassword("100000$arHk2+TbNYyFeUgAsDBz7O5M+W0Y3NKJGgvz0KsURHzfXaTXlLT0WYI3DWwXOgHLgyFidVJ4HF22h7zbJoaa8g==")
      .setCreatedAt(timeNow)
      .setUpdatedAt(timeNow)
      .setLastConnectionDate(nextBoolean() ? timeNow : null)
      .setResetPassword(nextBoolean() && nextBoolean() && nextBoolean())
      .setHomepageParameter(nextInt(10) + "")
      .setHomepageType("projects");
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
      .setScmAccounts(emptyList())
      .setEmail(null)
      .setCryptedPassword(null)
      .setSalt(null);
  }
}
