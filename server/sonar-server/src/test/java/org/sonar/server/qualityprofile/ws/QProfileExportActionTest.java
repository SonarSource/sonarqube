/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.qualityprofile.ws;

import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sonar.api.profiles.ProfileExporter;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.server.ws.WebService.Action;
import org.sonar.api.utils.System2;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.DbTester;
import org.sonar.core.qualityprofile.db.QualityProfileDao;
import org.sonar.core.qualityprofile.db.QualityProfileDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.language.LanguageTesting;
import org.sonar.server.qualityprofile.*;
import org.sonar.server.qualityprofile.index.ActiveRuleIndex;
import org.sonar.server.search.IndexClient;
import org.sonar.server.ws.WsTester;
import org.sonar.server.ws.WsTester.Result;

import java.io.IOException;
import java.io.Writer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class QProfileExportActionTest {

  @ClassRule
  public static final DbTester db = new DbTester();

  WsTester wsTester;

  QualityProfileDao qualityProfileDao;

  DbClient dbClient;

  DbSession session;

  QProfileBackuper backuper;

  QProfileExporters exporters;

  @Before
  public void before() throws Exception {
    qualityProfileDao = new QualityProfileDao(db.myBatis(), mock(System2.class));
    dbClient = new DbClient(db.database(), db.myBatis(), qualityProfileDao);
    session = dbClient.openSession(false);
    backuper = mock(QProfileBackuper.class);

    db.truncateTables();

    ProfileExporter exporter1 = newExporter("polop");
    ProfileExporter exporter2 = newExporter("palap");

    IndexClient indexClient = mock(IndexClient.class);
    ActiveRuleIndex activeRuleIndex = mock(ActiveRuleIndex.class);
    when(activeRuleIndex.findByProfile(Matchers.anyString())).thenReturn(Sets.<ActiveRule>newHashSet().iterator());

    when(indexClient.get(ActiveRuleIndex.class)).thenReturn(activeRuleIndex);
    exporters = new QProfileExporters(new QProfileLoader(dbClient, indexClient), null, null, new ProfileExporter[] {exporter1, exporter2}, null);
    wsTester = new WsTester(new QProfilesWs(mock(RuleActivationActions.class),
      mock(BulkRuleActivationActions.class),
      mock(ProjectAssociationActions.class),
      new QProfileExportAction(dbClient, new QProfileFactory(dbClient), backuper, exporters, LanguageTesting.newLanguages("xoo"))));
  }

  @After
  public void after() throws Exception {
    session.close();
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
    QProfileExporters myExporters = new QProfileExporters(null, null, null, new ProfileExporter[0], null);
    WsTester myWsTester = new WsTester(new QProfilesWs(mock(RuleActivationActions.class),
      mock(BulkRuleActivationActions.class),
      mock(ProjectAssociationActions.class),
      new QProfileExportAction(dbClient, new QProfileFactory(dbClient), backuper, myExporters, LanguageTesting.newLanguages("xoo"))));

    Action export = myWsTester.controller("api/qualityprofiles").action("export");
    assertThat(export.params()).hasSize(2);

    QualityProfileDto profile = QProfileTesting.newXooP1();
    qualityProfileDao.insert(session, profile);
    session.commit();

    myWsTester.newGetRequest("api/qualityprofiles", "export").setParam("language", "xoo").setParam("name", profile.getName()).execute();

  }
}
