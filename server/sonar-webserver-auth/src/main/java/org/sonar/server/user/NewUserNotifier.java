/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.user;

import org.slf4j.Logger;
import org.sonar.api.server.ServerSide;
import org.sonar.api.platform.NewUserHandler;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @since 3.2
 */
@ServerSide
public class NewUserNotifier {

  private static final Logger LOG = LoggerFactory.getLogger(NewUserNotifier.class);
  private final NewUserHandler[] handlers;

  @Autowired(required = false)
  public NewUserNotifier(NewUserHandler[] handlers) {
    this.handlers = handlers;
  }

  @Autowired(required = false)
  public NewUserNotifier() {
    this(new NewUserHandler[0]);
  }

  public void onNewUser(NewUserHandler.Context context) {
    LOG.debug("User created: {}. Notifying {} handlers...",context.getLogin(), NewUserHandler.class.getSimpleName() );
    for (NewUserHandler handler : handlers) {
      handler.doOnNewUser(context);
    }
  }
}
