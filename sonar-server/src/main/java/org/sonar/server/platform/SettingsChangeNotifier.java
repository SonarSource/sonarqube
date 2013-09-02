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
package org.sonar.server.platform;

import com.google.common.annotations.VisibleForTesting;
import org.sonar.api.ServerComponent;
import org.sonar.api.config.SettingsChangeHandler;
import org.sonar.core.resource.ResourceDao;
import org.sonar.core.resource.ResourceDto;
import org.sonar.core.user.UserDao;
import org.sonar.core.user.UserDto;

import javax.annotation.Nullable;

public class SettingsChangeNotifier implements ServerComponent {

  @VisibleForTesting
  SettingsChangeHandler[] changeHandlers;
  private final ResourceDao resourceDao;
  private final UserDao userDao;

  public SettingsChangeNotifier(ResourceDao resourceDao, UserDao userDao, SettingsChangeHandler[] changeHandlers) {
    this.resourceDao = resourceDao;
    this.userDao = userDao;
    this.changeHandlers = changeHandlers;
  }

  public SettingsChangeNotifier(ResourceDao resourceDao, UserDao userDao) {
    this(resourceDao, userDao, new SettingsChangeHandler[0]);
  }

  public void onPropertyChange(String key, @Nullable String value, @Nullable Long componentId, @Nullable Long userId) {
    String resourceKey = null;
    if (componentId != null) {
      ResourceDto resource = resourceDao.getResource(componentId);
      resourceKey = resource != null ? resource.getKey() : null;
    }
    String userLogin = null;
    if (userId != null) {
      UserDto user = userDao.getUser(userId);
      userLogin = user != null ? user.getLogin() : null;
    }
    SettingsChangeHandler.SettingsChange change = SettingsChangeHandler.SettingsChange.create(key, value, resourceKey, userLogin);
    for (SettingsChangeHandler changeHandler : changeHandlers) {
      changeHandler.onChange(change);
    }
  }
}
