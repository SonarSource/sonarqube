/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
package org.sonar.db.project;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.impl.utils.AlwaysIncreasingSystem2;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;

import static org.assertj.core.api.Assertions.assertThat;

public class ProjectDaoTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private System2 system2 = new AlwaysIncreasingSystem2(1000L);

  @Rule
  public DbTester db = DbTester.create(system2);

  private ProjectDao projectDao = new ProjectDao(system2);

  @Test
  public void should_insert_and_select_by_uuid() {
    ProjectDto dto = createProject("o1", "p1");

    projectDao.insert(db.getSession(), dto);

    Optional<ProjectDto> projectByUuid = projectDao.selectByUuid(db.getSession(), "uuid_o1_p1");
    assertThat(projectByUuid).isPresent();
    assertProject(projectByUuid.get(), "projectName_p1", "projectKee_o1_p1", "org_o1", "uuid_o1_p1", "desc_p1", "tag1,tag2", false);
    assertThat(projectByUuid.get().isPrivate()).isFalse();
  }

  @Test
  public void select_project_by_key() {
    ProjectDto dto = createProject("o1", "p1");

    projectDao.insert(db.getSession(), dto);

    Optional<ProjectDto> projectByKee = projectDao.selectProjectByKey(db.getSession(), "projectKee_o1_p1");
    assertThat(projectByKee).isPresent();
    assertProject(projectByKee.get(), "projectName_p1", "projectKee_o1_p1", "org_o1", "uuid_o1_p1", "desc_p1", "tag1,tag2", false);
  }

  @Test
  public void select_projects() {
    ProjectDto dto1 = createProject("o1", "p1");
    ProjectDto dto2 = createProject("o1", "p2");

    projectDao.insert(db.getSession(), dto1);
    projectDao.insert(db.getSession(), dto2);

    List<ProjectDto> projects = projectDao.selectProjects(db.getSession());
    assertThat(projects).extracting(ProjectDto::getKey).containsExactlyInAnyOrder("projectKee_o1_p1", "projectKee_o1_p2");
  }

  @Test
  public void select_by_organization_uuid() {
    ProjectDto dto1 = createProject("o1", "p1");
    ProjectDto dto2 = createProject("o1", "p2");
    ProjectDto dto3 = createProject("o2", "p1");

    projectDao.insert(db.getSession(), dto1);
    projectDao.insert(db.getSession(), dto2);
    projectDao.insert(db.getSession(), dto3);

    List<ProjectDto> projectsByOrg = projectDao.selectByOrganizationUuid(db.getSession(), "org_o1");
    assertThat(projectsByOrg).hasSize(2);
    assertProject(projectsByOrg.get(0), "projectName_p1", "projectKee_o1_p1", "org_o1", "uuid_o1_p1", "desc_p1", "tag1,tag2", false);
    assertProject(projectsByOrg.get(1), "projectName_p2", "projectKee_o1_p2", "org_o1", "uuid_o1_p2", "desc_p2", "tag1,tag2", false);
  }

  @Test
  public void update_tags() {
    ProjectDto dto1 = createProject("o1", "p1").setTagsString("");
    ProjectDto dto2 = createProject("o1", "p2").setTagsString("tag1,tag2");

    projectDao.insert(db.getSession(), dto1);
    projectDao.insert(db.getSession(), dto2);

    List<ProjectDto> projectsByUuids = projectDao.selectByUuids(db.getSession(), new HashSet<>(Arrays.asList("uuid_o1_p1", "uuid_o1_p2")));
    assertThat(projectsByUuids).hasSize(2);
    assertProject(projectsByUuids.get(0), "projectName_p1", "projectKee_o1_p1", "org_o1", "uuid_o1_p1", "desc_p1", null, false);
    assertProject(projectsByUuids.get(1), "projectName_p2", "projectKee_o1_p2", "org_o1", "uuid_o1_p2", "desc_p2", "tag1,tag2", false);

    dto1.setTags(Collections.singletonList("tag3"));
    dto2.setTagsString("");
    projectDao.updateTags(db.getSession(), dto1);
    projectDao.updateTags(db.getSession(), dto2);

    projectsByUuids = projectDao.selectByUuids(db.getSession(), new HashSet<>(Arrays.asList("uuid_o1_p1", "uuid_o1_p2")));
    assertThat(projectsByUuids).hasSize(2);
    assertProject(projectsByUuids.get(0), "projectName_p1", "projectKee_o1_p1", "org_o1", "uuid_o1_p1", "desc_p1", "tag3", false);
    assertProject(projectsByUuids.get(1), "projectName_p2", "projectKee_o1_p2", "org_o1", "uuid_o1_p2", "desc_p2", null, false);

    assertThat(projectsByUuids.get(0).getTags()).containsOnly("tag3");
  }

  @Test
  public void update_visibility() {
    ProjectDto dto1 = createProject("o1", "p1").setPrivate(true);
    ProjectDto dto2 = createProject("o1", "p2");

    projectDao.insert(db.getSession(), dto1);
    projectDao.insert(db.getSession(), dto2);

    List<ProjectDto> projectsByUuids = projectDao.selectByUuids(db.getSession(), new HashSet<>(Arrays.asList("uuid_o1_p1", "uuid_o1_p2")));
    assertThat(projectsByUuids).hasSize(2);
    assertProject(projectsByUuids.get(0), "projectName_p1", "projectKee_o1_p1", "org_o1", "uuid_o1_p1", "desc_p1", "tag1,tag2", true);
    assertProject(projectsByUuids.get(1), "projectName_p2", "projectKee_o1_p2", "org_o1", "uuid_o1_p2", "desc_p2", "tag1,tag2", false);

    projectDao.updateVisibility(db.getSession(), dto1.getUuid(), false);
    projectDao.updateVisibility(db.getSession(), dto2.getUuid(), true);

    projectsByUuids = projectDao.selectByUuids(db.getSession(), new HashSet<>(Arrays.asList("uuid_o1_p1", "uuid_o1_p2")));
    assertThat(projectsByUuids).hasSize(2);
    assertProject(projectsByUuids.get(0), "projectName_p1", "projectKee_o1_p1", "org_o1", "uuid_o1_p1", "desc_p1", "tag1,tag2", false);
    assertProject(projectsByUuids.get(1), "projectName_p2", "projectKee_o1_p2", "org_o1", "uuid_o1_p2", "desc_p2", "tag1,tag2", true);
  }

  @Test
  public void select_by_uuids() {
    ProjectDto dto1 = createProject("o1", "p1");
    ProjectDto dto2 = createProject("o1", "p2");
    ProjectDto dto3 = createProject("o1", "p3");

    projectDao.insert(db.getSession(), dto1);
    projectDao.insert(db.getSession(), dto2);
    projectDao.insert(db.getSession(), dto3);

    List<ProjectDto> projectsByUuids = projectDao.selectByUuids(db.getSession(), new HashSet<>(Arrays.asList("uuid_o1_p1", "uuid_o1_p2")));
    assertThat(projectsByUuids).hasSize(2);
    assertProject(projectsByUuids.get(0), "projectName_p1", "projectKee_o1_p1", "org_o1", "uuid_o1_p1", "desc_p1", "tag1,tag2", false);
    assertProject(projectsByUuids.get(1), "projectName_p2", "projectKee_o1_p2", "org_o1", "uuid_o1_p2", "desc_p2", "tag1,tag2", false);
  }

  @Test
  public void select_empty_by_uuids() {
    ProjectDto dto1 = createProject("o1", "p1");
    ProjectDto dto2 = createProject("o1", "p2");
    ProjectDto dto3 = createProject("o1", "p3");

    projectDao.insert(db.getSession(), dto1);
    projectDao.insert(db.getSession(), dto2);
    projectDao.insert(db.getSession(), dto3);

    List<ProjectDto> projectsByUuids = projectDao.selectByUuids(db.getSession(), Collections.emptySet());
    assertThat(projectsByUuids).hasSize(0);
  }

  private void assertProject(ProjectDto dto, String name, String kee, String org, String uuid, String desc, @Nullable String tags, boolean isPrivate) {
    assertThat(dto).extracting("name", "kee", "key", "organizationUuid", "uuid", "description", "tagsString", "private")
      .containsExactly(name, kee, kee, org, uuid, desc, tags, isPrivate);
  }

  private ProjectDto createProject(String org, String name) {
    return new ProjectDto()
      .setName("projectName_" + name)
      .setKey("projectKee_" + org + "_" + name)
      .setQualifier(Qualifiers.PROJECT)
      .setOrganizationUuid("org_" + org)
      .setUuid("uuid_" + org + "_" + name)
      .setTags(Arrays.asList("tag1", "tag2"))
      .setDescription("desc_" + name)
      .setPrivate(false);
  }
}
