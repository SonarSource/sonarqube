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
package org.sonar.core.user;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.sonar.api.user.User;

/**
 * @since 3.6
 */
public class DefaultUser implements User {
  private String login;
  private String name;
  private String email;
  private boolean active;

  @Override
  public String login() {
    return login;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  @CheckForNull
  public String email() {
    return email;
  }

  @Override
  public boolean active() {
    return active;
  }

  public DefaultUser setLogin(String login) {
    this.login = login;
    return this;
  }

  public DefaultUser setName(String name) {
    this.name = name;
    return this;
  }

  public DefaultUser setEmail(@Nullable String s) {
    this.email = StringUtils.defaultIfBlank(s, null);
    return this;
  }

  public DefaultUser setActive(boolean b) {
    this.active = b;
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
    DefaultUser that = (DefaultUser) o;
    return login.equals(that.login);
  }

  @Override
  public int hashCode() {
    return login.hashCode();
  }
}
