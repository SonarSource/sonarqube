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
package org.sonar.db;

import java.util.Objects;

public final class EmailSubscriberDto {
  private String login;
  private boolean global;
  private String email;

  public String getLogin() {
    return login;
  }

  public boolean isGlobal() {
    return global;
  }

  public String getEmail() {
    return email;
  }

  public EmailSubscriberDto setLogin(String login) {
    this.login = login;
    return this;
  }

  public EmailSubscriberDto setGlobal(boolean global) {
    this.global = global;
    return this;
  }

  public EmailSubscriberDto setEmail(String email) {
    this.email = email;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EmailSubscriberDto that = (EmailSubscriberDto) o;
    return global == that.global &&
      Objects.equals(login, that.login) &&
      Objects.equals(email, that.email);
  }

  @Override
  public int hashCode() {
    return Objects.hash(login, global, email);
  }

  @Override
  public String toString() {
    return "EmailSubscriberDto{" +
      "login='" + login + '\'' +
      ", global=" + global +
      ", email='" + email + '\'' +
      '}';
  }

  public static EmailSubscriberDto create(String login, boolean global, String email) {
    return new EmailSubscriberDto()
      .setLogin(login)
      .setGlobal(global)
      .setEmail(email);
  }
}
