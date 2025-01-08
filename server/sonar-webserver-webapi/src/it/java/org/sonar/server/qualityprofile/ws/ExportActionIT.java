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
import org.apache.commons.lang3.StringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.profiles.ProfileExporter;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.language.LanguageTesting;
import org.sonar.server.qualityprofile.QProfileBackuper;
import org.sonar.server.qualityprofile.QProfileExporters;
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
  public void export_profile() {
    QProfileDto profile = createProfile(false);

    WsActionTester tester = newWsActionTester(newExporter("polop"), newExporter("palap"));
    String result = tester.newRequest()
      .setParam("language", profile.getLanguage())
      .setParam("qualityProfile", profile.getName())
      .setParam("exporterKey", "polop").execute()
      .getInput();

    assertThat(result).isEqualTo("Profile " + profile.getLanguage() + "/" + profile.getName() + " exported by polop");
  }

  @Test
  public void export_default_profile() {
    QProfileDto nonDefaultProfile = createProfile(false);
    QProfileDto defaultProfile = createProfile(true);

    WsActionTester tester = newWsActionTester(newExporter("polop"), newExporter("palap"));
    String result = tester.newRequest()
      .setParam("language", XOO_LANGUAGE)
      .setParam("exporterKey", "polop")
      .execute()
      .getInput();

    assertThat(result).isEqualTo("Profile " + defaultProfile.getLanguage() + "/" + defaultProfile.getName() + " exported by polop");
  }

  @Test
  public void return_backup_when_exporter_is_not_specified() {
    QProfileDto profile = createProfile(false);

    String result = newWsActionTester(newExporter("polop")).newRequest()
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
  public void throw_IAE_if_export_with_specified_key_does_not_exist() {
    QProfileDto profile = createProfile(true);

    assertThatThrownBy(() -> {
      newWsActionTester(newExporter("polop"), newExporter("palap")).newRequest()
        .setParam("language", XOO_LANGUAGE)
        .setParam("exporterKey", "unknown").execute();
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Value of parameter 'exporterKey' (unknown) must be one of: [polop, palap]");
  }

  @Test
  public void definition_without_exporters() {
    WebService.Action definition = newWsActionTester().getDef();

    assertThat(definition.isPost()).isFalse();
    assertThat(definition.isInternal()).isFalse();
    assertThat(definition.params()).extracting(WebService.Param::key).containsExactlyInAnyOrder("language", "qualityProfile");

    WebService.Param name = definition.param("qualityProfile");
    assertThat(name.deprecatedSince()).isNullOrEmpty();

    WebService.Param language = definition.param("language");
    assertThat(language.deprecatedSince()).isNullOrEmpty();
  }

  @Test
  public void definition_with_exporters() {
    WebService.Action definition = newWsActionTester(newExporter("polop"), newExporter("palap")).getDef();

    assertThat(definition.isPost()).isFalse();
    assertThat(definition.isInternal()).isFalse();
    assertThat(definition.params()).extracting("key").containsExactlyInAnyOrder("language", "qualityProfile", "exporterKey");
    WebService.Param exportersParam = definition.param("exporterKey");
    assertThat(exportersParam.possibleValues()).containsOnly("polop", "palap");
    assertThat(exportersParam.isInternal()).isFalse();
  }

  private QProfileDto createProfile(boolean isDefault) {
    QProfileDto profile = db.qualityProfiles().insert(p -> p.setLanguage(XOO_LANGUAGE));
    if (isDefault) {
      db.qualityProfiles().setAsDefault(profile);
    }
    return profile;
  }

  private WsActionTester newWsActionTester(ProfileExporter... profileExporters) {
    QProfileExporters exporters = new QProfileExporters(dbClient, null, null, profileExporters, null);
    return new WsActionTester(new ExportAction(dbClient, backuper, exporters, LanguageTesting.newLanguages(XOO_LANGUAGE, JAVA_LANGUAGE)));
  }

  private static ProfileExporter newExporter(String key) {
    return new ProfileExporter(key, StringUtils.capitalize(key)) {
      @Override
      public String getMimeType() {
        return "text/plain+" + key;
      }

      @Override
      public void exportProfile(RulesProfile profile, Writer writer) {
        try {
          writer.write(format("Profile %s/%s exported by %s", profile.getLanguage(), profile.getName(), key));
        } catch (IOException ioe) {
          throw new RuntimeException(ioe);
        }
      }
    };
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
