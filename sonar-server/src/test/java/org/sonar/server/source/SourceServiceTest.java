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

package org.sonar.server.source;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.web.UserRole;
import org.sonar.core.measure.db.MeasureKey;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.measure.persistence.MeasureDao;
import org.sonar.server.user.MockUserSession;

import java.util.Collections;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SourceServiceTest {

  @Mock
  DbSession session;

  @Mock
  HtmlSourceDecorator sourceDecorator;

  @Mock
  DeprecatedSourceDecorator deprecatedSourceDecorator;

  @Mock
  MeasureDao measureDao;

  SourceService service;

  @Before
  public void setUp() throws Exception {
    MyBatis myBatis = mock(MyBatis.class);
    when(myBatis.openSession(false)).thenReturn(session);
    service = new SourceService(myBatis, sourceDecorator, deprecatedSourceDecorator, measureDao);
  }

  @Test
  public void get_lines() throws Exception {
    String projectKey = "org.sonar.sample";
    String componentKey = "org.sonar.sample:Sample";
    MockUserSession.set().addComponentPermission(UserRole.CODEVIEWER, projectKey, componentKey);

    service.getLinesAsHtml(componentKey);

    verify(sourceDecorator).getDecoratedSourceAsHtml(componentKey, null, null);
  }

  @Test
  public void fail_to_get_lines_if_file_not_found() throws Exception {
    String projectKey = "org.sonar.sample";
    String componentKey = "org.sonar.sample:Sample";
    MockUserSession.set().addProjectPermissions(UserRole.CODEVIEWER, projectKey);

    try {
      service.getLinesAsHtml(componentKey);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(NotFoundException.class);
    }

    verifyZeroInteractions(sourceDecorator);
  }

  @Test
  public void get_block_of_lines() throws Exception {
    String projectKey = "org.sonar.sample";
    String componentKey = "org.sonar.sample:Sample";
    MockUserSession.set().addComponentPermission(UserRole.CODEVIEWER, projectKey, componentKey);

    service.getLinesAsHtml(componentKey, 1, 2);

    verify(sourceDecorator).getDecoratedSourceAsHtml(componentKey, 1, 2);
  }

  @Test
  public void get_lines_from_deprecated_source_decorator_when_no_data_from_new_decorator() throws Exception {
    String projectKey = "org.sonar.sample";
    String componentKey = "org.sonar.sample:Sample";
    MockUserSession.set().addComponentPermission(UserRole.CODEVIEWER, projectKey, componentKey);
    when(sourceDecorator.getDecoratedSourceAsHtml(eq(componentKey), anyInt(), anyInt())).thenReturn(Collections.<String>emptyList());

    service.getLinesAsHtml(componentKey, 1, 2);

    verify(deprecatedSourceDecorator).getSourceAsHtml(componentKey, 1, 2);
  }

  @Test
  public void get_scm_author_data() throws Exception {
    String componentKey = "org.sonar.sample:Sample";
    service.getScmAuthorData(componentKey);
    verify(measureDao).getByKey(MeasureKey.of(componentKey, CoreMetrics.SCM_AUTHORS_BY_LINE_KEY), session);
  }

  @Test
  public void not_get_scm_author_data_if_no_data() throws Exception {
    String componentKey = "org.sonar.sample:Sample";
    when(measureDao.getByKey(any(MeasureKey.class), eq(session))).thenReturn(null);
    assertThat(service.getScmAuthorData(componentKey)).isNull();
  }

  @Test
  public void get_scm_date_data() throws Exception {
    String componentKey = "org.sonar.sample:Sample";
    service.getScmDateData(componentKey);
    verify(measureDao).getByKey(MeasureKey.of(componentKey, CoreMetrics.SCM_LAST_COMMIT_DATETIMES_BY_LINE_KEY), session);
  }

  @Test
  public void not_get_scm_date_data_if_no_data() throws Exception {
    String componentKey = "org.sonar.sample:Sample";
    when(measureDao.getByKey(any(MeasureKey.class), eq(session))).thenReturn(null);
    assertThat(service.getScmDateData(componentKey)).isNull();
  }
}
