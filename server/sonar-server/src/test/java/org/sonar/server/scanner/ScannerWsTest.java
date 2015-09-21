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
package org.sonar.server.scanner;

import java.io.File;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.db.property.PropertiesDao;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.db.DbClient;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ScannerWsTest {
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Mock
  ScannerIndex scannerIndex;

  WsTester tester;

  @Before
  public void before() {
    tester = new WsTester(new ScannerWs(scannerIndex,
      new GlobalAction(mock(DbClient.class), mock(PropertiesDao.class), userSessionRule),
      new ProjectAction(mock(ProjectDataLoader.class)),
      new IssuesAction(mock(DbClient.class), mock(IssueIndex.class), userSessionRule, mock(ComponentFinder.class))));
  }

  @Test
  public void download_index() throws Exception {
    when(scannerIndex.getIndex()).thenReturn("sonar-batch.jar|acbd18db4cc2f85cedef654fccc4a4d8");

    String index = tester.newGetRequest("scanner", "index").execute().outputAsString();
    assertThat(index).isEqualTo("sonar-batch.jar|acbd18db4cc2f85cedef654fccc4a4d8");
  }

  @Test
  public void download_file() throws Exception {
    String filename = "sonar-batch.jar";

    File file = temp.newFile(filename);
    FileUtils.writeStringToFile(file, "foo");
    when(scannerIndex.getFile(filename)).thenReturn(file);

    String jar = tester.newGetRequest("scanner", "file").setParam("name", filename).execute().outputAsString();
    assertThat(jar).isEqualTo("foo");
  }

}
