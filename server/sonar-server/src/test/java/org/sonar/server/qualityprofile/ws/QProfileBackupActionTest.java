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

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.sonar.core.persistence.DbTester;
import org.sonar.server.db.DbClient;
import org.sonar.server.language.LanguageTesting;
import org.sonar.server.qualityprofile.QProfileBackuper;
import org.sonar.server.qualityprofile.QProfileFactory;
import org.sonar.server.ws.WsTester;
import org.sonar.server.ws.WsTester.Result;

import java.io.PrintWriter;
import java.io.Writer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class QProfileBackupActionTest {

  @ClassRule
  public static final DbTester db = new DbTester();

  // TODO Replace with proper DbTester + EsTester medium test once DaoV2 is removed
  @Mock
  private QProfileBackuper backuper;

  private WsTester tester;

  @Before
  public void setUp() throws Exception {
    DbClient dbClient = new DbClient(db.database(), db.myBatis());

    tester = new WsTester(new QProfilesWs(
      mock(RuleActivationActions.class),
      mock(BulkRuleActivationActions.class),
      mock(ProjectAssociationActions.class),
      new QProfileBackupAction(backuper, dbClient, new QProfileFactory(dbClient), LanguageTesting.newLanguages("xoo"))));
  }

  @Test
  public void backup_profile() throws Exception {
    String profileKey = "polop-palap-xoo-12345";

    final String response = "<polop><palap/></polop>";
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        Writer w = (Writer) invocation.getArguments()[1];
        new PrintWriter(w).print(response);
        w.close();
        return null;
      }
    }).when(backuper).backup(eq(profileKey), any(Writer.class));

    Result result = tester.newGetRequest("api/qualityprofiles", "backup").setParam("profileKey", profileKey).execute();
    assertThat(result.outputAsString()).isEqualTo(response);
  }

  @Test(expected = IllegalArgumentException.class)
  public void fail_on_missing_key() throws Exception {
    tester.newGetRequest("api/qualityprofiles", "backup").execute();
  }
}
