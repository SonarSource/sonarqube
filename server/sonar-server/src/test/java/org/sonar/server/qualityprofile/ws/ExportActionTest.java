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

import java.io.IOException;
import java.io.Writer;
import org.apache.commons.lang.StringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.profiles.ProfileExporter;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.qualityprofile.QualityProfileDao;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.language.LanguageTesting;
import org.sonar.server.qualityprofile.QProfileBackuper;
import org.sonar.server.qualityprofile.QProfileExporters;
import org.sonar.server.qualityprofile.QProfileTesting;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

public class ExportActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private DbClient dbClient = db.getDbClient();
  private DbSession session = db.getSession();
  private QualityProfileDao qualityProfileDao = dbClient.qualityProfileDao();
  private QProfileBackuper backuper = mock(QProfileBackuper.class);

  @Test
  public void export_without_format() throws Exception {
    QualityProfileDto profile = db.qualityProfiles().insertQualityProfile(QProfileTesting.newXooP1());

    doAnswer(invocation -> {
      invocation.getArgumentAt(2, Writer.class).write("As exported by SQ !");
      return null;
    }).when(backuper).backup(any(DbSession.class), any(QualityProfileDto.class), any(Writer.class));

    String result = newWsActionTester().newRequest().setParam("language", profile.getLanguage()).setParam("name", profile.getName())
      .execute().getInput();

    assertThat(result).isEqualTo("As exported by SQ !");
  }

  @Test
  public void export_with_format() throws Exception {
    QualityProfileDto profile = db.qualityProfiles().insertQualityProfile(QProfileTesting.newXooP1());

    String result = newWsActionTester(newExporter("polop"), newExporter("palap")).newRequest().setParam("language", profile.getLanguage()).setParam("name", profile.getName())
      .setParam("exporterKey", "polop").execute()
      .getInput();

    assertThat(result).isEqualTo("Profile " + profile.getName() + " exported by polop");
  }

  @Test
  public void export_default_profile() throws Exception {
    db.qualityProfiles().insertQualityProfiles(QProfileTesting.newXooP1(), QProfileTesting.newXooP2().setName("SonarWay").setDefault(true));

    String result = newWsActionTester(newExporter("polop"), newExporter("palap")).newRequest().setParam("language", "xoo").setParam("exporterKey", "polop").execute().getInput();

    assertThat(result).isEqualTo("Profile SonarWay exported by polop");
  }

  @Test
  public void fail_on_unknown_profile() throws Exception {
    expectedException.expect(NotFoundException.class);
    newWsActionTester(newExporter("polop"), newExporter("palap")).newRequest().setParam("language", "xoo").setParam("exporterKey", "polop").execute();
  }

  @Test
  public void fail_on_unknown_exporter() throws Exception {
    db.qualityProfiles().insertQualityProfile(QProfileTesting.newXooP1());

    expectedException.expect(IllegalArgumentException.class);
    newWsActionTester(newExporter("polop"), newExporter("palap")).newRequest().setParam("language", "xoo").setParam("exporterKey", "unknown").execute();
  }

  @Test
  public void does_not_fail_when_no_exporters() throws Exception {
    QualityProfileDto profile = db.qualityProfiles().insertQualityProfile(QProfileTesting.newXooP1());

    newWsActionTester().newRequest().setParam("language", "xoo").setParam("name", profile.getName()).execute();
  }

  @Test
  public void test_definition() throws Exception {
    WebService.Action action = newWsActionTester(newExporter("polop"), newExporter("palap")).getDef();
    assertThat(action.isPost()).isFalse();
    assertThat(action.isInternal()).isFalse();
    assertThat(action.params()).hasSize(3);
    assertThat(action.param("exporterKey").possibleValues()).containsOnly("polop", "palap");
  }

  private WsActionTester newWsActionTester(ProfileExporter... profileExporters) {
    QProfileExporters exporters = new QProfileExporters(dbClient, null, null, profileExporters, null);
    return new WsActionTester(new ExportAction(dbClient, backuper, exporters, LanguageTesting.newLanguages("xoo")));
  }

  private static ProfileExporter

    newExporter(final String key) {
    return new ProfileExporter(key, StringUtils.capitalize(key)) {
      @Override
      public String getMimeType() {
        return "text/plain+" + key;
      }

      @Override
      public void exportProfile(RulesProfile profile, Writer writer) {
        try {
          writer.write(String.format("Profile %s exported by %s", profile.getName(), key));
        } catch (IOException ioe) {
          throw new RuntimeException(ioe);
        }
      }
    };
  }
}
