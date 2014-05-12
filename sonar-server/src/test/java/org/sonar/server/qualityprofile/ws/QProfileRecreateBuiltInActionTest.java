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

package org.sonar.server.qualityprofile.ws;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.server.qualityprofile.ActiveRuleService;
import org.sonar.server.qualityprofile.QProfileBackup;
import org.sonar.server.qualityprofile.QProfileResult;
import org.sonar.server.ws.WsTester;

import static com.google.common.collect.Lists.newArrayList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class QProfileRecreateBuiltInActionTest {

  @Mock
  QProfileBackup qProfileBackup;

  WsTester tester;

  @Before
  public void setUp() throws Exception {
    tester = new WsTester(new QProfilesWs(
      new QProfileRecreateBuiltInAction(qProfileBackup),
      new RuleActivationActions(mock(ActiveRuleService.class))));
  }

  @Test
  public void return_empty_result_when_no_infos_or_warnings() throws Exception {
    when(qProfileBackup.recreateBuiltInProfilesByLanguage("java")).thenReturn(new QProfileResult());

    WsTester.TestRequest request = tester.newPostRequest("api/qualityprofiles", "recreate_built_in").setParam("language", "java");
    request.execute().assertNoContent();
  }

  @Test
  public void show_infos() throws Exception {
    when(qProfileBackup.recreateBuiltInProfilesByLanguage("java")).thenReturn(new QProfileResult().addInfos(newArrayList("Some info")));

    WsTester.TestRequest request = tester.newPostRequest("api/qualityprofiles", "recreate_built_in").setParam("language", "java");
    request.execute().assertJson(getClass(), "show_infos.json");
  }

  @Test
  public void show_warnings() throws Exception {
    when(qProfileBackup.recreateBuiltInProfilesByLanguage("java")).thenReturn(new QProfileResult().addWarnings(newArrayList("Some warning")));

    WsTester.TestRequest request = tester.newPostRequest("api/qualityprofiles", "recreate_built_in").setParam("language", "java");
    request.execute().assertJson(getClass(), "show_warnings.json");
  }

  @Test
  public void show_infos_and_warnings() throws Exception {
    when(qProfileBackup.recreateBuiltInProfilesByLanguage("java")).thenReturn(new QProfileResult().addInfos(newArrayList("Some info")).addWarnings(newArrayList("Some warning")));

    WsTester.TestRequest request = tester.newPostRequest("api/qualityprofiles", "recreate_built_in").setParam("language", "java");
    request.execute().assertJson(getClass(), "show_infos_and_warnings.json");
  }
}
