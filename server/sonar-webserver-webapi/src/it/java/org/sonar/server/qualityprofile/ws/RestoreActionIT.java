/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import org.sonar.api.resources.Languages;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.language.LanguageTesting;
import org.sonar.server.qualityprofile.BulkChangeResult;
import org.sonar.server.qualityprofile.QProfileBackuper;
import org.sonar.server.qualityprofile.QProfileRestoreSummary;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;
import org.sonar.test.JsonAssert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER_QUALITY_PROFILES;

public class RestoreActionIT {

  private static final String A_LANGUAGE = "xoo";

  @Rule
  public DbTester db = DbTester.create();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private final TestBackuper backuper = new TestBackuper();
  private final Languages languages = LanguageTesting.newLanguages(A_LANGUAGE);
  private final WsActionTester tester = new WsActionTester(new RestoreAction(db.getDbClient(), backuper, languages, userSession));

  @Test
  public void test_definition() {
    WebService.Action definition = tester.getDef();

    assertThat(definition.key()).isEqualTo("restore");
    assertThat(definition.isPost()).isTrue();
    assertThat(definition.responseExampleAsString()).isNull();
    assertThat(definition.description()).isNotEmpty();

    // parameters
    assertThat(definition.params()).hasSize(1);
    WebService.Param backupParam = definition.param("backup");
    assertThat(backupParam.isRequired()).isTrue();
    assertThat(backupParam.since()).isNull();
  }

  @Test
  public void profile_is_restored_with_the_name_provided_in_backup() {
    logInAsQProfileAdministrator();
    TestResponse response = restore("<backup/>");

    assertThat(backuper.restoredBackup).isEqualTo("<backup/>");
    assertThat(backuper.restoredSummary.profile().getName()).isEqualTo("the-name-in-backup");
    JsonAssert.assertJson(response.getInput()).isSimilarTo("{" +
      "  \"profile\": {" +
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
    logInAsQProfileAdministrator();

    assertThatThrownBy(() -> {
      tester.newRequest()
        .setMethod("POST")
        .execute();
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("A backup file must be provided");
  }

  @Test
  public void throw_ForbiddenException_if_not_profile_administrator() {
    userSession.logIn();

    assertThatThrownBy(() -> {
      restore("<backup/>");
    })
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

  @Test
  public void throw_UnauthorizedException_if_not_logged_in() {
    userSession.anonymous();

    assertThatThrownBy(() -> {
      restore("<backup/>");
    })
      .isInstanceOf(UnauthorizedException.class)
      .hasMessage("Authentication is required");
  }

  private void logInAsQProfileAdministrator() {
    userSession
      .logIn()
      .addPermission(ADMINISTER_QUALITY_PROFILES);
  }

  private TestResponse restore(String backupContent) {
    TestRequest request = tester.newRequest()
      .setMethod("POST")
      .setParam("backup", backupContent);
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
    public QProfileRestoreSummary restore(DbSession dbSession, Reader backup, @Nullable String overriddenProfileName) {
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

    @Override
    public QProfileRestoreSummary copy(DbSession dbSession, QProfileDto from, QProfileDto to) {
      throw new UnsupportedOperationException();
    }
  }
}
