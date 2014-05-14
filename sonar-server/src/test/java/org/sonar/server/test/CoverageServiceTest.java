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

package org.sonar.server.test;

import org.junit.Before;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.test.MutableTestable;
import org.sonar.api.web.UserRole;
import org.sonar.core.component.SnapshotPerspectives;
import org.sonar.core.measure.db.MeasureDataDao;
import org.sonar.server.user.MockUserSession;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class CoverageServiceTest {

  @org.junit.Rule
  public ExpectedException thrown = ExpectedException.none();

  @Mock
  MeasureDataDao measureDataDao;

  @Mock
  SnapshotPerspectives snapshotPerspectives;

  static final String COMPONENT_KEY = "org.sonar.sample:Sample";

  CoverageService service;

  @Before
  public void setUp() throws Exception {
    service = new CoverageService(measureDataDao, snapshotPerspectives);
  }

  @Test
  public void check_permission() throws Exception {
    String projectKey = "org.sonar.sample";
    MockUserSession.set().addProjectPermissions(UserRole.CODEVIEWER, projectKey);
    MockUserSession.set().addProjectPermissions(UserRole.CODEVIEWER, projectKey).addComponent(COMPONENT_KEY, projectKey);

    service.checkPermission(COMPONENT_KEY);
  }

  @Test
  public void get_hits_data() throws Exception {
    service.getHitsData(COMPONENT_KEY);
    verify(measureDataDao).findByComponentKeyAndMetricKey(COMPONENT_KEY, CoreMetrics.COVERAGE_LINE_HITS_DATA_KEY);
  }

  @Test
  public void not_get_hits_data_if_no_data() throws Exception {
    when(measureDataDao.findByComponentKeyAndMetricKey(eq(COMPONENT_KEY), anyString())).thenReturn(null);
    assertThat(service.getHitsData(COMPONENT_KEY)).isNull();
  }

  @Test
  public void get_conditions_data() throws Exception {
    service.getConditionsData(COMPONENT_KEY);
    verify(measureDataDao).findByComponentKeyAndMetricKey(COMPONENT_KEY, CoreMetrics.SCM_LAST_COMMIT_DATETIMES_BY_LINE_KEY);
  }

  @Test
  public void get_coverered_conditions_data() throws Exception {
    service.getCoveredConditionsData(COMPONENT_KEY);
    verify(measureDataDao).findByComponentKeyAndMetricKey(COMPONENT_KEY, CoreMetrics.COVERED_CONDITIONS_BY_LINE_KEY);
  }

  @Test
  public void get_test_cases_by_lines() throws Exception {
    MutableTestable testable = mock(MutableTestable.class);
    when(snapshotPerspectives.as(MutableTestable.class, COMPONENT_KEY)).thenReturn(testable);

    service.getTestCasesByLines(COMPONENT_KEY);
    verify(testable).testCasesByLines();
  }

  @Test
  public void not_get_test_cases_by_lines_if_no_testable() throws Exception {
    when(snapshotPerspectives.as(MutableTestable.class, COMPONENT_KEY)).thenReturn(null);

    assertThat(service.getTestCasesByLines(COMPONENT_KEY)).isNull();
  }

}
