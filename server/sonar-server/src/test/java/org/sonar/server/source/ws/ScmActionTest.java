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

import org.junit.Test;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.server.source.SourceService;
import org.sonar.server.ws.WsTester;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ScmActionTest {

  SourceService sourceService = mock(SourceService.class);
  ScmWriter scmWriter = mock(ScmWriter.class);
  WsTester tester = new WsTester(new SourcesWs(mock(ShowAction.class), mock(RawAction.class), new ScmAction(sourceService, scmWriter), mock(LinesAction.class),
    mock(HashAction.class), mock(IndexAction.class)));

  @Test
  public void get_scm() throws Exception {
    String fileKey = "src/Foo.java";
    when(sourceService.getScmAuthorData(fileKey)).thenReturn("1=julien");
    when(sourceService.getScmDateData(fileKey)).thenReturn("1=2013-01-01");

    WsTester.TestRequest request = tester.newGetRequest("api/sources", "scm").setParam("key", fileKey);
    request.execute();
    verify(scmWriter).write(eq("1=julien"), eq("1=2013-01-01"), eq(1), eq(Integer.MAX_VALUE), eq(false), any(JsonWriter.class));
  }

  @Test
  public void do_not_group_lines_by_commit() throws Exception {
    String fileKey = "src/Foo.java";
    when(sourceService.getScmAuthorData(fileKey)).thenReturn("1=julien");
    when(sourceService.getScmDateData(fileKey)).thenReturn("1=2013-01-01");

    WsTester.TestRequest request = tester.newGetRequest("api/sources", "scm").setParam("key", fileKey).setParam("commits_by_line", "true");
    request.execute();
    verify(scmWriter).write(eq("1=julien"), eq("1=2013-01-01"), eq(1), eq(Integer.MAX_VALUE), eq(true), any(JsonWriter.class));
  }

  @Test
  public void range_of_lines() throws Exception {
    String fileKey = "src/Foo.java";
    when(sourceService.getScmAuthorData(fileKey)).thenReturn("1=julien");
    when(sourceService.getScmDateData(fileKey)).thenReturn("1=2013-01-01");

    WsTester.TestRequest request = tester.newGetRequest("api/sources", "scm").setParam("key", fileKey).setParam("from", "3").setParam("to", "20");
    request.execute();
    verify(scmWriter).write(eq("1=julien"), eq("1=2013-01-01"), eq(3), eq(20), eq(false), any(JsonWriter.class));
  }

  @Test
  public void accept_negative_from_line() throws Exception {
    String fileKey = "src/Foo.java";
    when(sourceService.getScmAuthorData(fileKey)).thenReturn("1=julien");
    when(sourceService.getScmDateData(fileKey)).thenReturn("1=2013-01-01");

    WsTester.TestRequest request = tester.newGetRequest("api/sources", "scm").setParam("key", fileKey).setParam("from", "-3").setParam("to", "20");
    request.execute();
    verify(scmWriter).write(eq("1=julien"), eq("1=2013-01-01"), eq(1), eq(20), eq(false), any(JsonWriter.class));
  }
}
