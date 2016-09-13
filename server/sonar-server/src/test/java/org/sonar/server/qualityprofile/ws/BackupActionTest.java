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

import java.io.PrintWriter;
import java.io.Writer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.QualityProfileTesting;
import org.sonar.server.language.LanguageTesting;
import org.sonar.server.qualityprofile.QProfileBackuper;
import org.sonar.server.qualityprofile.QProfileFactory;
import org.sonar.server.qualityprofile.QProfileRef;
import org.sonar.server.ws.WsTester;
import org.sonar.server.ws.WsTester.Result;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BackupActionTest {

  private static final String SOME_PROFILE_KEY = "polop-palap-xoo-12345";

  private QProfileBackuper backuper = mock(QProfileBackuper.class);
  private QProfileFactory profileFactory = mock(QProfileFactory.class);
  private WsTester tester = new WsTester(new QProfilesWs(
    mock(RuleActivationActions.class),
    mock(BulkRuleActivationActions.class),
    new BackupAction(mock(DbClient.class), backuper, profileFactory, LanguageTesting.newLanguages("xoo"))));

  @Test
  public void backup_profile() throws Exception {
    when(profileFactory.find(any(DbSession.class), eq(QProfileRef.fromKey(SOME_PROFILE_KEY)))).thenReturn(QualityProfileTesting.newQualityProfileDto().setKey(SOME_PROFILE_KEY));

    final String response = "<polop><palap/></polop>";
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        Writer w = (Writer) invocation.getArguments()[1];
        new PrintWriter(w).print(response);
        w.close();
        return null;
      }
    }).when(backuper).backup(eq(SOME_PROFILE_KEY), any(Writer.class));

    Result result = tester.newGetRequest("api/qualityprofiles", "backup").setParam(QProfileRef.PARAM_PROFILE_KEY, SOME_PROFILE_KEY).execute();
    assertThat(result.outputAsString()).isEqualTo(response);
    result.assertHeader("Content-Disposition", "attachment; filename=" + SOME_PROFILE_KEY + ".xml");
  }

  @Test(expected = IllegalArgumentException.class)
  public void fail_on_missing_key() throws Exception {
    tester.newGetRequest("api/qualityprofiles", "backup").execute();
  }
}
