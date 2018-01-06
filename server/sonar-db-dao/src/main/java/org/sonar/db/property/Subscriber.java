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
package org.sonar.db.property;

import java.util.Objects;

public class Subscriber {
  private String login;
  private boolean global;

  /**
   * Used by MyBatis
   */
  public Subscriber() {
  }

  public Subscriber(String login, boolean global) {
    this.login = login;
    this.global = global;
  }

  public String getLogin() {
    return login;
  }

  void setLogin(String login) {
    this.login = login;
  }

  public boolean isGlobal() {
    return global;
  }

  void setGlobal(boolean global) {
    this.global = global;
  }

  @Override
  public String toString() {
    return "Subscriber{" +
      "login='" + login + '\'' +
      ", global=" + global +
      '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Subscriber that = (Subscriber) o;
    return global == that.global && Objects.equals(login, that.login);
  }

  @Override
  public int hashCode() {
    return Objects.hash(login, global);
  }
}
