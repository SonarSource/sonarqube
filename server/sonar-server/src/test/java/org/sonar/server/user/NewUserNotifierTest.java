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
package org.sonar.server.user;

import org.junit.Test;
import org.sonar.api.platform.NewUserHandler;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class NewUserNotifierTest {

  NewUserHandler.Context context = NewUserHandler.Context.builder().setLogin("marius").setName("Marius").build();

  @Test
  public void do_not_fail_if_no_handlers() {
    NewUserNotifier notifier = new NewUserNotifier();

    notifier.onNewUser(context);
  }

  @Test
  public void execute_handlers_on_new_user() {
    NewUserHandler handler1 = mock(NewUserHandler.class);
    NewUserHandler handler2 = mock(NewUserHandler.class);
    NewUserNotifier notifier = new NewUserNotifier(new NewUserHandler[]{handler1, handler2});


    notifier.onNewUser(context);

    verify(handler1).doOnNewUser(context);
    verify(handler2).doOnNewUser(context);
  }
}
