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

import java.io.Reader;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.resources.Languages;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.language.LanguageTesting;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.qualityprofile.BulkChangeResult;
import org.sonar.server.qualityprofile.QProfileBackuper;
import org.sonar.server.qualityprofile.QProfileName;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;
import org.sonar.test.JsonAssert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_PROFILES;

public class RestoreActionTest {

  private static final String A_LANGUAGE = "xoo";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public DbTester db = DbTester.create();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private QProfileBackuper backuper = mock(QProfileBackuper.class);
  private DefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(db);
  private QProfileWsSupport wsSupport = new QProfileWsSupport(db.getDbClient(), userSession, defaultOrganizationProvider);
  private Languages languages = LanguageTesting.newLanguages(A_LANGUAGE);
  private WsActionTester tester = new WsActionTester(new RestoreAction(db.getDbClient(), backuper, languages, wsSupport));

  @Test
  public void test_definition() {
    WebService.Action definition = tester.getDef();

    assertThat(definition.key()).isEqualTo("restore");
    assertThat(definition.isPost()).isTrue();
    assertThat(definition.responseExampleAsString()).isNull();
    assertThat(definition.description()).isNotEmpty();

    // parameters
    assertThat(definition.params()).hasSize(1);
    assertThat(definition.param("backup").isRequired()).isTrue();
  }

  @Test
  public void restore_the_uploaded_backup_on_default_organization() throws Exception {
    QualityProfileDto profile = QualityProfileDto.createFor("P1")
      .setDefault(false).setLanguage("xoo").setName("Sonar way");
    BulkChangeResult restoreResult = new BulkChangeResult(profile);
    when(backuper.restore(any(DbSession.class), any(Reader.class), any(QProfileName.class))).thenReturn(restoreResult);

    logInAsQProfileAdministrator(db.getDefaultOrganization());
    TestResponse response = restore("<backup/>");

    JsonAssert.assertJson(response.getInput()).isSimilarTo(getClass().getResource("RestoreActionTest/restore_profile.json"));
    verify(backuper).restore(any(DbSession.class), any(Reader.class), any(QProfileName.class));
  }

  @Test
  public void throw_IAE_if_backup_is_missing() throws Exception {
    logInAsQProfileAdministrator(db.getDefaultOrganization());

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("A backup file must be provided");

    tester.newRequest()
      .setMethod("POST")
      .execute();
  }

  @Test
  public void throw_ForbiddenException_if_not_profile_administrator() throws Exception {
    userSession.logIn();

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    restore("<backup/>");
  }

  @Test
  public void throw_UnauthorizedException_if_not_logged_in() throws Exception {
    userSession.anonymous();

    expectedException.expect(UnauthorizedException.class);
    expectedException.expectMessage("Authentication is required");

    restore("<backup/>");
  }

  private void logInAsQProfileAdministrator(OrganizationDto org) {
    userSession
      .logIn()
      .addPermission(ADMINISTER_QUALITY_PROFILES, org);
  }

  private TestResponse restore(String backupContent) {
    return tester.newRequest()
      .setMethod("POST")
      .setParam("backup", backupContent)
      .execute();
  }
}
