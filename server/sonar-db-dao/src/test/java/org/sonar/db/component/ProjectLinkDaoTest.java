/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.internal.TestSystem2;
import org.sonar.db.DbTester;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

public class ProjectLinkDaoTest {

  private final static long PAST = 5_000_000_000L;
  private final static long NOW = 10_000_000_000L;

  private System2 system2 = new TestSystem2().setNow(NOW);

  @Rule
  public DbTester db = DbTester.create(system2);

  private ProjectLinkDao underTest = db.getDbClient().projectLinkDao();

  @Test
  public void select_by_id() {
    ComponentDto project = db.components().insertPrivateProject();
    ProjectLinkDto link = db.componentLinks().insertProvidedLink(project, c -> c
      .setUuid("ABCD")
      .setName("Home")
      .setType("homepage")
      .setHref("http://www.struts.org"));

    ProjectLinkDto reloaded = underTest.selectByUuid(db.getSession(), link.getUuid());

    assertThat(reloaded.getUuid()).isEqualTo("ABCD");
    assertThat(reloaded.getProjectUuid()).isEqualTo(project.uuid());
    assertThat(reloaded.getType()).isEqualTo("homepage");
    assertThat(reloaded.getName()).isEqualTo("Home");
    assertThat(reloaded.getHref()).isEqualTo("http://www.struts.org");
    assertThat(reloaded.getCreatedAt()).isEqualTo(NOW);
    assertThat(reloaded.getUpdatedAt()).isEqualTo(NOW);
  }

  @Test
  public void select_by_project_uuid() {
    ComponentDto project = db.components().insertPrivateProject();
    ProjectLinkDto link1 = db.componentLinks().insertProvidedLink(project);
    ProjectLinkDto link2 = db.componentLinks().insertProvidedLink(project);
    ProjectLinkDto link3 = db.componentLinks().insertProvidedLink(project);
    ComponentDto otherProject = db.components().insertPrivateProject();
    ProjectLinkDto otherLink = db.componentLinks().insertProvidedLink(otherProject);

    assertThat(underTest.selectByProjectUuid(db.getSession(), project.uuid()))
      .extracting(ProjectLinkDto::getUuid)
      .containsExactlyInAnyOrder(link1.getUuid(), link2.getUuid(), link3.getUuid());
    assertThat(underTest.selectByProjectUuid(db.getSession(), otherProject.uuid()))
      .extracting(ProjectLinkDto::getUuid)
      .containsExactlyInAnyOrder(otherLink.getUuid());
    assertThat(underTest.selectByProjectUuid(db.getSession(), "UNKNOWN")).isEmpty();
  }

  @Test
  public void select_by_project_uuids() {
    ComponentDto project1 = db.components().insertPrivateProject();
    ProjectLinkDto link1 = db.componentLinks().insertProvidedLink(project1);
    ProjectLinkDto link2 = db.componentLinks().insertProvidedLink(project1);
    ComponentDto project2 = db.components().insertPrivateProject();
    ProjectLinkDto link3 = db.componentLinks().insertProvidedLink(project2);

    assertThat(underTest.selectByProjectUuids(db.getSession(), asList(project1.uuid(), project2.uuid())))
      .extracting(ProjectLinkDto::getUuid)
      .containsOnly(link1.getUuid(), link2.getUuid(), link3.getUuid());
    assertThat(underTest.selectByProjectUuids(db.getSession(), singletonList(project1.uuid())))
      .extracting(ProjectLinkDto::getUuid)
      .containsOnly(link1.getUuid(), link2.getUuid());
    assertThat(underTest.selectByProjectUuids(db.getSession(), Collections.emptyList())).isEmpty();
  }

  @Test
  public void insert() {
    ComponentDto project = db.components().insertPrivateProject();
    ProjectLinkDto link = ProjectLinkTesting.newProvidedLinkDto()
      .setUuid("ABCD")
      .setProjectUuid(project.uuid())
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
    assertThat(reloaded.getProjectUuid()).isEqualTo(project.uuid());
    assertThat(reloaded.getType()).isEqualTo("homepage");
    assertThat(reloaded.getName()).isEqualTo("Home");
    assertThat(reloaded.getHref()).isEqualTo("http://www.struts.org");
    assertThat(reloaded.getCreatedAt()).isEqualTo(NOW);
    assertThat(reloaded.getUpdatedAt()).isEqualTo(NOW);
  }

  @Test
  public void update() {
    ComponentDto project = db.components().insertPrivateProject();
    ProjectLinkDto link = db.componentLinks().insertProvidedLink(project, c -> c
      .setUuid("ABCD")
      .setType("ci")
      .setName("Gihub")
      .setHref("http://www.github.org"));
    // Force dates to be in the past
    db.executeUpdateSql("UPDATE project_links SET created_at=" + PAST + " ,updated_at=" + PAST);

    ComponentDto project2 = db.components().insertPrivateProject();
    underTest.update(db.getSession(), link
      .setProjectUuid(project2.uuid())
      .setType("homepage")
      .setName("Home")
      .setHref("http://www.sonarqube.org"));
    db.getSession().commit();

    ProjectLinkDto reloaded = underTest.selectByUuid(db.getSession(), link.getUuid());
    assertThat(reloaded.getUuid()).isEqualTo("ABCD");
    assertThat(reloaded.getProjectUuid()).isEqualTo(project2.uuid());
    assertThat(reloaded.getType()).isEqualTo("homepage");
    assertThat(reloaded.getName()).isEqualTo("Home");
    assertThat(reloaded.getHref()).isEqualTo("http://www.sonarqube.org");
    assertThat(reloaded.getCreatedAt()).isEqualTo(PAST);
    assertThat(reloaded.getUpdatedAt()).isEqualTo(NOW);
  }

  @Test
  public void delete() {
    ComponentDto project = db.components().insertPrivateProject();
    ProjectLinkDto link = db.componentLinks().insertProvidedLink(project);

    underTest.delete(db.getSession(), link.getUuid());
    db.getSession().commit();

    assertThat(db.countRowsOfTable("project_links")).isEqualTo(0);
  }

}
