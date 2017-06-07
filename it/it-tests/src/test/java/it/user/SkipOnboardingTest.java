/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package it.user;

import com.sonar.orchestrator.Orchestrator;
import it.Category4Suite;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonarqube.ws.client.HttpException;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.WsResponse;
import org.sonarqube.ws.client.setting.ResetRequest;
import org.sonarqube.ws.client.setting.SetRequest;
import util.ItUtils;
import util.user.UserRule;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;

public class SkipOnboardingTest {

  @ClassRule
  public static final Orchestrator orchestrator = Category4Suite.ORCHESTRATOR;
  @ClassRule
  public static UserRule userRule = UserRule.from(orchestrator);
  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void onboarding_tutorial_for_anonymous() {
    WsClient wsClient = ItUtils.newUserWsClient(orchestrator, null, null);

    // anonymous should not see the onboarding tutorial
    assertThat((boolean) ItUtils.jsonToMap(wsClient.users().current().content()).get("showOnboardingTutorial")).isFalse();

    // anonymous should not be able to skip the tutorial
    thrown.expect(HttpException.class);
    wsClient.users().skipOnboardingTutorial();
  }

  @Test
  public void default_usecase() {
    ItUtils.newAdminWsClient(orchestrator).settingsService().reset(ResetRequest.builder().setKeys("sonar.onboardingTutorial.skip").build());

    // Step 1 create user
    String login = randomAlphabetic(10).toLowerCase();
    String password = randomAlphabetic(10).toLowerCase();
    userRule.createUser(login, password);
    WsClient wsClient = ItUtils.newUserWsClient(orchestrator, login, password);
    try {

      // the user should now see the onboarding tutorial
      assertThat((boolean) ItUtils.jsonToMap(wsClient.users().current().content()).get("showOnboardingTutorial")).isTrue();

      // Step 2 let the user skip the tutorial
      WsResponse response = wsClient.users().skipOnboardingTutorial();
      assertThat(response.code()).isEqualTo(204);
      assertThat(response.hasContent()).isFalse();

      // the user should not see the onboarding tutorial anymore
      assertThat((boolean) ItUtils.jsonToMap(wsClient.users().current().content()).get("showOnboardingTutorial")).isFalse();

      // Step 3 let the user skip the tutorial again
      WsResponse response2 = wsClient.users().skipOnboardingTutorial();
      assertThat(response2.code()).isEqualTo(204);
      assertThat(response2.hasContent()).isFalse();

      // the user should not see the onboarding tutorial anymore
      assertThat((boolean) ItUtils.jsonToMap(wsClient.users().current().content()).get("showOnboardingTutorial")).isFalse();
    } finally {
      userRule.deactivateUsers(login);
    }
  }

  @Test
  public void do_not_show_tutorial_if_the_administrators_decided_to_skip_it() {
    skipOnboardingGlobally(true);

    // Step 1 create user
    String login = randomAlphabetic(10).toLowerCase();
    String password = randomAlphabetic(10).toLowerCase();
    userRule.createUser(login, password);
    WsClient wsClient = ItUtils.newUserWsClient(orchestrator, login, password);
    try {

      // the user should not see the onboarding tutorial
      assertThat((boolean) ItUtils.jsonToMap(wsClient.users().current().content()).get("showOnboardingTutorial")).isFalse();

      // Step 2 let the user skip the tutorial
      WsResponse response = wsClient.users().skipOnboardingTutorial();
      assertThat(response.code()).isEqualTo(204);
      assertThat(response.hasContent()).isFalse();

      // the user should not see the onboarding tutorial
      assertThat((boolean) ItUtils.jsonToMap(wsClient.users().current().content()).get("showOnboardingTutorial")).isFalse();
    } finally {
      userRule.deactivateUsers(login);
    }
  }

  @Test
  public void if_user_gets_created_before_setting_skip_onboarding_to_true_he_should_not_see_onboarding_tutorial() {
    skipOnboardingGlobally(false);

    // Step 1 create user
    String login = randomAlphabetic(10).toLowerCase();
    String password = randomAlphabetic(10).toLowerCase();
    userRule.createUser(login, password);
    WsClient wsClient = ItUtils.newUserWsClient(orchestrator, login, password);
    try {

      // the user should see the onboarding tutorial
      assertThat((boolean) ItUtils.jsonToMap(wsClient.users().current().content()).get("showOnboardingTutorial")).isTrue();

      // Step 2 skip onboarding tutorial globally
      skipOnboardingGlobally(true);

      // the user should not see the onboarding tutorial
      assertThat((boolean) ItUtils.jsonToMap(wsClient.users().current().content()).get("showOnboardingTutorial")).isFalse();

      // but the user would see the tutorial, if it was not globally skipped
      skipOnboardingGlobally(false);
      assertThat((boolean) ItUtils.jsonToMap(wsClient.users().current().content()).get("showOnboardingTutorial")).isTrue();
      skipOnboardingGlobally(true);

      // Step 3 let the user skip the tutorial (although he does not see it)
      // side note: this is a valid case, because the tutorial will still be available form the help popup
      WsResponse response = wsClient.users().skipOnboardingTutorial();
      assertThat(response.code()).isEqualTo(204);
      assertThat(response.hasContent()).isFalse();

      // the user should not see the onboarding tutorial
      assertThat((boolean) ItUtils.jsonToMap(wsClient.users().current().content()).get("showOnboardingTutorial")).isFalse();

      // Step 4 do not skip onboarding tutorial globally anymore
      skipOnboardingGlobally(false);

      // the user should not see the onboarding tutorial
      assertThat((boolean) ItUtils.jsonToMap(wsClient.users().current().content()).get("showOnboardingTutorial")).isFalse();
    } finally {
      userRule.deactivateUsers(login);
    }
  }

  private void skipOnboardingGlobally(boolean skipOnboardingTutorial) {
    ItUtils.newAdminWsClient(orchestrator).settingsService()
      .set(SetRequest.builder().setKey("sonar.onboardingTutorial.skip").setValue(String.valueOf(skipOnboardingTutorial)).build());
  }
}
