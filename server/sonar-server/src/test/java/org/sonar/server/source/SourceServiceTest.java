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
import org.sonar.core.persistence.DbSession;
import org.sonar.server.db.DbClient;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.measure.persistence.MeasureDao;
import org.sonar.server.source.index.SourceLineDoc;
import org.sonar.server.source.index.SourceLineIndex;
import org.sonar.server.user.MockUserSession;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SourceServiceTest {

  static final String PROJECT_KEY = "org.sonar.sample";
  static final String COMPONENT_UUID = "abc123";
  @Mock
  DbSession session;
  @Mock
  HtmlSourceDecorator sourceDecorator;
  @Mock
  MeasureDao measureDao;
  @Mock
  SourceLineIndex sourceLineIndex;
  SourceService service;

  @Before
  public void setUp() throws Exception {
    DbClient dbClient = mock(DbClient.class);
    when(dbClient.openSession(false)).thenReturn(session);
    when(dbClient.measureDao()).thenReturn(measureDao);
    service = new SourceService(dbClient, sourceDecorator, sourceLineIndex);
  }

  @Test
  public void get_html_lines() throws Exception {
    MockUserSession.set().addComponentPermission(UserRole.CODEVIEWER, PROJECT_KEY, COMPONENT_UUID);
    when(sourceLineIndex.getLines(COMPONENT_UUID, 1, Integer.MAX_VALUE)).thenReturn(
      Arrays.asList(new SourceLineDoc().setSource("source").setHighlighting("highlight").setSymbols("symbols")));

    service.getLinesAsHtml(COMPONENT_UUID, null, null);

    verify(sourceDecorator).getDecoratedSourceAsHtml("source", "highlight", "symbols");
  }

  @Test
  public void get_block_of_lines() throws Exception {
    MockUserSession.set().addComponentPermission(UserRole.CODEVIEWER, PROJECT_KEY, COMPONENT_UUID);

    when(sourceLineIndex.getLines(COMPONENT_UUID, 1, Integer.MAX_VALUE)).thenReturn(
      Arrays.asList(new SourceLineDoc().setSource("source").setHighlighting("highlight").setSymbols("symbols"),
        new SourceLineDoc().setSource("source2").setHighlighting("highlight2").setSymbols("symbols2")));

    service.getLinesAsHtml(COMPONENT_UUID, null, null);

    verify(sourceDecorator).getDecoratedSourceAsHtml("source", "highlight", "symbols");
    verify(sourceDecorator).getDecoratedSourceAsHtml("source2", "highlight2", "symbols2");
  }

  @Test
  public void get_scm_author_data() throws Exception {
    service.getScmAuthorData(COMPONENT_UUID);
    verify(measureDao).findByComponentKeyAndMetricKey(session, COMPONENT_UUID, CoreMetrics.SCM_AUTHORS_BY_LINE_KEY);
  }

  @Test
  public void fail_to_get_scm_author_data_if_no_permission() throws Exception {
    MockUserSession.set().setLogin("johh");
    try {
      service.getScmAuthorData(COMPONENT_UUID);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(ForbiddenException.class);
    }
    verifyZeroInteractions(measureDao);
  }

  @Test
  public void not_get_scm_author_data_if_no_data() throws Exception {
    MockUserSession.set().addComponentPermission(UserRole.CODEVIEWER, PROJECT_KEY, COMPONENT_UUID);
    when(measureDao.findByComponentKeyAndMetricKey(eq(session), anyString(), anyString())).thenReturn(null);
    assertThat(service.getScmAuthorData(COMPONENT_UUID)).isNull();
  }

  @Test
  public void get_scm_date_data() throws Exception {
    MockUserSession.set().addComponentPermission(UserRole.CODEVIEWER, PROJECT_KEY, COMPONENT_UUID);
    service.getScmDateData(COMPONENT_UUID);
    verify(measureDao).findByComponentKeyAndMetricKey(session, COMPONENT_UUID, CoreMetrics.SCM_LAST_COMMIT_DATETIMES_BY_LINE_KEY);
  }

  @Test
  public void not_get_scm_date_data_if_no_data() throws Exception {
    MockUserSession.set().addComponentPermission(UserRole.CODEVIEWER, PROJECT_KEY, COMPONENT_UUID);
    when(measureDao.findByComponentKeyAndMetricKey(eq(session), anyString(), anyString())).thenReturn(null);
    assertThat(service.getScmDateData(COMPONENT_UUID)).isNull();
  }

  @Test
  public void getLinesAsTxt() throws Exception {
    MockUserSession.set().addComponentPermission(UserRole.CODEVIEWER, PROJECT_KEY, COMPONENT_UUID);
    when(sourceLineIndex.getLines(COMPONENT_UUID, 1, Integer.MAX_VALUE)).thenReturn(
      Arrays.asList(
        new SourceLineDoc().setSource("line1"),
        new SourceLineDoc().setSource("line2")));

    List<String> result = service.getLinesAsTxt(COMPONENT_UUID, null, null);
    assertThat(result).contains("line1", "line2");
  }

}
