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
package org.sonar.api.security;

import com.google.common.base.Preconditions;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class ExternalUsersProviderTest {

  @Test
  public void doGetUserDetails() {
    ExternalUsersProvider provider = new ExternalUsersProvider() {
      @Override
      public UserDetails doGetUserDetails(Context context) {
        Preconditions.checkNotNull(context.getUsername());
        Preconditions.checkNotNull(context.getRequest());
        UserDetails user = new UserDetails();
        user.setName(context.getUsername());
        user.setEmail("foo@bar.com");
        return user;
      }
    };
    UserDetails user = provider.doGetUserDetails(new ExternalUsersProvider.Context("foo", mock(HttpServletRequest.class)));

    assertThat(user.getName()).isEqualTo("foo");
    assertThat(user.getEmail()).isEqualTo("foo@bar.com");
  }

  @Test
  public void doGetUserDetails_deprecated_api() {
    ExternalUsersProvider provider = new ExternalUsersProvider() {
      @Override
      public UserDetails doGetUserDetails(String username) {
        UserDetails user = new UserDetails();
        user.setName(username);
        user.setEmail("foo@bar.com");
        return user;
      }
    };
    UserDetails user = provider.doGetUserDetails(new ExternalUsersProvider.Context("foo", mock(HttpServletRequest.class)));

    assertThat(user.getName()).isEqualTo("foo");
    assertThat(user.getEmail()).isEqualTo("foo@bar.com");
  }
}
