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
package org.sonar.server.notification.ws;

public class NotificationsWsParameters {
  public static final String CONTROLLER = "api/notifications";
  public static final String ACTION_ADD = "add";
  public static final String ACTION_REMOVE = "remove";
  public static final String ACTION_LIST = "list";

  public static final String PARAM_PROJECT = "project";
  public static final String PARAM_CHANNEL = "channel";
  public static final String PARAM_TYPE = "type";
  public static final String PARAM_LOGIN = "login";

  private NotificationsWsParameters() {
    // prevent instantiation
  }
}
