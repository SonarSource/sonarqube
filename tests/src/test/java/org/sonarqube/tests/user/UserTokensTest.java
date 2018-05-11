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

package org.sonarqube.tests.user;

import com.sonar.orchestrator.Orchestrator;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.ws.UserTokens.SearchWsResponse.UserToken;
import org.sonarqube.ws.Users.CreateWsResponse.User;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.usertokens.GenerateRequest;
import org.sonarqube.ws.client.usertokens.RevokeRequest;
import org.sonarqube.ws.client.usertokens.SearchRequest;

import static org.assertj.core.api.Assertions.assertThat;

public class UserTokensTest {

  @ClassRule
  public static final Orchestrator orchestrator = UserSuite.ORCHESTRATOR;

  @Rule
  public Tester tester = new Tester(orchestrator).disableOrganizations();

  @Test
  public void generate_and_search_for_user_tokens() {
    User user = tester.users().generate();
    // Generate tokens for the user
    WsClient userWsClient = tester.as(user.getLogin()).wsClient();
    userWsClient.userTokens().generate(new GenerateRequest().setName("token1"));
    userWsClient.userTokens().generate(new GenerateRequest().setName("token2"));
    // Generate token for another user
    User anotherUser = tester.users().generate();
    tester.as(anotherUser.getLogin()).wsClient().userTokens().generate(new GenerateRequest().setName("token"));

    assertThat(userWsClient.userTokens().search(new SearchRequest()).getUserTokensList())
      .extracting(UserToken::getName)
      .containsExactlyInAnyOrder("token1", "token2");
  }

  @Test
  public void revoke_user_token() {
    User user = tester.users().generate();
    WsClient userWsClient = tester.as(user.getLogin()).wsClient();
    userWsClient.userTokens().generate(new GenerateRequest().setName("token1"));
    userWsClient.userTokens().generate(new GenerateRequest().setName("token2"));
    assertThat(userWsClient.userTokens().search(new SearchRequest()).getUserTokensList())
      .extracting(UserToken::getName)
      .containsExactlyInAnyOrder("token1", "token2");

    userWsClient.userTokens().revoke(new RevokeRequest().setName("token2"));

    assertThat(userWsClient.userTokens().search(new SearchRequest()).getUserTokensList())
      .extracting(UserToken::getName)
      .containsExactlyInAnyOrder("token1");
  }

  @Test
  public void admin_can_generate_and_search_for_any_user_tokens() {
    User user = tester.users().generate();
    User admin = tester.users().generateAdministrator();
    WsClient adminWsClient = tester.as(admin.getLogin()).wsClient();

    adminWsClient.userTokens().generate(new GenerateRequest().setLogin(user.getLogin()).setName("token1"));
    adminWsClient.userTokens().generate(new GenerateRequest().setLogin(user.getLogin()).setName("token2"));

    assertThat(adminWsClient.userTokens().search(new SearchRequest().setLogin(user.getLogin())).getUserTokensList())
      .extracting(UserToken::getName)
      .containsExactlyInAnyOrder("token1", "token2");
  }

  @Test
  public void admin_can_revoke_token_from_any_user() {
    User user = tester.users().generate();
    WsClient userWsClient = tester.as(user.getLogin()).wsClient();
    User admin = tester.users().generateAdministrator();
    WsClient adminWsClient = tester.as(admin.getLogin()).wsClient();

    userWsClient.userTokens().generate(new GenerateRequest().setName("token1"));
    userWsClient.userTokens().generate(new GenerateRequest().setName("token2"));
    assertThat(userWsClient.userTokens().search(new SearchRequest()).getUserTokensList())
      .extracting(UserToken::getName)
      .containsExactlyInAnyOrder("token1", "token2");

    adminWsClient.userTokens().revoke(new RevokeRequest().setLogin(user.getLogin()).setName("token2"));

    assertThat(userWsClient.userTokens().search(new SearchRequest()).getUserTokensList())
      .extracting(UserToken::getName)
      .containsExactlyInAnyOrder("token1");
  }
}
