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

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ExternalIdentityTest {


  @Test
  public void create_external_identity() {
    ExternalIdentity externalIdentity = new ExternalIdentity("github", "login", "ABCD");
    assertThat(externalIdentity.getLogin()).isEqualTo("login");
    assertThat(externalIdentity.getProvider()).isEqualTo("github");
    assertThat(externalIdentity.getId()).isEqualTo("ABCD");
  }

  @Test
  public void login_is_used_when_id_is_not_provided() {
    ExternalIdentity externalIdentity = new ExternalIdentity("github", "login", null);
    assertThat(externalIdentity.getLogin()).isEqualTo("login");
    assertThat(externalIdentity.getProvider()).isEqualTo("github");
    assertThat(externalIdentity.getId()).isEqualTo("login");
  }

  @Test
  public void fail_with_NPE_when_identity_provider_is_null() {
    assertThatThrownBy(() -> new ExternalIdentity(null, "login", "ABCD"))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("Identity provider cannot be null");
  }

  @Test
  public void fail_with_NPE_when_identity_login_is_null() {
    assertThatThrownBy(() -> new ExternalIdentity("github", null, "ABCD"))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("Identity login cannot be null");
  }

}
