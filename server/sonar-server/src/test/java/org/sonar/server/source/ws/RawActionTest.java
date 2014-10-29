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
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.source.SourceService;
import org.sonar.server.ws.WsTester;

import java.util.Collections;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RawActionTest {

  SourceService sourceService = mock(SourceService.class);
  WsTester tester = new WsTester(new SourcesWs(mock(ShowAction.class), new RawAction(sourceService), mock(ScmAction.class)));

  @Test
  public void get_txt() throws Exception {
    String fileKey = "src/Foo.java";
    when(sourceService.getLinesAsTxt(fileKey)).thenReturn(newArrayList(
      "public class HelloWorld {",
      "}"
    ));

    WsTester.TestRequest request = tester.newGetRequest("api/sources", "raw").setParam("key", fileKey);
    String result = request.execute().outputAsString();
    assertThat(result).isEqualTo("public class HelloWorld {\r\n}\r\n");
  }

  @Test(expected = NotFoundException.class)
  public void fail_to_get_txt_when_no_source() throws Exception {
    String fileKey = "src/Foo.java";
    when(sourceService.getLinesAsTxt(fileKey)).thenReturn(Collections.<String>emptyList());

    WsTester.TestRequest request = tester.newGetRequest("api/sources", "raw").setParam("key", fileKey);
    request.execute();
  }

}
