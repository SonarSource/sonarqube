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
import org.sonarqube.ws.Users.CreateWsResponse.User;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.users.DeactivateRequest;
import util.ItUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class OnboardingTest {

  private static final String ONBOARDING_TUTORIAL_SHOW_TO_NEW_USERS = "sonar.onboardingTutorial.showToNewUsers";

  @ClassRule
  public static final Orchestrator orchestrator = Orchestrator.builderEnv().build();

  @Rule
  public Tester tester = new Tester(orchestrator).disableOrganizations();

  @Test
  public void by_default_new_user_does_not_see_onboarding_tutorial() {
    User user = tester.users().generate();

    verifyTutorial(user, false);
  }

  @Test
  public void new_user_see_onboarding_tutorial_when_show_onboarding_setting_is_enabled() {
    setShownOnboardingSetting(true);
    User user = tester.users().generate();

    verifyTutorial(user, true);
  }

  @Test
  public void new_user_does_not_see_onboarding_tutorial_when_show_onboarding_setting_is_disabled() {
    setShownOnboardingSetting(false);
    User user = tester.users().generate();

    verifyTutorial(user, false);
  }

  @Test
  public void new_user_does_not_see_onboarding_tutorial_when_show_onboarding_setting_is_enabled_after_user_creation() {
    setShownOnboardingSetting(false);
    // User is created when show onboading is disabled
    User user = tester.users().generate();
    setShownOnboardingSetting(true);

    // The user doesn't see the tutorial as he was created when the show onboading setting was disabled
    verifyTutorial(user, false);
  }

  @Test
  public void skip_onboarding_tutorial() {
    setShownOnboardingSetting(true);
    User user = tester.users().generate();

    tester.as(user.getLogin()).wsClient().users().skipOnboardingTutorial();

    verifyTutorial(user, false);
  }

  @Test
  public void skip_onboarding_tutorial_when_show_onboarding_setting_is_disabled() {
    setShownOnboardingSetting(true);
    User user = tester.users().generate();

    tester.as(user.getLogin()).wsClient().users().skipOnboardingTutorial();

    verifyTutorial(user, false);
  }

  @Test
  public void anonymous_user_does_not_see_onboarding_tutorial() {
    setShownOnboardingSetting(true);

    // anonymous should not see the onboarding tutorial
    verifyTutorialForAnonymous(false);

    // anonymous should not be able to skip the tutorial
    ItUtils.expectHttpError(401, () -> tester.asAnonymous().wsClient().users().skipOnboardingTutorial());
  }

  @Test
  public void admin_user_see_onboarding_tutorial() {

    assertThat(tester.wsClient().users().current().getShowOnboardingTutorial()).isEqualTo(true);

    // Onboarding setting has no effect as admin is created at startup
    setShownOnboardingSetting(false);
    assertThat(tester.wsClient().users().current().getShowOnboardingTutorial()).isEqualTo(true);

    setShownOnboardingSetting(true);
    assertThat(tester.wsClient().users().current().getShowOnboardingTutorial()).isEqualTo(true);
  }

  @Test
  public void reactivated_user_should_see_the_onboarding_tutorial() {
    setShownOnboardingSetting(true);
    User user = tester.users().generate();
    tester.as(user.getLogin()).wsClient().users().skipOnboardingTutorial();
    verifyTutorial(user, false);

    tester.wsClient().users().deactivate(new DeactivateRequest().setLogin(user.getLogin()));
    User reactivatedUser = tester.users().generate(u -> u.setLogin(user.getLogin()).setName(user.getName()).setPassword(user.getLogin()));

    verifyTutorial(reactivatedUser, true);
  }

  private void setShownOnboardingSetting(boolean showTutorial) {
    tester.settings().setGlobalSettings(ONBOARDING_TUTORIAL_SHOW_TO_NEW_USERS, String.valueOf(showTutorial));
  }

  private void verifyTutorial(User user, boolean expectedTutorial) {
    WsClient wsClient = tester.as(user.getLogin()).wsClient();
    assertThat(wsClient.users().current().getShowOnboardingTutorial()).isEqualTo(expectedTutorial);
  }

  private void verifyTutorialForAnonymous(boolean expectedTutorial) {
    WsClient wsClient = tester.asAnonymous().wsClient();
    assertThat(wsClient.users().current().getShowOnboardingTutorial()).isEqualTo(expectedTutorial);
  }
}
