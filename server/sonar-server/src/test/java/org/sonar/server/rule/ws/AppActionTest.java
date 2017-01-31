/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.rule.ws;

import java.util.Locale;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sonar.api.i18n.I18n;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.utils.System2;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbTester;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.db.rule.RuleRepositoryDto;
import org.sonar.server.qualityprofile.QProfileTesting;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsTester;

import static java.util.Arrays.asList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AppActionTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  private Languages languages = mock(Languages.class);

  private I18n i18n = mock(I18n.class);

  @Test
  public void should_generate_app_init_info() throws Exception {
    AppAction app = new AppAction(languages, dbTester.getDbClient(), i18n, userSessionRule);
    WsTester tester = new WsTester(new RulesWs(app));

    userSessionRule.setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN);

    QualityProfileDto profile1 = QProfileTesting.newXooP1();
    QualityProfileDto profile2 = QProfileTesting.newXooP2().setParentKee(QProfileTesting.XOO_P1_KEY);
    dbTester.getDbClient().qualityProfileDao().insert(dbTester.getSession(), profile1);
    dbTester.getDbClient().qualityProfileDao().insert(dbTester.getSession(), profile2);
    dbTester.commit();

    Language xoo = mock(Language.class);
    when(xoo.getKey()).thenReturn("xoo");
    when(xoo.getName()).thenReturn("Xoo");
    Language whitespace = mock(Language.class);
    when(whitespace.getKey()).thenReturn("ws");
    when(whitespace.getName()).thenReturn("Whitespace");
    when(languages.get("xoo")).thenReturn(xoo);
    when(languages.all()).thenReturn(new Language[] {xoo, whitespace});

    RuleRepositoryDto repo1 = new RuleRepositoryDto("xoo", "xoo", "SonarQube");
    RuleRepositoryDto repo2 = new RuleRepositoryDto("squid", "ws", "SonarQube");
    dbTester.getDbClient().ruleRepositoryDao().insert(dbTester.getSession(), asList(repo1, repo2));
    dbTester.getSession().commit();

    when(i18n.message(isA(Locale.class), anyString(), anyString())).thenAnswer(new Answer<String>() {
      @Override
      public String answer(InvocationOnMock invocation) throws Throwable {
        return (String) invocation.getArguments()[1];
      }
    });

    tester.newGetRequest("api/rules", "app").execute().assertJson(this.getClass(), "app.json");
  }
}
