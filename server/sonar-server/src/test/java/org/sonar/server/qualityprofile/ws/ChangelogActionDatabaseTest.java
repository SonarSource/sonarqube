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
package org.sonar.server.qualityprofile.ws;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.resources.Languages;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.QProfileChangeDto;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.db.qualityprofile.QualityProfileTesting;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;

public class ChangelogActionDatabaseTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private WsActionTester wsTester;
  private ChangelogLoader changelogLoader;
  private QProfileWsSupport wsSupport;
  private OrganizationDto organization;
  private DefaultOrganizationProvider defaultOrganizationProvider;

  @Before
  public void before() {
    defaultOrganizationProvider = TestDefaultOrganizationProvider.from(dbTester);
    wsSupport = new QProfileWsSupport(dbTester.getDbClient(), userSession, defaultOrganizationProvider);
    changelogLoader = new ChangelogLoader(dbTester.getDbClient());
    wsTester = new WsActionTester(
      new ChangelogAction(changelogLoader, wsSupport, new Languages(), dbTester.getDbClient()));
    organization = dbTester.organizations().insert();
  }

  @Test
  public void find_changelog_by_profileKey() throws Exception {
    QualityProfileDto qualityProfile = dbTester.qualityProfiles().insert(organization);

    String response = wsTester.newRequest()
      .setMethod("GET")
      .setParam("profileKey", qualityProfile.getKey())
      .execute()
      .getInput();

    assertThat(response).isNotEmpty();
  }

  @Test
  public void find_changelog_by_language_and_name() throws Exception {
    QualityProfileDto qualityProfile = dbTester.qualityProfiles().insert(dbTester.getDefaultOrganization());

    String response = wsTester.newRequest()
      .setMethod("GET")
      .setParam("language", qualityProfile.getLanguage())
      .setParam("profileName", qualityProfile.getName())
      .execute()
      .getInput();

    assertThat(response).isNotEmpty();
  }

  @Test
  public void find_changelog_by_organization_and_language_and_name() throws Exception {
    QualityProfileDto qualityProfile = dbTester.qualityProfiles().insert(organization);

    String response = wsTester.newRequest()
      .setMethod("GET")
      .setParam("language", qualityProfile.getLanguage())
      .setParam("profileName", qualityProfile.getName())
      .setParam("organization", organization.getKey())
      .execute()
      .getInput();

    assertThat(response).isNotEmpty();
  }

  @Test
  public void do_not_find_changelog_by_wrong_organization_and_language_and_name() throws Exception {
    OrganizationDto organization1 = dbTester.organizations().insert();
    OrganizationDto organization2 = dbTester.organizations().insert();

    QualityProfileDto qualityProfile = dbTester.qualityProfiles().insert(organization1);

    TestRequest request = wsTester.newRequest()
      .setMethod("GET")
      .setParam("language", qualityProfile.getLanguage())
      .setParam("profileName", qualityProfile.getName())
      .setParam("organization", organization2.getKey());

    thrown.expect(NotFoundException.class);

    request.execute();
  }

  @Test
  public void changelog_empty() throws Exception {
    QualityProfileDto qualityProfile = dbTester.qualityProfiles().insert(organization);

    String response = wsTester.newRequest()
      .setMethod("GET")
      .setParam("profileKey", qualityProfile.getKey())
      .execute()
      .getInput();

    assertThat(response).contains("\"total\":0");
    assertThat(response).contains("\"events\":[]");
  }

  @Test
  public void changelog_not_empty() throws Exception {
    QualityProfileDto qualityProfile = dbTester.qualityProfiles().insert(organization);
    QProfileChangeDto change = QualityProfileTesting.newQProfileChangeDto()
      .setKey(null)
      .setCreatedAt(0)
      .setProfileKey(qualityProfile.getKey());
    DbSession session = dbTester.getSession();
    dbTester.getDbClient().qProfileChangeDao().insert(session, change);
    session.commit();

    String response = wsTester.newRequest()
      .setMethod("GET")
      .setParam("profileKey", qualityProfile.getKey())
      .execute()
      .getInput();

    assertThat(response).contains("\"total\":1");
  }
}
