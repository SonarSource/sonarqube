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

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDao;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.QualityProfileDao;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.db.qualityprofile.QualityProfileTesting;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.language.LanguageTesting;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.qualityprofile.QProfileFactory;
import org.sonar.server.qualityprofile.QProfileName;
import org.sonar.server.qualityprofile.QProfileTesting;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_PROFILES;

public class DeleteActionTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  private DbClient dbClient = dbTester.getDbClient();
  private QualityProfileDao qualityProfileDao = dbClient.qualityProfileDao();
  private ComponentDao componentDao = dbClient.componentDao();
  private OrganizationDto organization;
  private Language xoo1;
  private Language xoo2;
  private WsTester tester;
  private DbSession session = dbTester.getSession();

  @Before
  public void setUp() {
    xoo1 = LanguageTesting.newLanguage("xoo1");
    xoo2 = LanguageTesting.newLanguage("xoo2");

    tester = new WsTester(new QProfilesWs(
      mock(RuleActivationActions.class),
      mock(BulkRuleActivationActions.class),
      new DeleteAction(new Languages(xoo1, xoo2), new QProfileFactory(dbClient), dbClient, userSessionRule,
        new QProfileWsSupport(dbClient, userSessionRule, TestDefaultOrganizationProvider.from(dbTester)))));
    organization = dbTester.organizations().insert();
  }

  @After
  public void tearDown() {
    session.close();
  }

  @Test
  public void delete_nominal_with_key() throws Exception {
    String profileKey = "sonar-way-xoo1-12345";

    ComponentDto project = ComponentTesting.newProjectDto(dbTester.organizations().insert(), "polop");
    componentDao.insert(session, project);
    QualityProfileDto qualityProfile = QualityProfileDto.createFor(profileKey)
      .setOrganizationUuid(organization.getUuid())
      .setLanguage(xoo1.getKey())
      .setName("Sonar way");
    qualityProfileDao.insert(session, qualityProfile);
    qualityProfileDao.insertProjectProfileAssociation(project.uuid(), profileKey, session);
    session.commit();

    logInAsQProfileAdministrator();

    tester.newPostRequest("api/qualityprofiles", "delete")
      .setParam("profileKey", "sonar-way-xoo1-12345")
      .execute().assertNoContent();

    assertThat(qualityProfileDao.selectByKey(session, "sonar-way-xoo1-12345")).isNull();
    assertThat(qualityProfileDao.selectProjects("Sonar way", xoo1.getName(), session)).isEmpty();
  }

  @Test
  public void delete_nominal_with_language_and_name() throws Exception {
    String profileKey = "sonar-way-xoo1-12345";

    ComponentDto project = ComponentTesting.newProjectDto(dbTester.organizations().insert(), "polop");
    componentDao.insert(session, project);
    qualityProfileDao.insert(session,
      QualityProfileDto.createFor(profileKey)
        .setOrganizationUuid(organization.getUuid())
        .setLanguage(xoo1.getKey())
        .setName("Sonar way"));
    qualityProfileDao.insertProjectProfileAssociation(project.uuid(), profileKey, session);
    session.commit();

    logInAsQProfileAdministrator();

    tester.newPostRequest("api/qualityprofiles", "delete")
      .setParam("profileName", "Sonar way")
      .setParam("language", xoo1.getKey())
      .setParam("organization", organization.getKey())
      .execute().assertNoContent();

    assertThat(qualityProfileDao.selectByKey(session, "sonar-way-xoo1-12345")).isNull();
    assertThat(qualityProfileDao.selectProjects("Sonar way", xoo1.getName(), session)).isEmpty();
  }

  @Test
  public void fail_if_insufficient_privileges() throws Exception {
    OrganizationDto organizationX = dbTester.organizations().insert();
    OrganizationDto organizationY = dbTester.organizations().insert();

    QualityProfileDto profileInOrganizationY = QualityProfileTesting.newQualityProfileDto()
      .setOrganizationUuid(organizationY.getUuid())
      .setLanguage(xoo1.getKey());
    qualityProfileDao.insert(dbTester.getSession(), profileInOrganizationY);
    session.commit();

    logInAsQProfileAdministrator(organizationX.getUuid());

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    tester.newPostRequest("api/qualityprofiles", "delete")
      .setParam("profileName", profileInOrganizationY.getName())
      .setParam("language", profileInOrganizationY.getLanguage())
      .setParam("organization", organizationY.getKey())
      .execute().assertNoContent();
  }

  @Test
  public void use_default_organization_if_no_organization_key_is_specified() throws Exception {
    logInAsQProfileAdministrator(dbTester.getDefaultOrganization().getUuid());

    QualityProfileDto profileInDefaultOrganization = QualityProfileTesting.newQualityProfileDto().setOrganizationUuid(dbTester.getDefaultOrganization().getUuid());
    qualityProfileDao.insert(dbTester.getSession(), profileInDefaultOrganization);

    assertThat(
      dbClient.qualityProfileDao()
        .selectByKey(dbTester.getSession(), profileInDefaultOrganization.getKey())
    ).isNotNull();
    dbTester.getSession().commit();

    tester.newPostRequest("api/qualityprofiles", "delete")
      .setParam("profileKey", profileInDefaultOrganization.getKey())
      .execute().assertNoContent();

    assertThat(
      dbClient.qualityProfileDao()
        .selectByKey(dbTester.getSession(), profileInDefaultOrganization.getKey())
    ).isNull();
  }

  @Test
  public void throw_ForbiddenException_if_not_profile_administrator() throws Exception {
    userSessionRule.logIn();
    QualityProfileDto profile = insertQualityProfile();

    expectedException.expect(ForbiddenException.class);

    tester.newPostRequest("api/qualityprofiles", "delete")
      .setParam("profileName", profile.getName())
      .setParam("language", profile.getLanguage())
      .setParam("organization", organization.getKey())
      .execute();
  }

  private QualityProfileDto insertQualityProfile() {
    QualityProfileDto profile = QualityProfileTesting.newQualityProfileDto()
      .setOrganizationUuid(organization.getUuid())
      .setLanguage(xoo1.getKey());
    qualityProfileDao.insert(dbTester.getSession(), profile);
    dbTester.getSession().commit();
    return profile;
  }

  @Test
  public void throw_UnauthorizedException_if_not_logged_in() throws Exception {
    expectedException.expect(UnauthorizedException.class);

    tester.newPostRequest("api/qualityprofiles", "delete")
      .setParam("organization", organization.getKey())
      .execute();
  }

  @Test
  public void fail_on_missing_arguments() throws Exception {
    logInAsQProfileAdministrator();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("If no quality profile key is specified, language and name must be set");

    tester.newPostRequest("api/qualityprofiles", "delete")
      .setParam("organization", organization.getKey())
      .execute();
  }

  @Test
  public void fail_on_missing_language() throws Exception {
    logInAsQProfileAdministrator();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("If no quality profile key is specified, language and name must be set");

    tester.newPostRequest("api/qualityprofiles", "delete")
      .setParam("organization", organization.getKey())
      .setParam("profileName", "Polop").execute();
  }

  @Test
  public void fail_on_missing_name() throws Exception {
    logInAsQProfileAdministrator();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("If no quality profile key is specified, language and name must be set");

    tester.newPostRequest("api/qualityprofiles", "delete")
      .setParam("language", xoo1.getKey())
      .setParam("organization", organization.getKey())
      .execute();
  }

  @Test
  public void fail_on_too_many_arguments() throws Exception {
    logInAsQProfileAdministrator();
    QualityProfileDto profile = insertQualityProfile();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("When providing a quality profile key, neither of organization/language/name must be set");

    tester.newPostRequest("api/qualityprofiles", "delete")
      .setParam("profileName", profile.getName())
      .setParam("language", profile.getLanguage())
      .setParam("profileKey", profile.getKey())
      .setParam("organization", organization.getKey())
      .execute();
  }

  @Test
  public void fail_if_profile_does_not_exist() throws Exception {
    logInAsQProfileAdministrator();

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Quality Profile for language '"+xoo1.getKey()+"' and name 'Polop' does not exist in organization '"+organization.getKey()+"'");

    tester.newPostRequest("api/qualityprofiles", "delete")
      .setParam("profileName", "Polop")
      .setParam("language", xoo1.getKey())
      .setParam("organization", organization.getKey())
      .execute();
  }

  private void logInAsQProfileAdministrator() {
    logInAsQProfileAdministrator(organization.getUuid());
  }

  private void logInAsQProfileAdministrator(String organizationUuid) {
    userSessionRule
      .logIn()
      .addPermission(ADMINISTER_QUALITY_PROFILES, organizationUuid);
  }
}
