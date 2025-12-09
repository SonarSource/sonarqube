/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.db.component;

import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.project.ProjectDto;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

class ProjectLinkDaoIT {

  private final static long PAST = 5_000_000_000L;
  private final static long NOW = 10_000_000_000L;

  private final System2 system2 = new TestSystem2().setNow(NOW);

  @RegisterExtension
  private final DbTester db = DbTester.create(system2);

  private final ProjectLinkDao underTest = db.getDbClient().projectLinkDao();

  @Test
  void select_by_id() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    ProjectLinkDto link = db.projectLinks().insertProvidedLink(project, c -> c
      .setUuid("ABCD")
      .setName("Home")
      .setType("homepage")
      .setHref("http://www.struts.org"));

    ProjectLinkDto reloaded = underTest.selectByUuid(db.getSession(), link.getUuid());

    assertThat(reloaded.getUuid()).isEqualTo("ABCD");
    assertThat(reloaded.getProjectUuid()).isEqualTo(project.getUuid());
    assertThat(reloaded.getType()).isEqualTo("homepage");
    assertThat(reloaded.getName()).isEqualTo("Home");
    assertThat(reloaded.getHref()).isEqualTo("http://www.struts.org");
    assertThat(reloaded.getCreatedAt()).isEqualTo(NOW);
    assertThat(reloaded.getUpdatedAt()).isEqualTo(NOW);
  }

  @Test
  void select_by_project_uuid() {
    ProjectDto project1 = db.components().insertPrivateProject().getProjectDto();
    ProjectLinkDto link1 = db.projectLinks().insertProvidedLink(project1);
    ProjectLinkDto link2 = db.projectLinks().insertProvidedLink(project1);
    ProjectLinkDto link3 = db.projectLinks().insertProvidedLink(project1);
    ProjectDto project2 = db.components().insertPrivateProject().getProjectDto();
    ProjectLinkDto otherLink = db.projectLinks().insertProvidedLink(project2);

    assertThat(underTest.selectByProjectUuid(db.getSession(), project1.getUuid()))
      .extracting(ProjectLinkDto::getUuid)
      .containsExactlyInAnyOrder(link1.getUuid(), link2.getUuid(), link3.getUuid());
    assertThat(underTest.selectByProjectUuid(db.getSession(), project2.getUuid()))
      .extracting(ProjectLinkDto::getUuid)
      .containsExactlyInAnyOrder(otherLink.getUuid());
    assertThat(underTest.selectByProjectUuid(db.getSession(), "UNKNOWN")).isEmpty();
  }

  @Test
  void select_by_project_uuids() {
    ProjectDto project1 = db.components().insertPrivateProject().getProjectDto();
    ProjectLinkDto link1 = db.projectLinks().insertProvidedLink(project1);
    ProjectLinkDto link2 = db.projectLinks().insertProvidedLink(project1);
    ProjectDto project2 = db.components().insertPrivateProject().getProjectDto();
    ProjectLinkDto link3 = db.projectLinks().insertProvidedLink(project2);

    assertThat(underTest.selectByProjectUuids(db.getSession(), asList(project1.getUuid(), project2.getUuid())))
      .extracting(ProjectLinkDto::getUuid)
      .containsOnly(link1.getUuid(), link2.getUuid(), link3.getUuid());
    assertThat(underTest.selectByProjectUuids(db.getSession(), singletonList(project1.getUuid())))
      .extracting(ProjectLinkDto::getUuid)
      .containsOnly(link1.getUuid(), link2.getUuid());
    assertThat(underTest.selectByProjectUuids(db.getSession(), Collections.emptyList())).isEmpty();
  }

  @Test
  void insert() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    ProjectLinkDto link = ProjectLinkTesting.newProvidedLinkDto()
      .setUuid("ABCD")
      .setProjectUuid(project.getUuid())
      .setName("Home")
      .setType("homepage")
      .setHref("http://www.struts.org")
      // These fields will be set by the DAO
      .setCreatedAt(0L)
      .setUpdatedAt(0L);

    underTest.insert(db.getSession(), link);
    db.getSession().commit();

    ProjectLinkDto reloaded = underTest.selectByUuid(db.getSession(), link.getUuid());
    assertThat(reloaded.getUuid()).isEqualTo("ABCD");
    assertThat(reloaded.getProjectUuid()).isEqualTo(project.getUuid());
    assertThat(reloaded.getType()).isEqualTo("homepage");
    assertThat(reloaded.getName()).isEqualTo("Home");
    assertThat(reloaded.getHref()).isEqualTo("http://www.struts.org");
    assertThat(reloaded.getCreatedAt()).isEqualTo(NOW);
    assertThat(reloaded.getUpdatedAt()).isEqualTo(NOW);
  }

  @Test
  void update() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    ProjectLinkDto link = db.projectLinks().insertProvidedLink(project, c -> c
      .setUuid("ABCD")
      .setType("ci")
      .setName("Gihub")
      .setHref("http://www.github.org"));
    // Force dates to be in the past
    db.executeUpdateSql("UPDATE project_links SET created_at=" + PAST + " ,updated_at=" + PAST);

    ComponentDto project2 = db.components().insertPrivateProject().getMainBranchComponent();
    underTest.update(db.getSession(), link
      .setProjectUuid(project2.uuid())
      .setType("homepage")
      .setName("Home")
      .setHref("https://www.sonarsource.com/products/sonarqube"));
    db.getSession().commit();

    ProjectLinkDto reloaded = underTest.selectByUuid(db.getSession(), link.getUuid());
    assertThat(reloaded.getUuid()).isEqualTo("ABCD");
    assertThat(reloaded.getProjectUuid()).isEqualTo(project2.uuid());
    assertThat(reloaded.getType()).isEqualTo("homepage");
    assertThat(reloaded.getName()).isEqualTo("Home");
    assertThat(reloaded.getHref()).isEqualTo("https://www.sonarsource.com/products/sonarqube");
    assertThat(reloaded.getCreatedAt()).isEqualTo(PAST);
    assertThat(reloaded.getUpdatedAt()).isEqualTo(NOW);
  }

  @Test
  void delete() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    ProjectLinkDto link = db.projectLinks().insertProvidedLink(project);

    underTest.delete(db.getSession(), link.getUuid());
    db.getSession().commit();

    assertThat(db.countRowsOfTable("project_links")).isZero();
  }

}
