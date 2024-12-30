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
package org.sonar.server.common.user;

import java.util.function.Supplier;
import javax.inject.Inject;
import org.apache.commons.lang3.RandomStringUtils;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.UserDto;
import org.sonar.server.user.ExternalIdentity;

public class UserAnonymizer {
  private static final int LOGIN_RANDOM_LENGTH = 6;

  private final DbClient dbClient;
  private final Supplier<String> randomNameGenerator;

  @Inject
  public UserAnonymizer(DbClient dbClient) {
    this(dbClient, () -> "sq-removed-" + RandomStringUtils.secure().nextAlphanumeric(LOGIN_RANDOM_LENGTH));
  }

  public UserAnonymizer(DbClient dbClient, Supplier<String> randomNameGenerator) {
    this.dbClient = dbClient;
    this.randomNameGenerator = randomNameGenerator;
  }

  public void anonymize(DbSession session, UserDto user) {
    String newLogin = generateAnonymousLogin(session);
    user
      .setLogin(newLogin)
      .setName(newLogin)
      .setExternalIdentityProvider(ExternalIdentity.SQ_AUTHORITY)
      .setLocal(true)
      .setExternalId(newLogin)
      .setExternalLogin(newLogin);
  }

  private String generateAnonymousLogin(DbSession session) {
    for (int i = 0; i < 10; i++) {
      String candidate = randomNameGenerator.get();
      if (dbClient.userDao().selectByLogin(session, candidate) == null) {
        return candidate;
      }
    }
    throw new IllegalStateException("Could not find a unique login");
  }
}
