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

import java.util.Collections;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbSession;
import org.sonar.db.MyBatis;
import org.sonar.db.measure.MeasureDao;
import org.sonar.server.tester.UserSessionRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CoverageServiceTest {

  static final String COMPONENT_KEY = "org.sonar.sample:Sample";

  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();
  @Rule
  public ExpectedException thrown = ExpectedException.none();
  @Mock
  DbSession session;
  @Mock
  MeasureDao measureDao;
  CoverageService service;

  @Before
  public void setUp() {
    MyBatis myBatis = mock(MyBatis.class);
    when(myBatis.openSession(false)).thenReturn(session);
    service = new CoverageService(myBatis, measureDao, userSessionRule);
  }

  @Test
  public void check_permission() {
    String projectKey = "org.sonar.sample";
    userSessionRule.addComponentPermission(UserRole.CODEVIEWER, projectKey, COMPONENT_KEY);

    service.checkPermission(COMPONENT_KEY);
  }

  @Test
  public void get_hits_data() {
    service.getHits(COMPONENT_KEY, CoverageService.TYPE.UT);
    verify(measureDao).selectByComponentKeyAndMetricKey(session, COMPONENT_KEY, CoreMetrics.COVERAGE_LINE_HITS_DATA_KEY);

    service.getHits(COMPONENT_KEY, CoverageService.TYPE.IT);
    verify(measureDao).selectByComponentKeyAndMetricKey(session, COMPONENT_KEY, CoreMetrics.IT_COVERAGE_LINE_HITS_DATA_KEY);

    service.getHits(COMPONENT_KEY, CoverageService.TYPE.OVERALL);
    verify(measureDao).selectByComponentKeyAndMetricKey(session, COMPONENT_KEY, CoreMetrics.OVERALL_COVERAGE_LINE_HITS_DATA_KEY);
  }

  @Test
  public void not_get_hits_data_if_no_data() {
    when(measureDao.selectByComponentKeyAndMetricKey(eq(session), anyString(), anyString())).thenReturn(null);
    assertThat(service.getHits(COMPONENT_KEY, CoverageService.TYPE.UT)).isEqualTo(Collections.emptyMap());
  }

  @Test
  public void get_conditions_data() {
    service.getConditions(COMPONENT_KEY, CoverageService.TYPE.UT);
    verify(measureDao).selectByComponentKeyAndMetricKey(session, COMPONENT_KEY, CoreMetrics.CONDITIONS_BY_LINE_KEY);

    service.getConditions(COMPONENT_KEY, CoverageService.TYPE.IT);
    verify(measureDao).selectByComponentKeyAndMetricKey(session, COMPONENT_KEY, CoreMetrics.IT_CONDITIONS_BY_LINE_KEY);

    service.getConditions(COMPONENT_KEY, CoverageService.TYPE.OVERALL);
    verify(measureDao).selectByComponentKeyAndMetricKey(session, COMPONENT_KEY, CoreMetrics.OVERALL_CONDITIONS_BY_LINE_KEY);
  }

  @Test
  public void get_covered_conditions_data() {
    service.getCoveredConditions(COMPONENT_KEY, CoverageService.TYPE.UT);
    verify(measureDao).selectByComponentKeyAndMetricKey(session, COMPONENT_KEY, CoreMetrics.COVERED_CONDITIONS_BY_LINE_KEY);

    service.getCoveredConditions(COMPONENT_KEY, CoverageService.TYPE.IT);
    verify(measureDao).selectByComponentKeyAndMetricKey(session, COMPONENT_KEY, CoreMetrics.IT_COVERED_CONDITIONS_BY_LINE_KEY);

    service.getCoveredConditions(COMPONENT_KEY, CoverageService.TYPE.OVERALL);
    verify(measureDao).selectByComponentKeyAndMetricKey(session, COMPONENT_KEY, CoreMetrics.OVERALL_COVERED_CONDITIONS_BY_LINE_KEY);
  }
}
