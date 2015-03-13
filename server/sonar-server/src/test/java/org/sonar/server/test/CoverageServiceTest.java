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
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.server.measure.persistence.MeasureDao;
import org.sonar.server.user.MockUserSession;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class CoverageServiceTest {

  static final String COMPONENT_KEY = "org.sonar.sample:Sample";
  @org.junit.Rule
  public ExpectedException thrown = ExpectedException.none();
  @Mock
  DbSession session;
  @Mock
  MeasureDao measureDao;
  @Mock
  SnapshotPerspectives snapshotPerspectives;
  CoverageService service;

  @Before
  public void setUp() throws Exception {
    MyBatis myBatis = mock(MyBatis.class);
    when(myBatis.openSession(false)).thenReturn(session);
    service = new CoverageService(myBatis, measureDao, snapshotPerspectives);
  }

  @Test
  public void check_permission() throws Exception {
    String projectKey = "org.sonar.sample";
    MockUserSession.set().addComponentPermission(UserRole.CODEVIEWER, projectKey, COMPONENT_KEY);

    service.checkPermission(COMPONENT_KEY);
  }

  @Test
  public void get_hits_data() throws Exception {
    service.getHits(COMPONENT_KEY, CoverageService.TYPE.UT);
    verify(measureDao).findByComponentKeyAndMetricKey(session, COMPONENT_KEY, CoreMetrics.COVERAGE_LINE_HITS_DATA_KEY);

    service.getHits(COMPONENT_KEY, CoverageService.TYPE.IT);
    verify(measureDao).findByComponentKeyAndMetricKey(session, COMPONENT_KEY, CoreMetrics.IT_COVERAGE_LINE_HITS_DATA_KEY);

    service.getHits(COMPONENT_KEY, CoverageService.TYPE.OVERALL);
    verify(measureDao).findByComponentKeyAndMetricKey(session, COMPONENT_KEY, CoreMetrics.OVERALL_COVERAGE_LINE_HITS_DATA_KEY);
  }

  @Test
  public void not_get_hits_data_if_no_data() throws Exception {
    when(measureDao.findByComponentKeyAndMetricKey(eq(session), anyString(), anyString())).thenReturn(null);
    assertThat(service.getHits(COMPONENT_KEY, CoverageService.TYPE.UT)).isEqualTo(Collections.emptyMap());
  }

  @Test
  public void get_conditions_data() throws Exception {
    service.getConditions(COMPONENT_KEY, CoverageService.TYPE.UT);
    verify(measureDao).findByComponentKeyAndMetricKey(session, COMPONENT_KEY, CoreMetrics.CONDITIONS_BY_LINE_KEY);

    service.getConditions(COMPONENT_KEY, CoverageService.TYPE.IT);
    verify(measureDao).findByComponentKeyAndMetricKey(session, COMPONENT_KEY, CoreMetrics.IT_CONDITIONS_BY_LINE_KEY);

    service.getConditions(COMPONENT_KEY, CoverageService.TYPE.OVERALL);
    verify(measureDao).findByComponentKeyAndMetricKey(session, COMPONENT_KEY, CoreMetrics.OVERALL_CONDITIONS_BY_LINE_KEY);
  }

  @Test
  public void get_covered_conditions_data() throws Exception {
    service.getCoveredConditions(COMPONENT_KEY, CoverageService.TYPE.UT);
    verify(measureDao).findByComponentKeyAndMetricKey(session, COMPONENT_KEY, CoreMetrics.COVERED_CONDITIONS_BY_LINE_KEY);

    service.getCoveredConditions(COMPONENT_KEY, CoverageService.TYPE.IT);
    verify(measureDao).findByComponentKeyAndMetricKey(session, COMPONENT_KEY, CoreMetrics.IT_COVERED_CONDITIONS_BY_LINE_KEY);

    service.getCoveredConditions(COMPONENT_KEY, CoverageService.TYPE.OVERALL);
    verify(measureDao).findByComponentKeyAndMetricKey(session, COMPONENT_KEY, CoreMetrics.OVERALL_COVERED_CONDITIONS_BY_LINE_KEY);
  }

  @Test
  public void get_test_cases_by_lines() throws Exception {
    MutableTestable testable = mock(MutableTestable.class);
    when(snapshotPerspectives.as(MutableTestable.class, COMPONENT_KEY)).thenReturn(testable);

    service.getTestCases(COMPONENT_KEY, CoverageService.TYPE.UT);
    verify(testable).testCasesByLines();

    assertThat(service.getTestCases(COMPONENT_KEY, CoverageService.TYPE.IT)).isEmpty();
  }

  @Test
  public void not_get_test_cases_by_lines_if_no_testable() throws Exception {
    when(snapshotPerspectives.as(MutableTestable.class, COMPONENT_KEY)).thenReturn(null);

    assertThat(service.getTestCases(COMPONENT_KEY, CoverageService.TYPE.UT)).isEqualTo(Collections.emptyMap());
  }

}
