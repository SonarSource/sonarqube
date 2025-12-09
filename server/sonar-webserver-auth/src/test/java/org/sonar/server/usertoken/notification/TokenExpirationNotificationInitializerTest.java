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
package org.sonar.server.usertoken.notification;

import org.junit.Test;
import org.sonar.api.platform.Server;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class TokenExpirationNotificationInitializerTest {
  @Test
  public void when_scheduler_should_start_on_server_start() {
    var scheduler = mock(TokenExpirationNotificationScheduler.class);
    var underTest = new TokenExpirationNotificationInitializer(scheduler);
    underTest.onServerStart(mock(Server.class));
    verify(scheduler, times(1)).startScheduling();
  }

  @Test
  public void server_start_with_no_scheduler_still_work() {
    var underTest = new TokenExpirationNotificationInitializer(null);
    underTest.onServerStart(mock(Server.class));
    assertThatNoException();
  }

}
