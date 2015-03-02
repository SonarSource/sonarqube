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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringEscapeUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.sonar.api.web.UserRole;
import org.sonar.core.component.ComponentDto;
import org.sonar.server.component.ComponentService;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.source.HtmlSourceDecorator;
import org.sonar.server.source.index.SourceLineDoc;
import org.sonar.server.source.index.SourceLineIndex;
import org.sonar.server.source.index.SourceLineIndexDefinition;
import org.sonar.server.user.MockUserSession;
import org.sonar.server.ws.WsTester;

import java.util.Date;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class LinesActionTest {

  @Mock
  SourceLineIndex sourceLineIndex;

  @Mock
  HtmlSourceDecorator htmlSourceDecorator;

  @Mock
  ComponentService componentService;

  WsTester tester;

  @Before
  public void setUp() throws Exception {
    tester = new WsTester(
      new SourcesWs(
        mock(ShowAction.class),
        mock(RawAction.class),
        mock(ScmAction.class),
        new LinesAction(sourceLineIndex, htmlSourceDecorator, componentService),
        mock(HashAction.class),
        mock(IndexAction.class)
      )
    );
    when(htmlSourceDecorator.getDecoratedSourceAsHtml(anyString(), anyString(), anyString())).thenAnswer(new Answer<String>() {
      @Override
      public String answer(InvocationOnMock invocation) throws Throwable {
        return "<span class=\"" + invocation.getArguments()[1] + " sym-" + invocation.getArguments()[2] + "\">" +
          StringEscapeUtils.escapeHtml((String) invocation.getArguments()[0]) +
            "</span>";
      }
    });
  }

  @Test
  public void show_source() throws Exception {
    String projectUuid = "abcd";
    String componentUuid = "efgh";
    Date updatedAt = new Date();
    String scmDate = "2014-01-01T12:34:56.789Z";
    SourceLineDoc line1 = new SourceLineDoc()
      .setProjectUuid(projectUuid)
      .setFileUuid(componentUuid)
      .setLine(1)
      .setScmRevision("cafebabe")
      .setScmAuthor("polop")
      .setSource("class Polop {")
      .setHighlighting("h1")
      .setSymbols("palap")
      .setUtLineHits(3)
      .setUtConditions(2)
      .setUtCoveredConditions(1)
      .setItLineHits(3)
      .setItConditions(2)
      .setItCoveredConditions(1)
      .setDuplications(ImmutableList.<Integer>of())
      .setUpdateDate(updatedAt);
    line1.setField(SourceLineIndexDefinition.FIELD_SCM_DATE, scmDate);

    SourceLineDoc line2 = new SourceLineDoc()
      .setProjectUuid(projectUuid)
      .setFileUuid(componentUuid)
      .setLine(2)
      .setScmRevision("cafebabe")
      .setScmAuthor("polop")
      .setSource("  // Empty")
      .setHighlighting("h2")
      .setSymbols("pulup")
      .setUtLineHits(3)
      .setUtConditions(2)
      .setUtCoveredConditions(1)
      .setItLineHits(null)
      .setItConditions(null)
      .setItCoveredConditions(null)
      .setDuplications(ImmutableList.<Integer>of(1))
      .setUpdateDate(updatedAt);
    line2.setField(SourceLineIndexDefinition.FIELD_SCM_DATE, scmDate);

    SourceLineDoc line3 = new SourceLineDoc()
      .setProjectUuid(projectUuid)
      .setFileUuid(componentUuid)
      .setLine(3)
      .setScmRevision("cafebabe")
      .setScmAuthor("polop")
      .setSource("}")
      .setHighlighting("h3")
      .setSymbols("pylyp")
      .setUtLineHits(null)
      .setUtConditions(null)
      .setUtCoveredConditions(null)
      .setItLineHits(3)
      .setItConditions(2)
      .setItCoveredConditions(1)
      .setDuplications(ImmutableList.<Integer>of())
      .setUpdateDate(updatedAt);
    line3.setField(SourceLineIndexDefinition.FIELD_SCM_DATE, scmDate);

    when(sourceLineIndex.getLines(eq(componentUuid), anyInt(), anyInt())).thenReturn(newArrayList(
      line1,
      line2,
      line3
    ));

    String componentKey = "componentKey";
    when(componentService.getByUuid(componentUuid)).thenReturn(new ComponentDto().setKey(componentKey).setProjectUuid(projectUuid));
    MockUserSession.set().setLogin("login").addProjectUuidPermissions(UserRole.CODEVIEWER, projectUuid);

    WsTester.TestRequest request = tester.newGetRequest("api/sources", "lines").setParam("uuid", componentUuid);
    // Using non-strict match b/c of dates
    request.execute().assertJson(getClass(), "show_source.json", false);
  }

  @Test
  public void fail_to_show_source_if_no_source_found() throws Exception {
    String componentUuid = "abcd";
    String projectUuid = "efgh";
    when(sourceLineIndex.getLines(anyString(), anyInt(), anyInt())).thenReturn(Lists.<SourceLineDoc>newArrayList());

    String componentKey = "componentKey";
    when(componentService.getByUuid(componentUuid)).thenReturn(new ComponentDto().setKey(componentKey).setProjectUuid(projectUuid));
    MockUserSession.set().setLogin("login").addProjectUuidPermissions(UserRole.CODEVIEWER, projectUuid);

    try {
      WsTester.TestRequest request = tester.newGetRequest("api/sources", "lines").setParam("uuid", componentUuid);
      request.execute();
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(NotFoundException.class);
    }
  }

  @Test
  public void show_source_with_from_and_to_params() throws Exception {
    String projectUuid = "abcd";
    String fileUuid = "efgh";

    String componentKey = "componentKey";
    when(componentService.getByUuid(fileUuid)).thenReturn(new ComponentDto().setKey(componentKey).setProjectUuid(projectUuid));
    MockUserSession.set().setLogin("login").addProjectUuidPermissions(UserRole.CODEVIEWER, projectUuid);

    when(sourceLineIndex.getLines(fileUuid, 3, 3)).thenReturn(newArrayList(
      new SourceLineDoc()
        .setProjectUuid(projectUuid)
        .setFileUuid(fileUuid)
        .setLine(3)
        .setScmRevision("cafebabe")
        .setScmDate(null)
        .setScmAuthor("polop")
        .setSource("}")
        .setHighlighting("")
        .setSymbols("")
        .setUtLineHits(null)
        .setUtConditions(null)
        .setUtCoveredConditions(null)
        .setItLineHits(null)
        .setItConditions(null)
        .setItCoveredConditions(null)
        .setDuplications(null)
        .setUpdateDate(new Date())
    ));
    WsTester.TestRequest request = tester
      .newGetRequest("api/sources", "lines")
      .setParam("uuid", fileUuid)
      .setParam("from", "3")
      .setParam("to", "3");
    request.execute().assertJson(getClass(), "show_source_with_params_from_and_to.json");
  }

  @Test(expected = ForbiddenException.class)
  public void should_check_permission() throws Exception {
    String fileUuid = "efgh";

    String componentKey = "componentKey";
    when(componentService.getByUuid(fileUuid)).thenReturn(new ComponentDto().setKey(componentKey));
    MockUserSession.set().setLogin("login");

    tester.newGetRequest("api/sources", "lines")
      .setParam("uuid", fileUuid)
      .execute();
  }
}
