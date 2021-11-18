/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
package org.sonar.auth.bitbucket;

import javax.annotation.Nullable;
import org.sonar.api.server.ServerSide;
import org.sonar.api.server.authentication.UserIdentity;

@ServerSide
public class UserIdentityFactory {

  public UserIdentity create(GsonUser gsonUser, @Nullable GsonEmails gsonEmails) {
    UserIdentity.Builder builder = UserIdentity.builder()
      .setProviderId(gsonUser.getUuid())
      .setProviderLogin(gsonUser.getUsername())
      .setName(generateName(gsonUser));
    if (gsonEmails != null) {
      builder.setEmail(gsonEmails.extractPrimaryEmail());
    }
    return builder.build();
  }

  private static String generateName(GsonUser gson) {
    String name = gson.getDisplayName();
    return name == null || name.isEmpty() ? gson.getUsername() : name;
  }

}
