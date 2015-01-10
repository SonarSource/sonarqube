/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DoPrivilegedTest {

  @Test
  public void should_allow_everything_in_privileged_block_only() {
    DoPrivileged.execute(new DoPrivileged.Task() {
      @Override
      protected void doPrivileged() {
        UserSession userSession = UserSession.get();
        assertThat(userSession.isLoggedIn()).isFalse();
        assertThat(userSession.hasGlobalPermission("any permission")).isTrue();
        assertThat(userSession.hasProjectPermission("any permission", "any project")).isTrue();
      }
    });

    assertThat(UserSession.get().isLoggedIn()).isFalse();
  }

  @Test
  public void should_lose_privileges_on_exception() {
    try {
      DoPrivileged.execute(new DoPrivileged.Task() {
        @Override
        protected void doPrivileged() {
          UserSession userSession = UserSession.get();
          assertThat(userSession.isLoggedIn()).isTrue();
          assertThat(userSession.hasGlobalPermission("any permission")).isTrue();
          assertThat(userSession.hasProjectPermission("any permission", "any project")).isTrue();

          throw new RuntimeException("Test to lose privileges");
        }
      });
    } catch(Throwable ignored) {
      assertThat(UserSession.get().isLoggedIn()).isFalse();
    }
  }

}
