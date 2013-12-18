/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.server.user;

import org.sonar.server.ui.JRubyI18n;

import javax.annotation.Nullable;

public class RubyUserSession {
  /**
   * Invoked by Ruby code - see application_controller.rb
   */
  public static void setSession(@Nullable Integer userId, @Nullable String login, @Nullable String name, @Nullable String localeRubyKey) {
    UserSession session = new UserSession().setLogin(login).setName(name).setUserId(userId).setLocale(JRubyI18n.toLocale(localeRubyKey));
    UserSession.set(session);
  }

  private RubyUserSession() {
    // Utility class
  }

}
