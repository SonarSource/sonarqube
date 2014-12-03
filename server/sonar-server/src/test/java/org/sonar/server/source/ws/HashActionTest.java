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
package org.sonar.server.source.ws;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.web.UserRole;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.source.db.FileSourceDao;
import org.sonar.server.component.db.ComponentDao;
import org.sonar.server.db.DbClient;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.MockUserSession;
import org.sonar.server.ws.WsTester;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class HashActionTest {

  @Mock
  DbClient dbClient;

  @Mock
  ComponentDao componentDao;

  @Mock
  FileSourceDao fileSourceDao;

  WsTester tester;

  @Before
  public void setUp() throws Exception {
    when(dbClient.componentDao()).thenReturn(componentDao);
    when(dbClient.openSession(false)).thenReturn(mock(DbSession.class));
    tester = new WsTester(
      new SourcesWs(
        mock(ShowAction.class),
        mock(RawAction.class),
        mock(ScmAction.class),
        mock(LinesAction.class),
        new HashAction(dbClient, fileSourceDao)
      )
    );
  }

  @Test
  public void show_hashes() throws Exception {
    String componentKey = "project:src/File.xoo";
    String uuid = "polop";
    ComponentDto component = new ComponentDto().setUuid(uuid);
    String hashes = "polop\n"
      + "\n"
      + "pilip";
    MockUserSession.set().setLogin("polop").addComponentPermission(UserRole.CODEVIEWER, "palap", componentKey);
    when(componentDao.getByKey(any(DbSession.class), eq(componentKey))).thenReturn(component);
    when(fileSourceDao.selectLineHashes(eq(uuid), any(DbSession.class))).thenReturn(hashes);
    WsTester.TestRequest request = tester.newGetRequest("api/sources", "hash").setParam("key", componentKey);
    assertThat(request.execute().outputAsString()).isEqualTo(hashes);
  }

  @Test
  public void hashes_empty_if_no_source() throws Exception {
    String componentKey = "project:src/File.xoo";
    String uuid = "polop";
    ComponentDto component = new ComponentDto().setUuid(uuid);
    MockUserSession.set().setLogin("polop").addComponentPermission(UserRole.CODEVIEWER, "palap", componentKey);
    when(componentDao.getByKey(any(DbSession.class), eq(componentKey))).thenReturn(component);
    WsTester.TestRequest request = tester.newGetRequest("api/sources", "hash").setParam("key", componentKey);
    request.execute().assertNoContent();
  }

  @Test
  public void fail_to_show_hashes_if_file_does_not_exist() throws Exception {
    String componentKey = "project:src/File.xoo";
    MockUserSession.set().setLogin("polop").addComponentPermission(UserRole.CODEVIEWER, "palap", componentKey);
    when(componentDao.getByKey(any(DbSession.class), eq(componentKey))).thenThrow(NotFoundException.class);
    try {
      WsTester.TestRequest request = tester.newGetRequest("api/sources", "hash").setParam("key", componentKey);
      request.execute();
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(NotFoundException.class);
    }
  }

  @Test(expected = ForbiddenException.class)
  public void fail_on_missing_permission() throws Exception {
    String componentKey = "project:src/File.xoo";
    String uuid = "polop";
    ComponentDto component = new ComponentDto().setUuid(uuid);
    MockUserSession.set().setLogin("polop");
    when(componentDao.getByKey(any(DbSession.class), eq(componentKey))).thenReturn(component);
    tester.newGetRequest("api/sources", "hash").setParam("key", componentKey).execute();
  }
}
