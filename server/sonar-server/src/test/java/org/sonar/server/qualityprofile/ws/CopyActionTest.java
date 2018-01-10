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

import java.io.Reader;
import java.io.Writer;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.resources.Languages;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.internal.JUnitTempFolder;
import org.sonar.core.util.SequenceUuidFactory;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.permission.OrganizationPermission;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.language.LanguageTesting;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.qualityprofile.BulkChangeResult;
import org.sonar.server.qualityprofile.QProfileBackuper;
import org.sonar.server.qualityprofile.QProfileCopier;
import org.sonar.server.qualityprofile.QProfileFactory;
import org.sonar.server.qualityprofile.QProfileFactoryImpl;
import org.sonar.server.qualityprofile.QProfileRestoreSummary;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_PROFILES;
import static org.sonar.test.JsonAssert.assertJson;

public class CopyActionTest {

  private static final String A_LANGUAGE = "lang1";

  @Rule
  public DbTester db = DbTester.create();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public JUnitTempFolder tempDir = new JUnitTempFolder();
  private ActiveRuleIndexer activeRuleIndexer = mock(ActiveRuleIndexer.class);
  private QProfileFactory profileFactory = new QProfileFactoryImpl(db.getDbClient(), new SequenceUuidFactory(), System2.INSTANCE, activeRuleIndexer);
  private TestBackuper backuper = new TestBackuper();
  private QProfileCopier profileCopier = new QProfileCopier(db.getDbClient(), profileFactory, backuper, tempDir);
  private DefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(db);
  private Languages languages = LanguageTesting.newLanguages(A_LANGUAGE);
  private QProfileWsSupport wsSupport = new QProfileWsSupport(db.getDbClient(), userSession, defaultOrganizationProvider);
  private CopyAction underTest = new CopyAction(db.getDbClient(), profileCopier, languages, userSession, wsSupport);
  private WsActionTester tester = new WsActionTester(underTest);

  @Test
  public void test_definition() {
    WebService.Action definition = tester.getDef();

    assertThat(definition.key()).isEqualTo("copy");
    assertThat(definition.isInternal()).isFalse();
    assertThat(definition.responseExampleAsString()).isNotEmpty();
    assertThat(definition.since()).isEqualTo("5.2");
    assertThat(definition.isPost()).isTrue();

    assertThat(definition.params()).extracting(WebService.Param::key).containsOnly("fromKey", "toName");
    assertThat(definition.param("fromKey").isRequired()).isTrue();
    assertThat(definition.param("toName").isRequired()).isTrue();
  }

  @Test
  public void example() {
    OrganizationDto organization = db.organizations().insert();
    logInAsQProfileAdministrator(organization);
    QProfileDto parent = db.qualityProfiles().insert(organization, p -> p.setKee("AU-TpxcA-iU5OvuD2FL2"));
    QProfileDto profile = db.qualityProfiles().insert(organization, p -> p.setKee("old")
      .setLanguage("Java")
      .setParentKee(parent.getKee()));
    String profileUuid = profile.getRulesProfileUuid();

    String response = tester.newRequest()
      .setMethod("POST")
      .setParam("fromKey", profile.getKee())
      .setParam("toName", "My New Profile")
      .execute()
      .getInput();

    assertJson(response).ignoreFields("key").isSimilarTo(getClass().getResource("copy-example.json"));
  }

  @Test
  public void create_profile_with_specified_name_and_copy_rules_from_source_profile() {
    OrganizationDto organization = db.organizations().insert();
    logInAsQProfileAdministrator(organization);

    QProfileDto sourceProfile = db.qualityProfiles().insert(organization, p -> p.setLanguage(A_LANGUAGE));
    TestResponse response = tester.newRequest()
      .setMethod("POST")
      .setParam("fromKey", sourceProfile.getKee())
      .setParam("toName", "target-name")
      .execute();

    String generatedUuid = "1";
    assertJson(response.getInput()).isSimilarTo("{" +
      "  \"key\": \"" + generatedUuid + "\"," +
      "  \"name\": \"target-name\"," +
      "  \"language\": \"lang1\"," +
      "  \"languageName\": \"Lang1\"," +
      "  \"isDefault\": false," +
      "  \"isInherited\": false" +
      "}");
    QProfileDto loadedProfile = db.getDbClient().qualityProfileDao().selectByNameAndLanguage(db.getSession(), organization, "target-name", sourceProfile.getLanguage());
    assertThat(loadedProfile.getKee()).isEqualTo(generatedUuid);
    assertThat(loadedProfile.getParentKee()).isNull();

    assertThat(backuper.backupedProfile.getKee()).isEqualTo(sourceProfile.getKee());
    assertThat(backuper.restoredProfile.getOrganizationUuid()).isEqualTo(sourceProfile.getOrganizationUuid());
    assertThat(backuper.restoredProfile.getLanguage()).isEqualTo(sourceProfile.getLanguage());
    assertThat(backuper.restoredProfile.getName()).isEqualTo("target-name");
    assertThat(backuper.restoredProfile.getKee()).isEqualTo(generatedUuid);
    assertThat(backuper.restoredProfile.getParentKee()).isNull();
  }

  @Test
  public void copy_rules_on_existing_profile_in_default_organization() {
    OrganizationDto organization = db.organizations().insert();
    logInAsQProfileAdministrator(organization);
    QProfileDto sourceProfile = db.qualityProfiles().insert(organization, p -> p.setLanguage(A_LANGUAGE));
    QProfileDto targetProfile = db.qualityProfiles().insert(organization, p -> p.setLanguage(A_LANGUAGE));

    TestResponse response = tester.newRequest()
      .setMethod("POST")
      .setParam("fromKey", sourceProfile.getKee())
      .setParam("toName", targetProfile.getName())
      .execute();

    assertJson(response.getInput()).isSimilarTo("{" +
      "  \"key\": \"" + targetProfile.getKee() + "\"," +
      "  \"name\": \"" + targetProfile.getName() + "\"," +
      "  \"language\": \"lang1\"," +
      "  \"languageName\": \"Lang1\"," +
      "  \"isDefault\": false," +
      "  \"isInherited\": false" +
      "}");
    QProfileDto loadedProfile = db.getDbClient().qualityProfileDao().selectByUuid(db.getSession(), targetProfile.getKee());
    assertThat(loadedProfile).isNotNull();

    assertThat(backuper.backupedProfile.getKee()).isEqualTo(sourceProfile.getKee());
    assertThat(backuper.restoredProfile.getKee()).isEqualTo(targetProfile.getKee());
  }

  @Test
  public void create_profile_with_same_parent_as_source_profile() {
    OrganizationDto organization = db.organizations().insert();
    logInAsQProfileAdministrator(organization);

    QProfileDto parentProfile = db.qualityProfiles().insert(organization, p -> p.setLanguage(A_LANGUAGE));
    QProfileDto sourceProfile = db.qualityProfiles().insert(organization, p -> p.setLanguage(A_LANGUAGE).setParentKee(parentProfile.getKee()));

    TestResponse response = tester.newRequest()
      .setMethod("POST")
      .setParam("fromKey", sourceProfile.getKee())
      .setParam("toName", "target-name")
      .execute();

    String generatedUuid = "1";
    assertJson(response.getInput()).isSimilarTo("{" +
      "  \"key\": \"" + generatedUuid + "\"," +
      "  \"name\": \"target-name\"," +
      "  \"language\": \"lang1\"," +
      "  \"languageName\": \"Lang1\"," +
      "  \"isDefault\": false," +
      "  \"isInherited\": true" +
      "}");
    QProfileDto loadedProfile = db.getDbClient().qualityProfileDao().selectByNameAndLanguage(db.getSession(), organization, "target-name", sourceProfile.getLanguage());
    assertThat(loadedProfile.getKee()).isEqualTo(generatedUuid);
    assertThat(loadedProfile.getParentKee()).isEqualTo(parentProfile.getKee());

    assertThat(backuper.backupedProfile.getKee()).isEqualTo(sourceProfile.getKee());
    assertThat(backuper.restoredProfile.getOrganizationUuid()).isEqualTo(sourceProfile.getOrganizationUuid());
    assertThat(backuper.restoredProfile.getLanguage()).isEqualTo(sourceProfile.getLanguage());
    assertThat(backuper.restoredProfile.getName()).isEqualTo("target-name");
    assertThat(backuper.restoredProfile.getKee()).isEqualTo(generatedUuid);
    assertThat(backuper.restoredProfile.getParentKee()).isEqualTo(parentProfile.getKee());
  }

  @Test
  public void throw_UnauthorizedException_if_not_logged_in() {
    userSession.anonymous();

    expectedException.expect(UnauthorizedException.class);
    expectedException.expectMessage("Authentication is required");

    tester.newRequest()
      .setMethod("POST")
      .setParam("fromKey", "foo")
      .setParam("toName", "bar")
      .execute();
  }

  @Test
  public void throw_ForbiddenException_if_not_profile_administrator_of_organization() {
    OrganizationDto organization = db.organizations().insert();
    QProfileDto profile = db.qualityProfiles().insert(organization, p -> p.setLanguage(A_LANGUAGE));
    userSession.logIn().addPermission(OrganizationPermission.SCAN, organization);

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    tester.newRequest()
      .setMethod("POST")
      .setParam("fromKey", profile.getKee())
      .setParam("toName", "bar")
      .execute();
  }

  @Test
  public void throw_ForbiddenException_if_not_profile_administrator() {
    OrganizationDto organization = db.organizations().insert();
    QProfileDto profile = db.qualityProfiles().insert(organization, p -> p.setLanguage(A_LANGUAGE));
    userSession.logIn().addPermission(OrganizationPermission.SCAN, organization);

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    tester.newRequest()
      .setMethod("POST")
      .setParam("fromKey", profile.getKee())
      .setParam("toName", "bar")
      .execute();
  }

  @Test
  public void fail_if_parameter_fromKey_is_missing() {
    OrganizationDto organization = db.organizations().insert();
    logInAsQProfileAdministrator(organization);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The 'fromKey' parameter is missing");

    tester.newRequest()
      .setParam("toName", "bar")
      .execute();
  }

  @Test
  public void fail_if_parameter_toName_is_missing() {
    OrganizationDto organization = db.organizations().insert();
    logInAsQProfileAdministrator(organization);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The 'toName' parameter is missing");

    tester.newRequest()
      .setParam("fromKey", "foo")
      .execute();
  }

  private void logInAsQProfileAdministrator(OrganizationDto organization) {
    userSession
      .logIn()
      .addPermission(ADMINISTER_QUALITY_PROFILES, organization);
  }

  private static class TestBackuper implements QProfileBackuper {

    private QProfileDto backupedProfile;
    private QProfileDto restoredProfile;

    @Override
    public void backup(DbSession dbSession, QProfileDto profile, Writer backupWriter) {
      if (this.backupedProfile != null) {
        throw new IllegalStateException("Already backup-ed/backed-up");
      }
      this.backupedProfile = profile;
    }

    @Override
    public QProfileRestoreSummary restore(DbSession dbSession, Reader backup, OrganizationDto organization, @Nullable String overriddenProfileName) {
      throw new UnsupportedOperationException();
    }

    @Override
    public QProfileRestoreSummary restore(DbSession dbSession, Reader backup, QProfileDto profile) {
      if (this.restoredProfile != null) {
        throw new IllegalStateException("Already restored");
      }
      this.restoredProfile = profile;
      return new QProfileRestoreSummary(profile, new BulkChangeResult());
    }
  }
}
