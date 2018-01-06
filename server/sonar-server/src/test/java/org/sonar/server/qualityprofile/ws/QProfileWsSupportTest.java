/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.qualityprofile.QualityProfileTesting;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.tester.UserSessionRule;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

public class QProfileWsSupportTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public DbTester db = DbTester.create();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private DefaultOrganizationProvider defaultOrgProvider = TestDefaultOrganizationProvider.from(db);
  private QProfileWsSupport underTest = new QProfileWsSupport(db.getDbClient(), userSession, defaultOrgProvider);

  @Test
  public void getProfile_returns_the_profile_specified_by_key() {
    QProfileDto profile = db.qualityProfiles().insert(db.getDefaultOrganization());

    QProfileDto loaded = underTest.getProfile(db.getSession(), QProfileReference.fromKey(profile.getKee()));

    assertThat(loaded.getKee()).isEqualTo(profile.getKee());
    assertThat(loaded.getOrganizationUuid()).isEqualTo(profile.getOrganizationUuid());
    assertThat(loaded.getLanguage()).isEqualTo(profile.getLanguage());
    assertThat(loaded.getName()).isEqualTo(profile.getName());
  }

  @Test
  public void getProfile_throws_NotFoundException_if_specified_key_does_not_exist() {
    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Quality Profile with key 'missing' does not exist");

    underTest.getProfile(db.getSession(), QProfileReference.fromKey("missing"));
  }

  @Test
  public void getProfile_returns_the_profile_specified_by_name_and_default_organization() {
    QProfileDto profile = QualityProfileTesting.newQualityProfileDto().setOrganizationUuid(db.getDefaultOrganization().getUuid());
    db.qualityProfiles().insert(profile);

    QProfileDto loaded = underTest.getProfile(db.getSession(), QProfileReference.fromName(null, profile.getLanguage(), profile.getName()));

    assertThat(loaded.getKee()).isEqualTo(profile.getKee());
    assertThat(loaded.getOrganizationUuid()).isEqualTo(profile.getOrganizationUuid());
    assertThat(loaded.getLanguage()).isEqualTo(profile.getLanguage());
    assertThat(loaded.getName()).isEqualTo(profile.getName());
  }

  @Test
  public void getProfile_throws_NotFoundException_if_specified_name_does_not_exist_on_default_organization() {
    QProfileDto profile = QualityProfileTesting.newQualityProfileDto().setOrganizationUuid(db.getDefaultOrganization().getUuid());
    db.qualityProfiles().insert(profile);

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Quality Profile for language 'java' and name 'missing' does not exist");

    underTest.getProfile(db.getSession(), QProfileReference.fromName(null, "java", "missing"));
  }

  @Test
  public void getProfile_throws_NotFoundException_if_specified_name_does_not_exist_on_specified_organization() {
    OrganizationDto org1 = db.organizations().insert();
    QProfileDto profile = QualityProfileTesting.newQualityProfileDto().setOrganizationUuid(org1.getUuid());
    db.qualityProfiles().insert(profile);
    OrganizationDto org2 = db.organizations().insert();

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage(format("Quality Profile for language '%s' and name '%s' does not exist in organization '%s'", profile.getLanguage(), profile.getName(), org2.getKey()));

    underTest.getProfile(db.getSession(), QProfileReference.fromName(org2.getKey(), profile.getLanguage(), profile.getName()));
  }

  @Test
  public void getProfile_throws_NotFoundException_if_specified_organization_does_not_exist() {
    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("No organization with key 'the-org'");

    underTest.getProfile(db.getSession(), QProfileReference.fromName("the-org", "java", "the-name"));
  }
}
