/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sonar.api.profiles.ProfileExporter;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.server.ws.WebService.Action;
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
import org.sonar.server.qualityprofile.QProfileFactory;
import org.sonar.server.qualityprofile.QProfileLoader;
import org.sonar.server.qualityprofile.QProfileTesting;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.ws.WsTester;
import org.sonar.server.ws.WsTester.Result;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

public class ExportActionTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  WsTester wsTester;

  DbClient dbClient = db.getDbClient();

  final DbSession session = db.getSession();

  QualityProfileDao qualityProfileDao = dbClient.qualityProfileDao();

  QProfileBackuper backuper;

  QProfileExporters exporters;

  @Before
  public void before() {
    backuper = mock(QProfileBackuper.class);

    ProfileExporter exporter1 = newExporter("polop");
    ProfileExporter exporter2 = newExporter("palap");

    exporters = new QProfileExporters(dbClient, new QProfileLoader(dbClient, null, mock(RuleIndex.class)), null, null, new ProfileExporter[] {exporter1, exporter2},
      null);
    wsTester = new WsTester(new QProfilesWs(mock(RuleActivationActions.class),
      mock(BulkRuleActivationActions.class),
      new ExportAction(dbClient, new QProfileFactory(dbClient), backuper, exporters, LanguageTesting.newLanguages("xoo"))));
  }

  private ProfileExporter newExporter(final String key) {
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

  @Test
  public void export_without_format() throws Exception {
    QualityProfileDto profile = QProfileTesting.newXooP1();
    qualityProfileDao.insert(session, profile);
    session.commit();

    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        invocation.getArgumentAt(1, Writer.class).write("As exported by SQ !");
        return null;
      }
    }).when(backuper).backup(Matchers.eq(profile.getKey()), Matchers.any(Writer.class));

    Result result = wsTester.newGetRequest("api/qualityprofiles", "export").setParam("language", profile.getLanguage()).setParam("name", profile.getName()).execute();

    assertThat(result.outputAsString()).isEqualTo("As exported by SQ !");
  }

  @Test
  public void export_with_format() throws Exception {
    QualityProfileDto profile = QProfileTesting.newXooP1();
    qualityProfileDao.insert(session, profile);
    session.commit();

    Result result = wsTester.newGetRequest("api/qualityprofiles", "export")
      .setParam("language", profile.getLanguage()).setParam("name", profile.getName()).setParam("exporterKey", "polop").execute();

    assertThat(result.outputAsString()).isEqualTo("Profile " + profile.getName() + " exported by polop");
  }

  @Test
  public void export_default_profile() throws Exception {
    QualityProfileDto profile1 = QProfileTesting.newXooP1();
    QualityProfileDto profile2 = QProfileTesting.newXooP2().setDefault(true);
    qualityProfileDao.insert(session, profile1, profile2);
    session.commit();

    Result result = wsTester.newGetRequest("api/qualityprofiles", "export")
      .setParam("language", "xoo").setParam("exporterKey", "polop").execute();

    assertThat(result.outputAsString()).isEqualTo("Profile " + profile2.getName() + " exported by polop");
  }

  @Test(expected = NotFoundException.class)
  public void fail_on_unknown_profile() throws Exception {
    wsTester.newGetRequest("api/qualityprofiles", "export")
      .setParam("language", "xoo").setParam("exporterKey", "polop").execute();
  }

  @Test(expected = IllegalArgumentException.class)
  public void fail_on_unknown_exporter() throws Exception {
    QualityProfileDto profile = QProfileTesting.newXooP1();
    qualityProfileDao.insert(session, profile);
    session.commit();

    wsTester.newGetRequest("api/qualityprofiles", "export")
      .setParam("language", "xoo").setParam("exporterKey", "unknown").execute();
  }

  @Test
  public void do_not_fail_when_no_exporters() throws Exception {
    QProfileExporters myExporters = new QProfileExporters(dbClient, null, null, null, new ProfileExporter[0], null);
    WsTester myWsTester = new WsTester(new QProfilesWs(mock(RuleActivationActions.class),
      mock(BulkRuleActivationActions.class),
      new ExportAction(dbClient, new QProfileFactory(dbClient), backuper, myExporters, LanguageTesting.newLanguages("xoo"))));

    Action export = myWsTester.controller("api/qualityprofiles").action("export");
    assertThat(export.params()).hasSize(2);

    QualityProfileDto profile = QProfileTesting.newXooP1();
    qualityProfileDao.insert(session, profile);
    session.commit();

    myWsTester.newGetRequest("api/qualityprofiles", "export").setParam("language", "xoo").setParam("name", profile.getName()).execute();

  }
}
