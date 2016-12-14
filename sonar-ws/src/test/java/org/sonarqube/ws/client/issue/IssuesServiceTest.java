/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonarqube.ws.client.issue;

import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.ws.Issues;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.ServiceTester;
import org.sonarqube.ws.client.WsConnector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_ISSUE;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_TEXT;

public class IssuesServiceTest {

  @Rule
  public ServiceTester<IssuesService> serviceTester = new ServiceTester<>(new IssuesService(mock(WsConnector.class)));

  private IssuesService underTest = serviceTester.getInstanceUnderTest();

  @Test
  public void changelog() {
    underTest.changelog("ABCD");

    GetRequest getRequest = serviceTester.getGetRequest();

    assertThat(serviceTester.getGetParser()).isSameAs(Issues.ChangelogWsResponse.parser());
    serviceTester.assertThat(getRequest)
      .hasParam(PARAM_ISSUE, "ABCD")
      .andNoOtherParam();
  }

  @Test
  public void add_comment() {
    underTest.addComment("ABCD", "Please help me to fix this issue");

    PostRequest request = serviceTester.getPostRequest();

    assertThat(serviceTester.getPostParser()).isSameAs(Issues.Operation.parser());
    serviceTester.assertThat(request)
      .hasParam(PARAM_ISSUE, "ABCD")
      .hasParam(PARAM_TEXT, "Please help me to fix this issue")
      .andNoOtherParam();
  }

}
