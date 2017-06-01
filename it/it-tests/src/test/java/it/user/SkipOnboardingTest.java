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
import org.junit.Test;
import org.sonarqube.ws.client.WsResponse;
import util.ItUtils;
import util.user.UserRule;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;

public class SkipOnboardingTest {

  @ClassRule
  public static final Orchestrator orchestrator = Category4Suite.ORCHESTRATOR;
  @ClassRule
  public static UserRule userRule = UserRule.from(orchestrator);

  @Test
  public void should_operate_silently() {
    String login = randomAlphabetic(10).toLowerCase();
    String password = randomAlphabetic(10).toLowerCase();
    userRule.createUser(login, password);
    WsResponse response;
    try {

      response = ItUtils.newUserWsClient(orchestrator, null, null).users().skipOnboardingTutorial();

    } finally {
      userRule.deactivateUsers(login);
    }

    assertThat(response.code()).isEqualTo(204);
    assertThat(response.hasContent()).isFalse();
  }
}
