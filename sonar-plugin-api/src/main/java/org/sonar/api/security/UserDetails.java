/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.api.security;

import com.google.common.base.Objects;

/**
 * This class is not intended to be subclassed by clients.
 *
 * @see ExternalUsersProvider
 * @since 2.14
 */
public final class UserDetails {

  private String name = "";
  private String email = "";
  private String userId = "";

  public UserDetails() {
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getEmail() {
    return email;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  /**
   * @since 5.2
   */
  public void setUserId(String userId) {
    this.userId = userId;
  }

  /**
   * @since 5.2
   */
  public String getUserId() {
    return userId;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("name", name)
      .add("email", email)
      .add("userId", userId)
      .toString();
  }
}
