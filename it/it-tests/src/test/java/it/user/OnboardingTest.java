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
import java.util.Optional;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonarqube.ws.client.HttpException;
import org.sonarqube.ws.client.user.UsersService;
import util.user.UserRule;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.newAdminWsClient;
import static util.ItUtils.newUserWsClient;
import static util.ItUtils.resetSettings;
import static util.ItUtils.setServerProperty;

public class OnboardingTest {

  @ClassRule
  public static final Orchestrator orchestrator = Category4Suite.ORCHESTRATOR;
  @ClassRule
  public static UserRule userRule = UserRule.from(orchestrator);
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private static final String ONBOARDING_TUTORIAL_SHOW_TO_NEW_USERS = "sonar.onboardingTutorial.showToNewUsers";

  private String userLogin;

  @Before
  public void setUp() throws Exception {
    resetSettings(orchestrator, null, ONBOARDING_TUTORIAL_SHOW_TO_NEW_USERS);
  }

  @After
  public void reset() throws Exception {
    Optional.ofNullable(userLogin).ifPresent(login -> userRule.deactivateUsers(userLogin));
    resetSettings(orchestrator, null, ONBOARDING_TUTORIAL_SHOW_TO_NEW_USERS);
  }

  @Test
  public void by_default_new_user_does_not_see_onboarding_tutorial() {
    createUser();

    assertOnboardingTutorial(false);
  }

  @Test
  public void new_user_see_onboarding_tutorial_when_show_onboarding_setting_is_enabled() {
    setShownOnboardingSetting(true);
    createUser();

    assertOnboardingTutorial(true);
  }

  @Test
  public void new_user_does_not_see_onboarding_tutorial_when_show_onboarding_setting_is_disabled() {
    setShownOnboardingSetting(false);
    createUser();

    assertOnboardingTutorial(false);
  }

  @Test
  public void new_user_does_not_see_onboarding_tutorial_when_show_onboarding_setting_is_enabled_after_user_creation() {
    setShownOnboardingSetting(false);
    // User is created when show onboading is disabled
    createUser();
    setShownOnboardingSetting(true);

    // The user doesn't see the tutorial as he was created when the show onboading setting was disabled
    assertOnboardingTutorial(false);
  }

  @Test
  public void skip_onboarding_tutorial() {
    setShownOnboardingSetting(true);
    createUser();

    createUsersServiceForUser().skipOnboardingTutorial();

    assertOnboardingTutorial(false);
  }

  @Test
  public void skip_onboarding_tutorial_when_show_onboarding_setting_is_disabled() {
    setShownOnboardingSetting(true);
    createUser();

    createUsersServiceForUser().skipOnboardingTutorial();

    assertOnboardingTutorial(false);
  }

  @Test
  public void anonymous_user_does_not_see_onboarding_tutorial() {
    setShownOnboardingSetting(true);

    // anonymous should not see the onboarding tutorial
    assertOnboardingTutorial(false);

    // anonymous should not be able to skip the tutorial
    expectedException.expect(HttpException.class);
    createUsersServiceForUser().skipOnboardingTutorial();
  }

  @Test
  public void admin_user_see_onboarding_tutorial() {
    UsersService adminService = newAdminWsClient(orchestrator).users();

    assertThat(adminService.current().getShowOnboardingTutorial()).isEqualTo(true);

    // Onboarding setting has no effect as admin is created at startup
    setShownOnboardingSetting(false);
    assertThat(adminService.current().getShowOnboardingTutorial()).isEqualTo(true);

    setShownOnboardingSetting(true);
    assertThat(adminService.current().getShowOnboardingTutorial()).isEqualTo(true);
  }

  @Test
  public void reactivated_user_should_see_the_onboarding_tutorial() {
    setShownOnboardingSetting(true);
    createUser();
    createUsersServiceForUser().skipOnboardingTutorial();
    assertOnboardingTutorial(false);

    userRule.deactivateUsers(userLogin);
    userRule.createUser(userLogin, userLogin);

    assertOnboardingTutorial(true);
  }

  private void createUser() {
    userLogin = randomAlphabetic(10).toLowerCase();
    userRule.createUser(userLogin, userLogin);
  }

  private static void setShownOnboardingSetting(boolean showOnboardingTutorial) {
    setServerProperty(orchestrator, ONBOARDING_TUTORIAL_SHOW_TO_NEW_USERS, String.valueOf(showOnboardingTutorial));
  }

  private void assertOnboardingTutorial(boolean expectedOnboardingTutorial) {
    assertThat(createUsersServiceForUser().current().getShowOnboardingTutorial()).isEqualTo(expectedOnboardingTutorial);
  }

  private UsersService createUsersServiceForUser() {
    return newUserWsClient(orchestrator, userLogin, userLogin).users();
  }

}
