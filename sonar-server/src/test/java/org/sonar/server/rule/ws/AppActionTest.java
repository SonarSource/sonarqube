/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.rule.ws;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.sonar.api.i18n.I18n;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.server.debt.DebtCharacteristic;
import org.sonar.api.server.debt.DebtModel;
import org.sonar.api.server.debt.internal.DefaultDebtCharacteristic;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.qualityprofile.db.QualityProfileDto;
import org.sonar.server.qualityprofile.QProfileLoader;
import org.sonar.server.qualityprofile.QProfileService;
import org.sonar.server.qualityprofile.QProfileTesting;
import org.sonar.server.rule.RuleRepositories;
import org.sonar.server.user.MockUserSession;
import org.sonar.server.ws.WsTester;

import java.util.Locale;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AppActionTest {

  @Mock
  Languages languages;

  @Mock
  RuleRepositories ruleRepositories;

  @Mock
  I18n i18n;

  @Mock
  DebtModel debtModel;

  @Mock
  QProfileLoader profileLoader;

  @Test
  public void should_generate_app_init_info() throws Exception {
    AppAction app = new AppAction(languages, ruleRepositories, i18n, debtModel, profileLoader);
    WsTester tester = new WsTester(new RulesWebService(
      mock(SearchAction.class), mock(ShowAction.class), mock(TagsAction.class), mock(CreateAction.class),
      app, mock(UpdateAction.class), mock(DeleteAction.class)));

    MockUserSession.set().setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN);

    QualityProfileDto profile1 = QProfileTesting.newXooP1();
    QualityProfileDto profile2 = QProfileTesting.newXooP2().setParentKee(QProfileTesting.XOO_P1_KEY);
    when(profileLoader.findAll()).thenReturn(ImmutableList.of(profile1, profile2));

    Language xoo = mock(Language.class);
    when(xoo.getKey()).thenReturn("xoo");
    when(xoo.getName()).thenReturn("Xoo");
    Language whitespace = mock(Language.class);
    when(whitespace.getKey()).thenReturn("ws");
    when(whitespace.getName()).thenReturn("Whitespace");
    when(languages.get("xoo")).thenReturn(xoo);
    when(languages.all()).thenReturn(new Language[]{xoo, whitespace});

    RuleRepositories.Repository repo1 = mock(RuleRepositories.Repository.class);
    when(repo1.key()).thenReturn("xoo");
    when(repo1.name()).thenReturn("SonarQube");
    when(repo1.language()).thenReturn("xoo");
    RuleRepositories.Repository repo2 = mock(RuleRepositories.Repository.class);
    when(repo2.key()).thenReturn("squid");
    when(repo2.name()).thenReturn("SonarQube");
    when(repo2.language()).thenReturn("ws");
    when(ruleRepositories.repositories()).thenReturn(ImmutableList.of(repo1, repo2));

    when(i18n.message(isA(Locale.class), anyString(), anyString())).thenAnswer(new Answer<String>() {
      @Override
      public String answer(InvocationOnMock invocation) throws Throwable {
        return (String) invocation.getArguments()[1];
      }
    });

    int parentId = 42;
    DefaultDebtCharacteristic char1 = new DefaultDebtCharacteristic();
    char1.setId(parentId).setKey("REUSABILITY").setName("Reusability");
    DefaultDebtCharacteristic char2 = new DefaultDebtCharacteristic();
    char2.setId(24).setParentId(parentId).setKey("MODULARITY").setName("Modularity");
    when(debtModel.allCharacteristics()).thenReturn(ImmutableList.<DebtCharacteristic>of(char1, char2));

    tester.newGetRequest("api/rules", "app").execute().assertJson(this.getClass(), "app.json");
  }
}
