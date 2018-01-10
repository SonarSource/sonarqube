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

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import javax.annotation.Nullable;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.resources.Languages;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.language.LanguageTesting;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.qualityprofile.BulkChangeResult;
import org.sonar.server.qualityprofile.QProfileBackuper;
import org.sonar.server.qualityprofile.QProfileRestoreSummary;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;
import org.sonar.test.JsonAssert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_PROFILES;

public class RestoreActionTest {

  private static final String A_LANGUAGE = "xoo";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public DbTester db = DbTester.create();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private TestBackuper backuper = new TestBackuper();
  private DefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(db);
  private QProfileWsSupport wsSupport = new QProfileWsSupport(db.getDbClient(), userSession, defaultOrganizationProvider);
  private Languages languages = LanguageTesting.newLanguages(A_LANGUAGE);
  private WsActionTester tester = new WsActionTester(new RestoreAction(db.getDbClient(), backuper, languages, userSession, wsSupport));

  @Test
  public void test_definition() {
    WebService.Action definition = tester.getDef();

    assertThat(definition.key()).isEqualTo("restore");
    assertThat(definition.isPost()).isTrue();
    assertThat(definition.responseExampleAsString()).isNull();
    assertThat(definition.description()).isNotEmpty();

    // parameters
    assertThat(definition.params()).hasSize(2);
    WebService.Param backupParam = definition.param("backup");
    assertThat(backupParam.isRequired()).isTrue();
    assertThat(backupParam.since()).isNull();
    WebService.Param orgParam = definition.param("organization");
    assertThat(orgParam.isRequired()).isFalse();
    assertThat(orgParam.since()).isEqualTo("6.4");
  }

  @Test
  public void profile_is_restored_on_default_organization_with_the_name_provided_in_backup() {
    logInAsQProfileAdministrator(db.getDefaultOrganization());
    TestResponse response = restore("<backup/>", null);

    assertThat(backuper.restoredBackup).isEqualTo("<backup/>");
    assertThat(backuper.restoredSummary.getProfile().getName()).isEqualTo("the-name-in-backup");
    JsonAssert.assertJson(response.getInput()).isSimilarTo("{" +
      "  \"profile\": {" +
      "    \"organization\": \"" + db.getDefaultOrganization().getKey() + "\"," +
      "    \"name\": \"the-name-in-backup\"," +
      "    \"language\": \"xoo\"," +
      "    \"languageName\": \"Xoo\"," +
      "    \"isDefault\": false," +
      "    \"isInherited\": false" +
      "  }," +
      "  \"ruleSuccesses\": 0," +
      "  \"ruleFailures\": 0" +
      "}");
  }

  @Test
  public void profile_is_restored_on_specified_organization_with_the_name_provided_in_backup() {
    OrganizationDto org = db.organizations().insert();
    logInAsQProfileAdministrator(org);
    TestResponse response = restore("<backup/>", org.getKey());

    assertThat(backuper.restoredBackup).isEqualTo("<backup/>");
    assertThat(backuper.restoredSummary.getProfile().getName()).isEqualTo("the-name-in-backup");
    JsonAssert.assertJson(response.getInput()).isSimilarTo("{" +
      "  \"profile\": {" +
      "    \"organization\": \"" + org.getKey() + "\"," +
      "    \"name\": \"the-name-in-backup\"," +
      "    \"language\": \"xoo\"," +
      "    \"languageName\": \"Xoo\"," +
      "    \"isDefault\": false," +
      "    \"isInherited\": false" +
      "  }," +
      "  \"ruleSuccesses\": 0," +
      "  \"ruleFailures\": 0" +
      "}");

  }

  @Test
  public void throw_IAE_if_backup_is_missing() {
    logInAsQProfileAdministrator(db.getDefaultOrganization());

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("A backup file must be provided");

    tester.newRequest()
      .setMethod("POST")
      .execute();
  }

  @Test
  public void throw_ForbiddenException_if_not_profile_administrator_of_default_organization() {
    userSession.logIn();

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    restore("<backup/>", null);
  }

  @Test
  public void throw_ForbiddenException_if_not_profile_administrator_of_specified_organization() {
    OrganizationDto org = db.organizations().insert();
    logInAsQProfileAdministrator(db.getDefaultOrganization());

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    restore("<backup/>", org.getKey());
  }

  @Test
  public void throw_NotFoundException_if_specified_organization_does_not_exist() {
    userSession.logIn();

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("No organization with key 'missing'");

    restore("<backup/>", "missing");
  }

  @Test
  public void throw_UnauthorizedException_if_not_logged_in() {
    userSession.anonymous();

    expectedException.expect(UnauthorizedException.class);
    expectedException.expectMessage("Authentication is required");

    restore("<backup/>", null);
  }

  private void logInAsQProfileAdministrator(OrganizationDto org) {
    userSession
      .logIn()
      .addPermission(ADMINISTER_QUALITY_PROFILES, org);
  }

  private TestResponse restore(String backupContent, @Nullable String organizationKey) {
    TestRequest request = tester.newRequest()
      .setMethod("POST")
      .setParam("backup", backupContent);
    if (organizationKey != null) {
      request.setParam("organization", organizationKey);
    }
    return request.execute();
  }

  private static class TestBackuper implements QProfileBackuper {

    private String restoredBackup;
    private QProfileRestoreSummary restoredSummary;

    @Override
    public void backup(DbSession dbSession, QProfileDto profile, Writer backupWriter) {
      throw new UnsupportedOperationException();
    }

    @Override
    public QProfileRestoreSummary restore(DbSession dbSession, Reader backup, OrganizationDto organization, @Nullable String overriddenProfileName) {
      if (restoredSummary != null) {
        throw new IllegalStateException("Already restored");
      }
      try {
        restoredBackup = IOUtils.toString(backup);
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
      QProfileDto profile = new QProfileDto()
        .setKee("P1")
        .setRulesProfileUuid("rp-P1")
        .setLanguage("xoo")
        .setName(overriddenProfileName != null ? overriddenProfileName : "the-name-in-backup");
      restoredSummary = new QProfileRestoreSummary(profile, new BulkChangeResult());
      return restoredSummary;
    }

    @Override
    public QProfileRestoreSummary restore(DbSession dbSession, Reader backup, QProfileDto profile) {
      throw new UnsupportedOperationException();
    }
  }
}
