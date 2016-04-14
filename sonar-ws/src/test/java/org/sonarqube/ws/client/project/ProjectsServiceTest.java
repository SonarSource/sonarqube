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
package org.sonarqube.ws.client.project;

import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.ws.client.ServiceTester;
import org.sonarqube.ws.client.WsConnector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;
import static org.mockito.Mockito.mock;

public class ProjectsServiceTest {

  @Rule
  public ServiceTester<ProjectsService> serviceTester = new ServiceTester<>(new ProjectsService(mock(WsConnector.class)));

  private ProjectsService underTest = serviceTester.getInstanceUnderTest();

  @Test
  public void creates_project() {
    underTest.create(CreateRequest.builder()
      .setKey("project_key")
      .setName("Project Name")
      .build());

    assertThat(serviceTester.getPostRequest().getPath()).isEqualTo("api/projects/create");
    assertThat(serviceTester.getPostRequest().getParams()).containsOnly(
      entry("key", "project_key"),
      entry("name", "Project Name"));
  }

  @Test
  public void creates_project_on_branch() {
    underTest.create(CreateRequest.builder()
      .setKey("project_key")
      .setName("Project Name")
      .setBranch("the_branch")
      .build());

    assertThat(serviceTester.getPostRequest().getPath()).isEqualTo("api/projects/create");
    assertThat(serviceTester.getPostRequest().getParams()).containsOnly(
      entry("key", "project_key"),
      entry("name", "Project Name"),
      entry("branch", "the_branch"));
  }

  @Test
  public void deletes_project_by_id() {
    underTest.delete(DeleteRequest.builder().setId("abc").build());

    assertThat(serviceTester.getPostRequest().getPath()).isEqualTo("api/projects/delete");
    assertThat(serviceTester.getPostRequest().getParams()).containsOnly(entry("id", "abc"));
  }

  @Test
  public void deletes_project_by_key() {
    underTest.delete(DeleteRequest.builder().setKey("project_key").build());

    assertThat(serviceTester.getPostRequest().getPath()).isEqualTo("api/projects/delete");
    assertThat(serviceTester.getPostRequest().getParams()).containsOnly(entry("key", "project_key"));
  }
}
