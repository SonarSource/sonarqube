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
package org.sonar.server.user;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class ExternalIdentityTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void create_external_identity() {
    ExternalIdentity externalIdentity = new ExternalIdentity("github", "login");
    assertThat(externalIdentity.getId()).isEqualTo("login");
    assertThat(externalIdentity.getProvider()).isEqualTo("github");
  }

  @Test
  public void fail_with_NPE_when_identity_provider_is_null() {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("Identity provider cannot be null");

    new ExternalIdentity(null, "login");
  }

  @Test
  public void fail_with_NPE_when_identity_id_is_null() {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("Identity id cannot be null");

    new ExternalIdentity("github", null);
  }

}
