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
package org.sonar.server.issue.ws;

import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.ws.WsTester;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class IssuesWsTest {

  IssueShowWsHandler showHandler = mock(IssueShowWsHandler.class);
  WsTester tester = new WsTester(new IssuesWs(showHandler));

  @Test
  public void define_ws() throws Exception {
    WebService.Controller controller = tester.controller("api/issues");
    assertThat(controller).isNotNull();
    assertThat(controller.description()).isNotEmpty();

    WebService.Action page = controller.action("show");
    assertThat(page).isNotNull();
    assertThat(page.handler()).isNotNull();
    assertThat(page.since()).isEqualTo("4.2");
    assertThat(page.isPost()).isFalse();
    assertThat(page.isPrivate()).isTrue();
    assertThat(page.handler()).isSameAs(showHandler);
  }

}
