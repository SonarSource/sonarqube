/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.v2.api.projects.controller;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ProjectLinkDao;
import org.sonar.db.component.ProjectLinkDto;
import org.sonar.db.project.ProjectDao;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.v2.api.ControllerTester;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.db.permission.ProjectPermission.USER;
import static org.sonar.server.v2.WebApiEndpoints.PROJECTS_ENDPOINT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DefaultProjectsControllerTest {
  private final DbClient dbClient = mock();
  private final DbSession dbSession = mock();
  private final ProjectDao projectDao = mock();
  private final ProjectLinkDao projectLinkDao = mock();
  private final UserSessionRule userSession = UserSessionRule.standalone();
  private final MockMvc mockMvc = ControllerTester.getMockMvc(new DefaultProjectsController(dbClient, userSession));

  @BeforeEach
  void setUp() {
    when(dbClient.openSession(false)).thenReturn(dbSession);
    when(dbClient.projectDao()).thenReturn(projectDao);
    when(dbClient.projectLinkDao()).thenReturn(projectLinkDao);
  }

  @Test
  void getProjects_returnsPublicProjectsForAnonymousCaller() throws Exception {
    ProjectDto project = project("id", "key", 1000).setDescription("description").setTags(List.of("java"));
    when(projectDao.selectProjectsByKeys(dbSession, Set.of("key"))).thenReturn(List.of(project));
    when(projectLinkDao.selectByProjectUuids(dbSession, List.of("id"))).thenReturn(List.of(
      new ProjectLinkDto().setProjectUuid("id").setName("Home").setHref("https://example.com").setType("homepage")));

    mockMvc.perform(get(PROJECTS_ENDPOINT).param("keys", "key"))
      .andExpect(status().isOk())
      .andExpect(content().json("""
        {
          "projects": [{
            "id": "id", "key": "key", "name": "Name", "visibility": "public", "description": "description",
            "tags": ["java"], "links": [{"name": "Home", "url": "https://example.com", "type": "homepage"}],
            "createdAt": "1970-01-01T00:00:01Z", "updatedAt": "1970-01-01T00:00:02Z"
          }],
          "page": {"pageIndex": 1, "pageSize": 50, "total": 1}
        }
        """, true));
    verify(dbSession).close();
  }

  @Test
  void getProjects_appliesIntersectionAndAnyTagBeforeCounting() throws Exception {
    ProjectDto matching = project("id1", "key1", 1000).setTags(List.of("java"));
    ProjectDto wrongKey = project("id2", "key2", 1000).setTags(List.of("java"));
    ProjectDto wrongTag = project("id3", "key3", 1000).setTags(List.of("javascript"));
    ProjectDto application = project("id4", "key4", 1000).setQualifier("APP").setTags(List.of("java"));
    when(projectDao.selectByUuids(dbSession, Set.of("id1", "id2", "id3", "id4")))
      .thenReturn(List.of(matching, wrongKey, wrongTag, application));

    mockMvc.perform(get(PROJECTS_ENDPOINT).param("ids", "id1,id2,id3,id4").param("keys", "key1,key3,key4").param("tags", "java,kotlin"))
      .andExpect(status().isOk())
      .andExpect(content().json("{\"page\": {\"total\": 1}, \"projects\": [{\"id\": \"id1\"}]}"));
  }

  @Test
  void getProjects_filtersPermissionsBeforePaginatingAndOrdersByCreatedAtThenId() throws Exception {
    ProjectDto hidden = project("hidden", "hidden", 0).setPrivate(true);
    ProjectDto later = project("later", "later", 2000);
    ProjectDto second = project("b", "second", 1000).setPrivate(true);
    ProjectDto first = project("a", "first", 1000);
    userSession.logIn().addProjectPermission(USER, second);
    when(projectDao.selectByUuids(dbSession, Set.of("hidden", "later", "a", "b")))
      .thenReturn(List.of(later, second, first, hidden));

    mockMvc.perform(get(PROJECTS_ENDPOINT).param("ids", "hidden,later,a,b").param("pageIndex", "2").param("pageSize", "1"))
      .andExpect(status().isOk())
      .andExpect(content().json("{\"page\": {\"total\": 3}, \"projects\": [{\"id\": \"b\", \"visibility\": \"private\"}]}"));
    verify(projectLinkDao).selectByProjectUuids(dbSession, List.of("b"));
  }

  @Test
  void getProjects_anonymousCallerCannotSeePrivateProjects() throws Exception {
    when(projectDao.selectProjectsByKeys(dbSession, Set.of("private")))
      .thenReturn(List.of(project("private", "private", 0).setPrivate(true)));
    mockMvc.perform(get(PROJECTS_ENDPOINT).param("keys", "private"))
      .andExpect(status().isOk())
      .andExpect(content().json("{\"page\": {\"total\": 0}, \"projects\": []}"));
    verifyNoInteractions(projectLinkDao);
  }

  @Test
  void getProjects_zeroPageSizeReturnsOnlyAuthorizedCount() throws Exception {
    when(projectDao.selectProjectsByKeys(dbSession, Set.of("key"))).thenReturn(List.of(project("id", "key", 0)));
    mockMvc.perform(get(PROJECTS_ENDPOINT).param("keys", "key").param("pageSize", "0"))
      .andExpect(status().isOk())
      .andExpect(content().json("{\"page\": {\"total\": 1, \"pageSize\": 0}, \"projects\": []}"));
    verifyNoInteractions(projectLinkDao);
  }

  @Test
  void getProjects_pageBeyondResultsReturnsEmptyPage() throws Exception {
    when(projectDao.selectProjectsByKeys(dbSession, Set.of("key"))).thenReturn(List.of(project("id", "key", 0)));
    mockMvc.perform(get(PROJECTS_ENDPOINT).param("keys", "key").param("pageIndex", "2147483647").param("pageSize", "5000"))
      .andExpect(status().isOk())
      .andExpect(content().json("{\"page\": {\"total\": 1}, \"projects\": []}"));
  }

  @Test
  void getProjects_requiresIdsOrKeysEvenWhenTagsAreProvided() throws Exception {
    mockMvc.perform(get(PROJECTS_ENDPOINT).param("tags", "java"))
      .andExpect(status().isBadRequest());
    verifyNoInteractions(projectDao);
  }

  @ParameterizedTest
  @CsvSource({"pageIndex,0", "pageIndex,-1", "pageSize,-1", "pageSize,5001", "pageSize,invalid"})
  void getProjects_rejectsInvalidPagination(String parameter, String value) throws Exception {
    mockMvc.perform(get(PROJECTS_ENDPOINT).param("keys", "key").param(parameter, value))
      .andExpect(status().isBadRequest());
    verifyNoInteractions(projectDao);
  }

  @ParameterizedTest
  @CsvSource({"organizationIds", "legacyIds"})
  void getProjects_rejectsCloudOnlyParameters(String parameter) throws Exception {
    mockMvc.perform(get(PROJECTS_ENDPOINT).param("keys", "key").queryParam(parameter, "value"))
      .andExpect(status().isBadRequest());
    verifyNoInteractions(projectDao);
  }

  private static ProjectDto project(String id, String key, long createdAt) {
    return new ProjectDto().setUuid(id).setKey(key).setName("Name").setQualifier("TRK").setPrivate(false)
      .setCreatedAt(createdAt).setUpdatedAt(2000);
  }
}
