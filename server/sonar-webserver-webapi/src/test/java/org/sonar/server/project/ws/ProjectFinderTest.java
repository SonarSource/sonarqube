/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.project.ws;

import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.DbTester;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.project.ws.ProjectFinder.Project;
import org.sonar.server.tester.UserSessionRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.api.web.UserRole.SCAN;
import static org.sonar.server.project.ws.ProjectFinder.SearchResult;

public class ProjectFinderTest {
  @Rule
  public DbTester db = DbTester.create();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private final ProjectFinder underTest = new ProjectFinder(db.getDbClient(), userSession);

  @Test
  public void selected_projects() {
    ProjectDto project1 = db.components().insertPrivateProjectDto();
    ProjectDto project2 = db.components().insertPrivateProjectDto();
    ProjectDto project3 = db.components().insertPrivateProjectDto();
    userSession.addProjectPermission(SCAN, project1, project2);
    List<Project> projects = underTest.search(db.getSession(), "").getProjects();

    assertThat(projects)
      .extracting(Project::getKey, Project::getName)
      .containsExactlyInAnyOrder(
        tuple(project1.getKey(), project1.getName()),
        tuple(project2.getKey(), project2.getName()));
  }

  @Test
  public void sort_project_by_name() {
    ProjectDto project1 = db.components().insertPrivateProjectDto(p -> p.setKey("project:one").setName("Projet Un"));
    ProjectDto project2 = db.components().insertPrivateProjectDto(p -> p.setKey("project:two").setName("Projet Deux"));
    ProjectDto project3 = db.components().insertPrivateProjectDto(p -> p.setKey("project:three").setName("Projet Trois"));
    ProjectDto project4 = db.components().insertPrivateProjectDto(p -> p.setKey("project:four").setName("Projet Quatre"));
    userSession.addProjectPermission(SCAN, project1, project2, project3, project4);

    assertThat(underTest.search(db.getSession(), "projet")
      .getProjects())
      .extracting(Project::getName)
      .containsExactly("Projet Deux", "Projet Quatre", "Projet Trois", "Projet Un");
  }

  @Test
  public void projects_are_filtered_by_permissions() {
    ProjectDto project1 = db.components().insertPrivateProjectDto(p -> p.setKey("project:one").setName("Projet Un"));
    ProjectDto project2 = db.components().insertPrivateProjectDto(p -> p.setKey("project:two").setName("Projet Deux"));
    ProjectDto project3 = db.components().insertPrivateProjectDto(p -> p.setKey("project:three").setName("Projet Trois"));
    ProjectDto project4 = db.components().insertPrivateProjectDto(p -> p.setKey("project:four").setName("Projet Quatre"));
    db.components().insertPrivateProjectDto(p -> p.setKey("project:five").setName("Projet Cinq"));

    userSession.addProjectPermission(SCAN, project1, project2, project3, project4);

    SearchResult result = underTest.search(db.getSession(), null);

    assertThat(result.getProjects())
      .extracting(Project::getKey, Project::getName)
      .containsExactlyInAnyOrder(
        tuple("project:one", "Projet Un"),
        tuple("project:two", "Projet Deux"),
        tuple("project:three", "Projet Trois"),
        tuple("project:four", "Projet Quatre"));
  }

  @Test
  public void projects_are_not_filtered_due_to_global_scan_permission() {
    db.components().insertPrivateProjectDto(p -> p.setKey("project:one").setName("Projet Un"));
    db.components().insertPrivateProjectDto(p -> p.setKey("project:two").setName("Projet Deux"));
    db.components().insertPrivateProjectDto(p -> p.setKey("project:three").setName("Projet Trois"));
    db.components().insertPrivateProjectDto(p -> p.setKey("project:four").setName("Projet Quatre"));
    db.components().insertPrivateProjectDto(p -> p.setKey("project:five").setName("Projet Cinq"));

    userSession.addPermission(GlobalPermission.SCAN);

    SearchResult result = underTest.search(db.getSession(), null);

    assertThat(result.getProjects())
      .extracting(Project::getKey, Project::getName)
      .containsExactlyInAnyOrder(
        tuple("project:one", "Projet Un"),
        tuple("project:two", "Projet Deux"),
        tuple("project:three", "Projet Trois"),
        tuple("project:four", "Projet Quatre"),
        tuple("project:five", "Projet Cinq"));
  }

  @Test
  public void search_by_query_on_name_case_insensitive() {
    ProjectDto project1 = db.components().insertPrivateProjectDto(p -> p.setKey("project:one").setName("Projet Un"));
    ProjectDto project2 = db.components().insertPrivateProjectDto(p -> p.setKey("project:two").setName("Projet Deux"));
    ProjectDto project3 = db.components().insertPrivateProjectDto(p -> p.setKey("project:three").setName("Projet Trois"));
    ProjectDto project4 = db.components().insertPrivateProjectDto(p -> p.setKey("project:four").setName("Projet Quatre"));

    userSession.addProjectPermission(SCAN, project1, project2, project3, project4);

    assertThat(underTest.search(db.getSession(), "projet")
      .getProjects())
      .extracting(Project::getKey)
      .containsExactlyInAnyOrder("project:one", "project:two", "project:three", "project:four");

    assertThat(underTest.search(db.getSession(), "un")
      .getProjects())
      .extracting(Project::getKey)
      .containsExactlyInAnyOrder("project:one");

    assertThat(underTest.search(db.getSession(), "TROIS")
      .getProjects())
      .extracting(Project::getKey)
      .containsExactlyInAnyOrder("project:three");
  }
}
