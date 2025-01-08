/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
import org.sonar.api.impl.utils.JUnitTempFolder;
import org.sonar.api.resources.Languages;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.core.util.SequenceUuidFactory;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.language.LanguageTesting;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.sonar.core.util.SequenceUuidFactory.UUID_1;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER_QUALITY_PROFILES;
import static org.sonar.test.JsonAssert.assertJson;

public class CopyActionIT {

  private static final String A_LANGUAGE = "lang1";

  @Rule
  public DbTester db = DbTester.create();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public JUnitTempFolder tempDir = new JUnitTempFolder();
  private final ActiveRuleIndexer activeRuleIndexer = mock(ActiveRuleIndexer.class);
  private final TestBackuper backuper = new TestBackuper();
  private final QProfileFactory profileFactory = new QProfileFactoryImpl(db.getDbClient(), new SequenceUuidFactory(), System2.INSTANCE, activeRuleIndexer);
  private final QProfileCopier profileCopier = new QProfileCopier(db.getDbClient(), profileFactory, backuper);
  private final Languages languages = LanguageTesting.newLanguages(A_LANGUAGE);
  private final QProfileWsSupport wsSupport = new QProfileWsSupport(db.getDbClient(), userSession);
  private final CopyAction underTest = new CopyAction(db.getDbClient(), profileCopier, languages, userSession, wsSupport);
  private final WsActionTester tester = new WsActionTester(underTest);

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
    logInAsQProfileAdministrator();
    QProfileDto parent = db.qualityProfiles().insert(p -> p.setKee("AU-TpxcA-iU5OvuD2FL2"));
    QProfileDto profile = db.qualityProfiles().insert(p -> p.setKee("old")
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
    logInAsQProfileAdministrator();

    QProfileDto sourceProfile = db.qualityProfiles().insert(p -> p.setLanguage(A_LANGUAGE));
    TestResponse response = tester.newRequest()
      .setMethod("POST")
      .setParam("fromKey", sourceProfile.getKee())
      .setParam("toName", "target-name")
      .execute();

    assertJson(response.getInput()).isSimilarTo("{" +
      "  \"key\": \"" + UUID_1 + "\"," +
      "  \"name\": \"target-name\"," +
      "  \"language\": \"lang1\"," +
      "  \"languageName\": \"Lang1\"," +
      "  \"isDefault\": false," +
      "  \"isInherited\": false" +
      "}");
    QProfileDto loadedProfile = db.getDbClient().qualityProfileDao().selectByNameAndLanguage(db.getSession(), "target-name", sourceProfile.getLanguage());
    assertThat(loadedProfile.getKee()).isEqualTo(UUID_1);
    assertThat(loadedProfile.getParentKee()).isNull();

    assertThat(backuper.copiedProfile.getKee()).isEqualTo(sourceProfile.getKee());
    assertThat(backuper.toProfile.getLanguage()).isEqualTo(sourceProfile.getLanguage());
    assertThat(backuper.toProfile.getName()).isEqualTo("target-name");
    assertThat(backuper.toProfile.getKee()).isEqualTo(UUID_1);
    assertThat(backuper.toProfile.getParentKee()).isNull();
  }

  @Test
  public void copy_rules_on_existing_profile() {
    logInAsQProfileAdministrator();
    QProfileDto sourceProfile = db.qualityProfiles().insert(p -> p.setLanguage(A_LANGUAGE));
    QProfileDto targetProfile = db.qualityProfiles().insert(p -> p.setLanguage(A_LANGUAGE));

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

    assertThat(backuper.copiedProfile.getKee()).isEqualTo(sourceProfile.getKee());
    assertThat(backuper.toProfile.getKee()).isEqualTo(targetProfile.getKee());
  }

  @Test
  public void create_profile_with_same_parent_as_source_profile() {
    logInAsQProfileAdministrator();

    QProfileDto parentProfile = db.qualityProfiles().insert(p -> p.setLanguage(A_LANGUAGE));
    QProfileDto sourceProfile = db.qualityProfiles().insert(p -> p.setLanguage(A_LANGUAGE).setParentKee(parentProfile.getKee()));

    TestResponse response = tester.newRequest()
      .setMethod("POST")
      .setParam("fromKey", sourceProfile.getKee())
      .setParam("toName", "target-name")
      .execute();

    assertJson(response.getInput()).isSimilarTo("{" +
      "  \"key\": \"" + UUID_1 + "\"," +
      "  \"name\": \"target-name\"," +
      "  \"language\": \"lang1\"," +
      "  \"languageName\": \"Lang1\"," +
      "  \"isDefault\": false," +
      "  \"isInherited\": true" +
      "}");
    QProfileDto loadedProfile = db.getDbClient().qualityProfileDao().selectByNameAndLanguage(db.getSession(), "target-name", sourceProfile.getLanguage());
    assertThat(loadedProfile.getKee()).isEqualTo(UUID_1);
    assertThat(loadedProfile.getParentKee()).isEqualTo(parentProfile.getKee());

    assertThat(backuper.copiedProfile.getKee()).isEqualTo(sourceProfile.getKee());
    assertThat(backuper.toProfile.getLanguage()).isEqualTo(sourceProfile.getLanguage());
    assertThat(backuper.toProfile.getName()).isEqualTo("target-name");
    assertThat(backuper.toProfile.getKee()).isEqualTo(UUID_1);
    assertThat(backuper.toProfile.getParentKee()).isEqualTo(parentProfile.getKee());
  }

  @Test
  public void throw_UnauthorizedException_if_not_logged_in() {
    userSession.anonymous();

    assertThatThrownBy(() -> {
      tester.newRequest()
        .setMethod("POST")
        .setParam("fromKey", "foo")
        .setParam("toName", "bar")
        .execute();
    })
      .isInstanceOf(UnauthorizedException.class)
      .hasMessage("Authentication is required");
  }

  @Test
  public void throw_ForbiddenException_if_not_profile_global_administrator() {
    QProfileDto profile = db.qualityProfiles().insert(p -> p.setLanguage(A_LANGUAGE));
    userSession.logIn().addPermission(GlobalPermission.SCAN);

    assertThatThrownBy(() -> {
      tester.newRequest()
        .setMethod("POST")
        .setParam("fromKey", profile.getKee())
        .setParam("toName", "bar")
        .execute();
    })
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

  @Test
  public void throw_ForbiddenException_if_not_profile_administrator() {
    QProfileDto profile = db.qualityProfiles().insert(p -> p.setLanguage(A_LANGUAGE));
    userSession.logIn().addPermission(GlobalPermission.SCAN);

    assertThatThrownBy(() -> {
      tester.newRequest()
        .setMethod("POST")
        .setParam("fromKey", profile.getKee())
        .setParam("toName", "bar")
        .execute();
    })
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

  @Test
  public void fail_if_parameter_fromKey_is_missing() {
    logInAsQProfileAdministrator();

    assertThatThrownBy(() -> {
      tester.newRequest()
        .setParam("toName", "bar")
        .execute();
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("The 'fromKey' parameter is missing");
  }

  @Test
  public void fail_if_parameter_toName_is_missing() {
    logInAsQProfileAdministrator();

    assertThatThrownBy(() -> {
      tester.newRequest()
        .setParam("fromKey", "foo")
        .execute();
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("The 'toName' parameter is missing");
  }

  private void logInAsQProfileAdministrator() {
    userSession
      .logIn()
      .addPermission(ADMINISTER_QUALITY_PROFILES);
  }

  private static class TestBackuper implements QProfileBackuper {

    private QProfileDto copiedProfile;
    private QProfileDto toProfile;

    @Override
    public void backup(DbSession dbSession, QProfileDto profile, Writer backupWriter) {
      throw new UnsupportedOperationException();
    }

    @Override
    public QProfileRestoreSummary restore(DbSession dbSession, Reader backup, @Nullable String overriddenProfileName) {
      throw new UnsupportedOperationException();
    }

    @Override
    public QProfileRestoreSummary restore(DbSession dbSession, Reader backup, QProfileDto profile) {
      throw new UnsupportedOperationException();
    }

    @Override
    public QProfileRestoreSummary copy(DbSession dbSession, QProfileDto from, QProfileDto to) {
      this.copiedProfile = from;
      this.toProfile = to;
      return null;
    }
  }
}
