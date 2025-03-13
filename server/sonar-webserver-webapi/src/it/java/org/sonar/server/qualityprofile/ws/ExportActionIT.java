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

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.language.LanguageTesting;
import org.sonar.server.qualityprofile.QProfileBackuper;
import org.sonar.server.qualityprofile.QProfileRestoreSummary;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ExportActionIT {

  private static final String XOO_LANGUAGE = "xoo";
  private static final String JAVA_LANGUAGE = "java";

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private final DbClient dbClient = db.getDbClient();
  private final QProfileBackuper backuper = new TestBackuper();

  @Test
  public void return_backup() {
    QProfileDto profile = createProfile(false);

    String result = newWsActionTester().newRequest()
      .setParam("language", profile.getLanguage())
      .setParam("qualityProfile", profile.getName())
      .execute()
      .getInput();

    assertThat(result).isEqualTo("Backup of " + profile.getLanguage() + "/" + profile.getKee());
  }

  @Test
  public void throw_NotFoundException_if_profile_with_specified_name_does_not_exist() {
    assertThatThrownBy(() -> {
      newWsActionTester().newRequest()
        .setParam("language", XOO_LANGUAGE)
        .setParam("exporterKey", "polop").execute();
    })
      .isInstanceOf(NotFoundException.class);
  }

  @Test
  public void definition() {
    WebService.Action definition = newWsActionTester().getDef();

    assertThat(definition.isPost()).isFalse();
    assertThat(definition.isInternal()).isFalse();
    assertThat(definition.params()).extracting(WebService.Param::key).containsExactlyInAnyOrder("language", "qualityProfile");

    WebService.Param name = definition.param("qualityProfile");
    assertThat(name.deprecatedSince()).isNullOrEmpty();

    WebService.Param language = definition.param("language");
    assertThat(language.deprecatedSince()).isNullOrEmpty();
  }

  private QProfileDto createProfile(boolean isDefault) {
    QProfileDto profile = db.qualityProfiles().insert(p -> p.setLanguage(XOO_LANGUAGE));
    if (isDefault) {
      db.qualityProfiles().setAsDefault(profile);
    }
    return profile;
  }

  private WsActionTester newWsActionTester() {
    return new WsActionTester(new ExportAction(dbClient, backuper, LanguageTesting.newLanguages(XOO_LANGUAGE, JAVA_LANGUAGE)));
  }

  private static class TestBackuper implements QProfileBackuper {

    @Override
    public void backup(DbSession dbSession, QProfileDto profile, Writer backupWriter) {
      try {
        backupWriter.write(format("Backup of %s/%s", profile.getLanguage(), profile.getKee()));
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
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
      throw new UnsupportedOperationException();
    }
  }
}
