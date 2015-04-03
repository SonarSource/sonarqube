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

import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.server.ws.WebService.Action;
import org.sonar.api.utils.System2;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.DbTester;
import org.sonar.core.qualityprofile.db.QualityProfileDao;
import org.sonar.server.db.DbClient;
import org.sonar.server.language.LanguageTesting;
import org.sonar.server.qualityprofile.QProfileFactory;
import org.sonar.server.user.MockUserSession;
import org.sonar.server.ws.WsTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class QProfileCreateActionTest {

  @ClassRule
  public static final DbTester db = new DbTester();

  @Test
  public void should_not_fail_on_no_importers() throws Exception {
    QualityProfileDao profileDao = new QualityProfileDao(db.myBatis(), mock(System2.class));
    DbClient dbClient = new DbClient(db.database(), db.myBatis(), profileDao);

    String xooKey = "xoo";
    WsTester wsTester = new WsTester(new QProfilesWs(
      mock(RuleActivationActions.class), mock(BulkRuleActivationActions.class), mock(ProjectAssociationActions.class),
      new QProfileCreateAction(dbClient, new QProfileFactory(dbClient), null, LanguageTesting.newLanguages(xooKey))));

    Action create = wsTester.controller("api/qualityprofiles").action("create");
    assertThat(create.params()).hasSize(2);

    MockUserSession.set().setLogin("anakin").setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN);

    wsTester.newPostRequest("api/qualityprofiles", "create")
      .setParam("language", xooKey).setParam("name", "Yeehaw!").execute().assertJson(getClass(), "create-no-importer.json");
  }
}
