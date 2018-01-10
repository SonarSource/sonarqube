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
package org.sonar.server.user.ws;

import java.io.IOException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.authentication.Display;
import org.sonar.api.server.authentication.IdentityProvider;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.authentication.IdentityProviderRepositoryRule;
import org.sonar.server.authentication.TestIdentityProvider;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.test.JsonAssert.assertJson;

public class IdentityProvidersActionTest {
  static IdentityProvider GITHUB = new TestIdentityProvider()
    .setKey("github")
    .setName("Github")
    .setDisplay(Display.builder()
      .setIconPath("/static/authgithub/github.svg")
      .setBackgroundColor("#444444")
      .build())
    .setEnabled(true);

  static IdentityProvider BITBUCKET = new TestIdentityProvider()
    .setKey("bitbucket")
    .setName("Bitbucket")
    .setDisplay(Display.builder()
      .setIconPath("/static/authbitbucket/bitbucket.svg")
      .setBackgroundColor("#205081")
      .build())
    .setEnabled(true);

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public IdentityProviderRepositoryRule identityProviderRepository = new IdentityProviderRepositoryRule()
    .addIdentityProvider(GITHUB)
    .addIdentityProvider(BITBUCKET);

  WsActionTester ws = new WsActionTester(new IdentityProvidersAction(identityProviderRepository));

  @Test
  public void json_example() {
    String response = ws.newRequest().execute().getInput();

    assertJson(response).isSimilarTo(getClass().getResource("identity_providers-example.json"));
  }

  @Test
  public void ws_properties() {
    WebService.Action webService = ws.getDef();

    assertThat(webService.key()).isEqualTo("identity_providers");
    assertThat(webService.responseExampleAsString()).isNotEmpty();
    assertThat(webService.since()).isEqualTo("5.5");
    assertThat(webService.isInternal()).isTrue();

  }
}
