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
package org.sonar.core.permission;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

public class UserWithPermission {

  private String login;
  private String name;
  private String email;
  private boolean hasPermission;

  public String login() {
    return login;
  }

  public UserWithPermission setLogin(String login) {
    this.login = login;
    return this;
  }

  public String name() {
    return name;
  }

  public UserWithPermission setName(String name) {
    this.name = name;
    return this;
  }

  public String email() {
    return email;
  }

  public UserWithPermission setEmail(String email) {
    this.email = email;
    return this;
  }

  public boolean hasPermission() {
    return hasPermission;
  }

  public UserWithPermission hasPermission(boolean hasPermission) {
    this.hasPermission = hasPermission;
    return this;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UserWithPermission that = (UserWithPermission) o;
    return login.equals(that.login);
  }

  @Override
  public int hashCode() {
    return login.hashCode();
  }
}
