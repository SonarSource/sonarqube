/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.user;

import java.util.List;

import javax.annotation.Nullable;

import org.sonar.core.platform.ComponentContainer;
import org.sonar.db.component.ResourceDao;
import org.sonar.db.user.AuthorizationDao;
import org.sonar.server.platform.Platform;
import org.sonar.server.ui.JRubyI18n;

import com.google.common.annotations.VisibleForTesting;

public class RubyUserSession {

  private static RubyUserSession instance;

  private static RubyUserSession getInstance() {
    if (instance == null) {
      instance = new RubyUserSession(Platform.getInstance());
    }
    return instance;
  }

  private final Platform platform;

  /**
   * Invoked by Ruby code - see application_controller.rb
   */
  public static void setSession(@Nullable Integer userId, @Nullable String login, @Nullable String name, @Nullable List<String> userGroups, @Nullable String localeRubyKey) {
    getInstance().setSessionImpl(userId, login, name, userGroups, localeRubyKey);
  }

  @VisibleForTesting
  RubyUserSession(Platform platform) {
    // Utility class
    this.platform = platform;
  }

  public void setSessionImpl(@Nullable Integer userId, @Nullable String login, @Nullable String name, @Nullable List<String> userGroups, @Nullable String localeRubyKey) {
    ComponentContainer container = platform.getContainer();
    ThreadLocalUserSession threadLocalUserSession = container.getComponentByType(ThreadLocalUserSession.class);

    UserSession session = new ServerUserSession(container.getComponentByType(AuthorizationDao.class),
      container.getComponentByType(ResourceDao.class))
      .setLogin(login)
      .setName(name)
      .setUserId(userId)
      .setUserGroups(userGroups != null ? userGroups.toArray(new String[userGroups.size()]) : null)
      .setLocale(JRubyI18n.toLocale(localeRubyKey));
    threadLocalUserSession.set(session);
  }

}
