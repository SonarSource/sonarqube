/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
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

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class IssuesServiceTest {

  @Rule
  public ServiceTester<IssuesService> serviceTester = new ServiceTester<>(new IssuesService(mock(WsConnector.class)));

  private IssuesService underTest = serviceTester.getInstanceUnderTest();

  @Test
  public void add_comment() {
    underTest.addComment(new AddCommentRequest("ABCD", "Please help me to fix this issue"));
    PostRequest request = serviceTester.getPostRequest();

    assertThat(serviceTester.getPostParser()).isSameAs(Issues.Operation.parser());
    serviceTester.assertThat(request)
      .hasParam("issue", "ABCD")
      .hasParam("text", "Please help me to fix this issue")
      .andNoOtherParam();
  }

  @Test
  public void assign() {
    underTest.assign(new AssignRequest("ABCD", "teryk"));
    PostRequest request = serviceTester.getPostRequest();

    assertThat(serviceTester.getPostParser()).isSameAs(Issues.Operation.parser());
    serviceTester.assertThat(request)
      .hasParam("issue", "ABCD")
      .hasParam("assignee", "teryk")
      .andNoOtherParam();
  }

  @Test
  public void bulk_change() {
    underTest.bulkChange(BulkChangeRequest.builder()
      .setIssues(asList("ABCD", "EFGH"))
      .setAssign("john")
      .setSetSeverity("MAJOR")
      .setSetType("bugs")
      .setDoTransition("confirm")
      .setAddTags(asList("tag1", "tag2"))
      .setRemoveTags(asList("tag3", "tag4"))
      .setComment("some comment")
      .setSendNotifications(true)
      .build());
    PostRequest request = serviceTester.getPostRequest();

    assertThat(serviceTester.getPostParser()).isSameAs(Issues.BulkChangeWsResponse.parser());
    serviceTester.assertThat(request)
      .hasParam("issues", "ABCD,EFGH")
      .hasParam("assign", "john")
      .hasParam("set_severity", "MAJOR")
      .hasParam("set_type", "bugs")
      .hasParam("do_transition", "confirm")
      .hasParam("add_tags", "tag1,tag2")
      .hasParam("remove_tags", "tag3,tag4")
      .hasParam("comment", "some comment")
      .hasParam("sendNotifications", "true")
      .andNoOtherParam();
  }

  @Test
  public void changelog() {
    underTest.changelog("ABCD");
    GetRequest getRequest = serviceTester.getGetRequest();

    assertThat(serviceTester.getGetParser()).isSameAs(Issues.ChangelogWsResponse.parser());
    serviceTester.assertThat(getRequest)
      .hasParam("issue", "ABCD")
      .andNoOtherParam();
  }

  @Test
  public void do_transition() {
    underTest.doTransition(new DoTransitionRequest("ABCD", "confirm"));
    PostRequest request = serviceTester.getPostRequest();

    assertThat(serviceTester.getPostParser()).isSameAs(Issues.Operation.parser());
    serviceTester.assertThat(request)
      .hasParam("issue", "ABCD")
      .hasParam("transition", "confirm")
      .andNoOtherParam();
  }

  @Test
  public void delete_comment() {
    underTest.deleteComment("ABCD");
    PostRequest request = serviceTester.getPostRequest();

    assertThat(serviceTester.getPostParser()).isSameAs(Issues.Operation.parser());
    serviceTester.assertThat(request)
      .hasParam("comment", "ABCD")
      .andNoOtherParam();
  }

  @Test
  public void edit_comment() {
    underTest.editComment(new EditCommentRequest("ABCD", "Please help me to fix this issue"));
    PostRequest request = serviceTester.getPostRequest();

    assertThat(serviceTester.getPostParser()).isSameAs(Issues.Operation.parser());
    serviceTester.assertThat(request)
      .hasParam("comment", "ABCD")
      .hasParam("text", "Please help me to fix this issue")
      .andNoOtherParam();
  }

  @Test
  public void set_severity() {
    underTest.setSeverity(new SetSeverityRequest("ABCD", "confirm"));
    PostRequest request = serviceTester.getPostRequest();

    assertThat(serviceTester.getPostParser()).isSameAs(Issues.Operation.parser());
    serviceTester.assertThat(request)
      .hasParam("issue", "ABCD")
      .hasParam("severity", "confirm")
      .andNoOtherParam();
  }

  @Test
  public void set_type() {
    underTest.setType(new SetTypeRequest("ABCD", "bugs"));
    PostRequest request = serviceTester.getPostRequest();

    assertThat(serviceTester.getPostParser()).isSameAs(Issues.Operation.parser());
    serviceTester.assertThat(request)
      .hasParam("issue", "ABCD")
      .hasParam("type", "bugs")
      .andNoOtherParam();
  }

}
