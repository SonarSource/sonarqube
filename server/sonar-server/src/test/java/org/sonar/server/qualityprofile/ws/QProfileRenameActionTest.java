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

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.DbTester;
import org.sonar.core.qualityprofile.db.QualityProfileDao;
import org.sonar.core.qualityprofile.db.QualityProfileDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.qualityprofile.QProfileFactory;
import org.sonar.server.user.MockUserSession;
import org.sonar.server.ws.WsTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class QProfileRenameActionTest {

  @ClassRule
  public static DbTester dbTester = new DbTester();

  private DbClient dbClient;

  private QualityProfileDao qualityProfileDao;

  private String xoo1Key = "xoo1", xoo2Key = "xoo2";

  private WsTester tester;

  private DbSession session;


  @Before
  public void setUp() throws Exception {
    dbTester.truncateTables();
    qualityProfileDao = new QualityProfileDao(dbTester.myBatis(), mock(System2.class));
    dbClient = new DbClient(dbTester.database(), dbTester.myBatis(), qualityProfileDao);
    session = dbClient.openSession(false);

    createProfiles();

    tester = new WsTester(new QProfilesWs(
      mock(RuleActivationActions.class),
      mock(BulkRuleActivationActions.class),
      mock(ProjectAssociationActions.class),
      new QProfileRenameAction(new QProfileFactory(dbClient))));
  }

  @After
  public void tearDown() {
    session.close();
  }

  @Test
  public void rename_nominal() throws Exception {
    MockUserSession.set().setLogin("obiwan").setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN);

    tester.newPostRequest("api/qualityprofiles", "rename")
      .setParam("key", "sonar-way-xoo2-23456")
      .setParam("name", "Other Sonar Way")
      .execute().assertNoContent();

    assertThat(qualityProfileDao.getNonNullByKey(session, "sonar-way-xoo2-23456").getName()).isEqualTo("Other Sonar Way");
  }

  @Test(expected = BadRequestException.class)
  public void do_nothing_on_conflict() throws Exception {
    MockUserSession.set().setLogin("obiwan").setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN);

    tester.newPostRequest("api/qualityprofiles", "rename")
      .setParam("key", "sonar-way-xoo2-23456")
      .setParam("name", "My Sonar way")
      .execute();
  }

  @Test(expected = IllegalArgumentException.class)
  public void fail_on_missing_key() throws Exception {
    MockUserSession.set().setLogin("obiwan").setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN);

    tester.newPostRequest("api/qualityprofiles", "rename")
      .setParam("name", "Other Sonar Way")
      .execute();
  }

  @Test(expected = IllegalArgumentException.class)
  public void fail_on_missing_name() throws Exception {
    MockUserSession.set().setLogin("obiwan").setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN);

    tester.newPostRequest("api/qualityprofiles", "rename")
      .setParam("key", "sonar-way-xoo1-13245")
      .execute();
  }

  @Test(expected = ForbiddenException.class)
  public void fail_on_missing_permission() throws Exception {
    MockUserSession.set().setLogin("obiwan");

    tester.newPostRequest("api/qualityprofiles", "rename")
      .setParam("key", "sonar-way-xoo1-13245")
      .setParam("name", "Hey look I am not quality profile admin!")
      .execute();
  }

  @Test(expected = IllegalArgumentException.class)
  public void fail_on_unknown_profile() throws Exception {
    MockUserSession.set().setLogin("obiwan").setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN);

    tester.newPostRequest("api/qualityprofiles", "rename")
      .setParam("key", "polop")
      .setParam("name", "Uh oh, I don't know this profile")
      .execute();
  }

  private void createProfiles() {
    qualityProfileDao.insert(session,
      QualityProfileDto.createFor("sonar-way-xoo1-12345").setLanguage(xoo1Key).setName("Sonar way").setDefault(true),
      QualityProfileDto.createFor("sonar-way-xoo2-23456").setLanguage(xoo2Key).setName("Sonar way"),
      QualityProfileDto.createFor("my-sonar-way-xoo2-34567").setLanguage(xoo2Key).setName("My Sonar way").setParentKee("sonar-way-xoo2-23456").setDefault(true)
      );
    session.commit();
  }
}
