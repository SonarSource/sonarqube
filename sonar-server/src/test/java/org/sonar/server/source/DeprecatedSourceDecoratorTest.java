/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

import org.apache.ibatis.session.SqlSession;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.resource.ResourceDao;
import org.sonar.core.resource.ResourceDto;
import org.sonar.core.resource.ResourceQuery;
import org.sonar.core.source.db.SnapshotSourceDao;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.ui.CodeColorizers;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DeprecatedSourceDecoratorTest {

  @Mock
  MyBatis mybatis;

  @Mock
  SqlSession session;

  @Mock
  ResourceDao resourceDao;

  @Mock
  CodeColorizers codeColorizers;

  @Mock
  SnapshotSourceDao snapshotSourceDao;

  DeprecatedSourceDecorator sourceDecorator;

  @Before
  public void setUp() throws Exception {
    when(mybatis.openSession()).thenReturn(session);
    sourceDecorator = new DeprecatedSourceDecorator(mybatis, resourceDao, codeColorizers, snapshotSourceDao);
  }

  @Test
  public void get_source_as_html() throws Exception {
    String componentKey = "org.sonar.sample:Sample";
    String source = "line 1\nline 2\nline 3\n";
    String htmlSource = "<span>line 1</span>\n<span>line 2</span>\n";

    when(resourceDao.getResource(any(ResourceQuery.class), eq(session))).thenReturn(new ResourceDto().setKey(componentKey).setLanguage("java"));
    when(snapshotSourceDao.selectSnapshotSourceByComponentKey(componentKey, session)).thenReturn(source);
    when(codeColorizers.toHtml(source, "java")).thenReturn(htmlSource);

    List<String> result = sourceDecorator.getSourceAsHtml(componentKey);
    assertThat(result).containsExactly("<span>line 1</span>", "<span>line 2</span>", "");
  }

  @Test
  public void return_null_if_no_source_code_on_component() throws Exception {
    String componentKey = "org.sonar.sample:Sample";
    when(resourceDao.getResource(any(ResourceQuery.class), eq(session))).thenReturn(new ResourceDto().setKey(componentKey).setLanguage("java"));
    when(snapshotSourceDao.selectSnapshotSourceByComponentKey(componentKey, session)).thenReturn(null);

    assertThat(sourceDecorator.getSourceAsHtml(componentKey)).isNull();
  }

  @Test
  public void fail_to_get_source_as_html_on_unknown_component() throws Exception {
    String componentKey = "org.sonar.sample:Sample";
    when(resourceDao.getResource(any(ResourceQuery.class), eq(session))).thenReturn(null);
    try {
      sourceDecorator.getSourceAsHtml(componentKey);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(NotFoundException.class);
    }
  }

  @Test
  public void get_source_as_html_with_from_and_to_params() throws Exception {
    String componentKey = "org.sonar.sample:Sample";
    String source = "line 1\nline 2\nline 3\n";
    String htmlSource = "<span>line 1</span>\n<span>line 2</span>\n<span>line 3</span>\n";

    when(resourceDao.getResource(any(ResourceQuery.class), eq(session))).thenReturn(new ResourceDto().setKey(componentKey).setLanguage("java"));
    when(snapshotSourceDao.selectSnapshotSourceByComponentKey(componentKey, session)).thenReturn(source);
    when(codeColorizers.toHtml(source, "java")).thenReturn(htmlSource);

    List<String> result = sourceDecorator.getSourceAsHtml(componentKey, 2, 3);
    assertThat(result).containsExactly("<span>line 2</span>", "<span>line 3</span>");
  }

  @Test
  public void get_source_as_html_with_from_param() throws Exception {
    String componentKey = "org.sonar.sample:Sample";
    String source = "line 1\nline 2\nline 3\n";
    String htmlSource = "<span>line 1</span>\n<span>line 2</span>\n<span>line 3</span>\n";

    when(resourceDao.getResource(any(ResourceQuery.class), eq(session))).thenReturn(new ResourceDto().setKey(componentKey).setLanguage("java"));
    when(snapshotSourceDao.selectSnapshotSourceByComponentKey(componentKey, session)).thenReturn(source);
    when(codeColorizers.toHtml(source, "java")).thenReturn(htmlSource);

    List<String> result = sourceDecorator.getSourceAsHtml(componentKey, 2, null);
    assertThat(result).containsExactly("<span>line 2</span>", "<span>line 3</span>", "");
  }

  @Test
  public void get_source_as_html_with_to_param() throws Exception {
    String componentKey = "org.sonar.sample:Sample";
    String source = "line 1\nline 2\nline 3\n";
    String htmlSource = "<span>line 1</span>\n<span>line 2</span>\n<span>line 3</span>\n";

    when(resourceDao.getResource(any(ResourceQuery.class), eq(session))).thenReturn(new ResourceDto().setKey(componentKey).setLanguage("java"));
    when(snapshotSourceDao.selectSnapshotSourceByComponentKey(componentKey, session)).thenReturn(source);
    when(codeColorizers.toHtml(source, "java")).thenReturn(htmlSource);

    List<String> result = sourceDecorator.getSourceAsHtml(componentKey, null, 3);
    assertThat(result).containsExactly("<span>line 1</span>", "<span>line 2</span>", "<span>line 3</span>");
  }
}
