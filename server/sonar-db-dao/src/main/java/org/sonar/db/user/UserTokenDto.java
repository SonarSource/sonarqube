/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import static org.sonar.db.user.UserTokenValidator.checkTokenHash;

public class UserTokenDto {
  private String login;
  private String name;
  private String tokenHash;
  private Long createdAt;

  public String getLogin() {
    return login;
  }

  public UserTokenDto setLogin(String login) {
    this.login = login;
    return this;
  }

  public String getName() {
    return name;
  }

  public UserTokenDto setName(String name) {
    this.name = name;
    return this;
  }

  public String getTokenHash() {
    return tokenHash;
  }

  public UserTokenDto setTokenHash(String tokenHash) {
    this.tokenHash = checkTokenHash(tokenHash);
    return this;
  }

  public Long getCreatedAt() {
    return createdAt;
  }

  public UserTokenDto setCreatedAt(long createdAt) {
    this.createdAt = createdAt;
    return this;
  }
}
