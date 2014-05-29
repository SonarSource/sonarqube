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
import org.sonar.server.db.DbClient;
import org.sonar.server.exceptions.ForbiddenException;
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

  static final String PROJECT_KEY = "org.sonar.sample";
  static final String COMPONENT_KEY = "org.sonar.sample:Sample";

  SourceService service;

  @Before
  public void setUp() throws Exception {
    DbClient dbClient = mock(DbClient.class);
    when(dbClient.openSession(false)).thenReturn(session);
    when(dbClient.measureDao()).thenReturn(measureDao);
    service = new SourceService(dbClient, sourceDecorator, deprecatedSourceDecorator);
  }

  @Test
  public void get_lines() throws Exception {
    MockUserSession.set().addComponentPermission(UserRole.CODEVIEWER, PROJECT_KEY, COMPONENT_KEY);

    service.getLinesAsHtml(COMPONENT_KEY);

    verify(sourceDecorator).getDecoratedSourceAsHtml(COMPONENT_KEY, null, null);
  }

  @Test
  public void get_block_of_lines() throws Exception {
    MockUserSession.set().addComponentPermission(UserRole.CODEVIEWER, PROJECT_KEY, COMPONENT_KEY);

    service.getLinesAsHtml(COMPONENT_KEY, 1, 2);

    verify(sourceDecorator).getDecoratedSourceAsHtml(COMPONENT_KEY, 1, 2);
  }

  @Test
  public void get_lines_from_deprecated_source_decorator_when_no_data_from_new_decorator() throws Exception {
    MockUserSession.set().addComponentPermission(UserRole.CODEVIEWER, PROJECT_KEY, COMPONENT_KEY);
    when(sourceDecorator.getDecoratedSourceAsHtml(eq(COMPONENT_KEY), anyInt(), anyInt())).thenReturn(Collections.<String>emptyList());

    service.getLinesAsHtml(COMPONENT_KEY, 1, 2);

    verify(deprecatedSourceDecorator).getSourceAsHtml(COMPONENT_KEY, 1, 2);
  }

  @Test
  public void get_scm_author_data() throws Exception {
    service.getScmAuthorData(COMPONENT_KEY);
    verify(measureDao).getNullableByKey(session, MeasureKey.of(COMPONENT_KEY, CoreMetrics.SCM_AUTHORS_BY_LINE_KEY));
  }

  @Test
  public void fail_to_get_scm_author_data_if_no_permission() throws Exception {
    MockUserSession.set().setLogin("johh");
    try {
      service.getScmAuthorData(COMPONENT_KEY);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(ForbiddenException.class);
    }
    verifyZeroInteractions(measureDao);
  }

  @Test
  public void not_get_scm_author_data_if_no_data() throws Exception {
    MockUserSession.set().addComponentPermission(UserRole.CODEVIEWER, PROJECT_KEY, COMPONENT_KEY);
    when(measureDao.getNullableByKey(eq(session), any(MeasureKey.class))).thenReturn(null);
    assertThat(service.getScmAuthorData(COMPONENT_KEY)).isNull();
  }

  @Test
  public void get_scm_date_data() throws Exception {
    MockUserSession.set().addComponentPermission(UserRole.CODEVIEWER, PROJECT_KEY, COMPONENT_KEY);
    service.getScmDateData(COMPONENT_KEY);
    verify(measureDao).getNullableByKey(session, MeasureKey.of(COMPONENT_KEY, CoreMetrics.SCM_LAST_COMMIT_DATETIMES_BY_LINE_KEY));
  }

  @Test
  public void not_get_scm_date_data_if_no_data() throws Exception {
    MockUserSession.set().addComponentPermission(UserRole.CODEVIEWER, PROJECT_KEY, COMPONENT_KEY);
    when(measureDao.getNullableByKey(eq(session), any(MeasureKey.class))).thenReturn(null);
    assertThat(service.getScmDateData(COMPONENT_KEY)).isNull();
  }

  @Test
  public void has_scm_data() throws Exception {
    MockUserSession.set().addComponentPermission(UserRole.CODEVIEWER, PROJECT_KEY, COMPONENT_KEY);

    when(measureDao.existsByKey(any(MeasureKey.class), eq(session))).thenReturn(true);
    assertThat(service.hasScmData(COMPONENT_KEY)).isTrue();
  }
}
