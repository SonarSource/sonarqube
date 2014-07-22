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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.config.Settings;
import org.sonar.core.measure.db.MetricDto;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.db.DbClient;
import org.sonar.server.measure.persistence.MetricDao;
import org.sonar.server.user.MockUserSession;
import org.sonar.server.ws.WsTester;

import static com.google.common.collect.Lists.newArrayList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GlobalReferentialsActionTest {

  @Mock
  DbSession session;

  @Mock
  MetricDao metricDao;

  Settings settings;

  WsTester tester;

  @Before
  public void setUp() throws Exception {
    DbClient dbClient = mock(DbClient.class);
    when(dbClient.openSession(false)).thenReturn(session);
    when(dbClient.metricDao()).thenReturn(metricDao);

    settings = new Settings();

    tester = new WsTester(new BatchWs(mock(BatchIndex.class), new GlobalReferentialsAction(dbClient, settings)));
  }

  @Test
  public void return_metrics() throws Exception {
    when(metricDao.findEnabled(session)).thenReturn(newArrayList(
      MetricDto.createFor("coverage").setDescription("Coverage by unit tests").setValueType("PERCENT").setQualitative(true)
        .setWorstValue(0d).setBestValue(100d).setOptimizedBestValue(false).setDirection(1).setEnabled(true)
    ));    
    
    WsTester.TestRequest request = tester.newGetRequest("batch", "global");
    request.execute().assertJson(getClass(), "return_global_referentials.json");
  }

  @Test
  public void return_global_settings() throws Exception {
    MockUserSession.set().setLogin("john").setGlobalPermissions(GlobalPermissions.SCAN_EXECUTION, GlobalPermissions.DRY_RUN_EXECUTION);

    settings.setProperty("foo", "bar");
    settings.setProperty("foo.secured", "1234");
    settings.setProperty("foo.license.secured", "5678");

    WsTester.TestRequest request = tester.newGetRequest("batch", "global");
    request.execute().assertJson(getClass(), "return_global_settings.json");
  }

  @Test
  public void return_only_license_settings_without_scan_but_with_preview_permission() throws Exception {
    MockUserSession.set().setLogin("john").setGlobalPermissions(GlobalPermissions.DRY_RUN_EXECUTION);

    settings.setProperty("foo", "bar");
    settings.setProperty("foo.secured", "1234");
    settings.setProperty("foo.license.secured", "5678");

    WsTester.TestRequest request = tester.newGetRequest("batch", "global");
    request.execute().assertJson(getClass(), "return_only_license_settings_without_scan_but_with_preview_permission.json");
  }

  @Test
  public void return_no_secured_settings_without_scan_and_preview_permission() throws Exception {
    MockUserSession.set().setLogin("john").setGlobalPermissions();

    settings.setProperty("foo", "bar");
    settings.setProperty("foo.secured", "1234");
    settings.setProperty("foo.license.secured", "5678");

    WsTester.TestRequest request = tester.newGetRequest("batch", "global");
    request.execute().assertJson(getClass(), "return_no_secured_settings_without_scan_and_preview_permission.json");
  }
}
