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

package org.sonar.server.issue.ws;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.issue.IssueService;
import org.sonar.server.ws.WsTester;

import java.util.Arrays;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AuthorsActionTest {

  WebService.Controller controller;

  WsTester tester;

  @Mock
  IssueService service;

  @Before
  public void setUp() {
    tester = new WsTester(new IssuesWs(new AuthorsAction(service)));
    controller = tester.controller("api/issues");
  }

  @Test
  public void fetch_authors() throws Exception {
    String query = "luke";
    int pageSize = 42;
    when(service.listAuthors(query, pageSize))
      .thenReturn(Arrays.asList("luke.skywalker", "luke@skywalker.name"));

    tester.newGetRequest("api/issues", "authors")
      .setParam("q", query)
      .setParam("ps", Integer.toString(pageSize))
      .execute()
      .assertJson(getClass(), "authors.json");

    verify(service).listAuthors(query, pageSize);
  }
}
