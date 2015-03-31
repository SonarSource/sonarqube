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

package org.sonar.server.batch;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.core.measure.db.MetricDto;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.properties.PropertiesDao;
import org.sonar.core.properties.PropertyDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.measure.persistence.MetricDao;
import org.sonar.server.user.MockUserSession;
import org.sonar.server.ws.WsTester;

import static com.google.common.collect.Lists.newArrayList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GlobalRepositoryActionTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Mock
  DbSession session;

  @Mock
  MetricDao metricDao;

  @Mock
  PropertiesDao propertiesDao;

  WsTester tester;

  @Before
  public void setUp() throws Exception {
    DbClient dbClient = mock(DbClient.class);
    when(dbClient.openSession(false)).thenReturn(session);
    when(dbClient.metricDao()).thenReturn(metricDao);

    tester = new WsTester(new BatchWs(mock(BatchIndex.class), new GlobalRepositoryAction(dbClient, propertiesDao), mock(ProjectRepositoryAction.class), mock(IssuesAction.class)));
  }

  @Test
  public void return_metrics() throws Exception {
    when(metricDao.selectEnabled(session)).thenReturn(newArrayList(
      new MetricDto().setId(1).setKey("coverage").setDescription("Coverage by unit tests").setValueType("PERCENT").setQualitative(true)
        .setWorstValue(0d).setBestValue(100d).setOptimizedBestValue(false).setDirection(1).setEnabled(true)
      ));

    WsTester.TestRequest request = tester.newGetRequest("batch", "global");
    request.execute().assertJson(getClass(), "return_global_referentials.json");
  }

  @Test
  public void return_global_settings() throws Exception {
    MockUserSession.set().setLogin("john").setGlobalPermissions(GlobalPermissions.SCAN_EXECUTION, GlobalPermissions.PREVIEW_EXECUTION);

    when(propertiesDao.selectGlobalProperties(session)).thenReturn(newArrayList(
      new PropertyDto().setKey("foo").setValue("bar"),
      new PropertyDto().setKey("foo.secured").setValue("1234"),
      new PropertyDto().setKey("foo.license.secured").setValue("5678")
      ));

    WsTester.TestRequest request = tester.newGetRequest("batch", "global");
    request.execute().assertJson(getClass(), "return_global_settings.json");
  }

  @Test
  public void return_only_license_settings_without_scan_but_with_preview_permission() throws Exception {
    MockUserSession.set().setLogin("john").setGlobalPermissions(GlobalPermissions.PREVIEW_EXECUTION);

    when(propertiesDao.selectGlobalProperties(session)).thenReturn(newArrayList(
      new PropertyDto().setKey("foo").setValue("bar"),
      new PropertyDto().setKey("foo.secured").setValue("1234"),
      new PropertyDto().setKey("foo.license.secured").setValue("5678")
      ));

    WsTester.TestRequest request = tester.newGetRequest("batch", "global");
    request.execute().assertJson(getClass(), "return_only_license_settings_without_scan_but_with_preview_permission.json");
  }

  @Test
  public void access_forbidden_without_scan_and_preview_permission() throws Exception {
    MockUserSession.set().setLogin("john").setGlobalPermissions();

    when(propertiesDao.selectGlobalProperties(session)).thenReturn(newArrayList(
      new PropertyDto().setKey("foo").setValue("bar"),
      new PropertyDto().setKey("foo.secured").setValue("1234"),
      new PropertyDto().setKey("foo.license.secured").setValue("5678")
      ));

    thrown.expect(ForbiddenException.class);

    tester.newGetRequest("batch", "global").execute();
  }
}
