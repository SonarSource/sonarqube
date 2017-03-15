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
package org.sonar.server.rule.ws;

import java.util.Locale;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.i18n.I18n;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.db.rule.RuleRepositoryDto;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.qualityprofile.QProfileTesting;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsTester;

import static java.util.Arrays.asList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_PROFILES;

public class AppActionTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  private Languages languages = mock(Languages.class);
  private DefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(db);
  private I18n i18n = mock(I18n.class);

  @Test
  public void should_generate_app_init_info() throws Exception {
    AppAction app = new AppAction(languages, db.getDbClient(), i18n, userSessionRule, defaultOrganizationProvider);
    WsTester tester = new WsTester(new RulesWs(app));

    userSessionRule.addPermission(ADMINISTER_QUALITY_PROFILES, defaultOrganizationProvider.get().getUuid());

    QualityProfileDto profile1 = QProfileTesting.newXooP1(db.getDefaultOrganization());
    QualityProfileDto profile2 = QProfileTesting.newXooP2(db.getDefaultOrganization()).setParentKee(QProfileTesting.XOO_P1_KEY);
    db.getDbClient().qualityProfileDao().insert(db.getSession(), profile1);
    db.getDbClient().qualityProfileDao().insert(db.getSession(), profile2);
    db.commit();

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
    db.getDbClient().ruleRepositoryDao().insert(db.getSession(), asList(repo1, repo2));
    db.getSession().commit();

    when(i18n.message(isA(Locale.class), anyString(), anyString())).thenAnswer(
      invocation -> invocation.getArguments()[1]);

    tester.newGetRequest("api/rules", "app").execute().assertJson(this.getClass(), "app.json");
  }
}
