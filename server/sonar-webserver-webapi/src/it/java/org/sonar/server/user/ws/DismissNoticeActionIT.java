/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.property.PropertyDto;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.server.user.ws.DismissNoticeAction.AVAILABLE_NOTICE_KEYS;

@RunWith(DataProviderRunner.class)
public class DismissNoticeActionIT {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  private final WsActionTester tester = new WsActionTester(new DismissNoticeAction(userSessionRule, db.getDbClient()));

  @Test
  public void authentication_is_required() {
    TestRequest testRequest = tester.newRequest()
      .setParam("notice", "anyValue");

    assertThatThrownBy(testRequest::execute)
      .isInstanceOf(UnauthorizedException.class)
      .hasMessage("Authentication is required");
  }

  @Test
  public void notice_parameter_is_mandatory() {
    userSessionRule.logIn();
    TestRequest testRequest = tester.newRequest();

    assertThatThrownBy(testRequest::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("The 'notice' parameter is missing");
  }

  @Test
  public void notice_not_supported() {
    userSessionRule.logIn();
    TestRequest testRequest = tester.newRequest()
      .setParam("notice", "not_supported_value");

    assertThatThrownBy(testRequest::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage(
        "Value of parameter 'notice' (not_supported_value) must be one of: [educationPrinciples, sonarlintAd, issueCleanCodeGuide, qualityGateCaYCConditionsSimplification, " +
          "overviewZeroNewIssuesSimplification, issueNewIssueStatusAndTransitionGuide, onboardingDismissCaycBranchSummaryGuide, showNewModesTour, showNewModesBanner]");
  }

  @Test
  public void notice_already_exist_dont_fail() {
    userSessionRule.logIn();
    PropertyDto property = new PropertyDto().setKey("user.dismissedNotices.educationPrinciples").setUserUuid(userSessionRule.getUuid());
    db.properties().insertProperties(userSessionRule.getLogin(), null, null, null, property);
    assertThat(db.properties().findFirstUserProperty(userSessionRule.getUuid(), "user.dismissedNotices.educationPrinciples")).isPresent();

    TestResponse testResponse = tester.newRequest()
      .setParam("notice", "educationPrinciples")
      .execute();

    assertThat(testResponse.getStatus()).isEqualTo(204);
    assertThat(db.properties().findFirstUserProperty(userSessionRule.getUuid(), "user.dismissedNotices.educationPrinciples")).isPresent();
  }

  @Test
  @UseDataProvider("noticeKeys")
  public void dismiss_notice(String noticeKey) {
    userSessionRule.logIn();

    TestResponse testResponse = tester.newRequest()
      .setParam("notice", noticeKey)
      .execute();

    assertThat(testResponse.getStatus()).isEqualTo(204);

    Optional<PropertyDto> propertyDto = db.properties().findFirstUserProperty(userSessionRule.getUuid(), "user.dismissedNotices." + noticeKey);
    assertThat(propertyDto).isPresent();
  }

  @DataProvider
  public static Object[][] noticeKeys() {
    return AVAILABLE_NOTICE_KEYS.stream()
      .map(noticeKey -> new Object[] {noticeKey})
      .toArray(Object[][]::new);
  }
}
