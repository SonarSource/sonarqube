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
package org.sonar.core.resource;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import org.apache.ibatis.session.SqlSession;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.component.Component;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Scopes;
import org.sonar.api.utils.System2;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.component.SnapshotDto;
import org.sonar.core.persistence.AbstractDaoTestCase;
import org.sonar.core.persistence.DbSession;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ResourceDaoTest extends AbstractDaoTestCase {

  private DbSession session;

  private ResourceDao dao;
  private System2 system2;

  @Before
  public void createDao() {
    session = getMyBatis().openSession(false);
    system2 = mock(System2.class);
    when(system2.now()).thenReturn(1_500_000_000_000L);
    dao = new ResourceDao(getMyBatis(), system2);
  }

  @After
  public void tearDown() throws Exception {
    session.close();
  }

  @Test
  public void testDescendantProjects_do_not_include_self() {
    setupData("fixture");

    List<ResourceDto> resources = dao.getDescendantProjects(1L);

    assertThat(resources).extracting("id").containsOnly(2L);
  }

  @Test
  public void testDescendantProjects_id_not_found() {
    setupData("fixture");

    List<ResourceDto> resources = dao.getDescendantProjects(33333L);

    assertThat(resources).isEmpty();
  }

  @Test
  public void get_resource_by_id() {
    setupData("fixture");

    ResourceDto resource = dao.getResource(1L);

    assertThat(resource.getUuid()).isEqualTo("ABCD");
    assertThat(resource.getProjectUuid()).isEqualTo("ABCD");
    assertThat(resource.getPath()).isNull();
    assertThat(resource.getName()).isEqualTo("Struts");
    assertThat(resource.getLongName()).isEqualTo("Apache Struts");
    assertThat(resource.getScope()).isEqualTo("PRJ");
    assertThat(resource.getDescription()).isEqualTo("the description");
    assertThat(resource.getLanguage()).isEqualTo("java");
    assertThat(resource.isEnabled()).isTrue();
    assertThat(resource.getAuthorizationUpdatedAt()).isNotNull();
    assertThat(resource.getCreatedAt()).isNotNull();
  }

  @Test
  public void get_resource_by_uuid() {
    setupData("fixture");

    ResourceDto resource = dao.getResource("ABCD");

    assertThat(resource.getUuid()).isEqualTo("ABCD");
    assertThat(resource.getProjectUuid()).isEqualTo("ABCD");
    assertThat(resource.getPath()).isNull();
    assertThat(resource.getName()).isEqualTo("Struts");
    assertThat(resource.getLongName()).isEqualTo("Apache Struts");
    assertThat(resource.getScope()).isEqualTo("PRJ");
    assertThat(resource.getDescription()).isEqualTo("the description");
    assertThat(resource.getLanguage()).isEqualTo("java");
    assertThat(resource.isEnabled()).isTrue();
    assertThat(resource.getAuthorizationUpdatedAt()).isNotNull();
    assertThat(resource.getCreatedAt()).isNotNull();
  }

  @Test
  public void get_resource_path_and_module_key() {
    setupData("fixture");

    ResourceDto dir = dao.getResource(3L);
    assertThat(dir.getPath()).isEqualTo("src/org/struts");

    ResourceDto file = dao.getResource(4L);
    assertThat(file.getPath()).isEqualTo("src/org/struts/RequestContext.java");
  }

  @Test
  public void get_uuid() {
    setupData("fixture");

    ResourceDto file = dao.getResource(4L);
    assertThat(file.getUuid()).isEqualTo("DEFG");
    assertThat(file.getProjectUuid()).isEqualTo("ABCD");
    assertThat(file.getModuleUuid()).isEqualTo("BCDE");
    assertThat(file.getModuleUuidPath()).isEqualTo(".ABCD.BCDE.");
  }

  @Test
  public void getResource_not_found() {
    setupData("fixture");

    assertThat(dao.getResource(987654321L)).isNull();
  }

  @Test
  public void getResources_all() {
    setupData("fixture");

    List<ResourceDto> resources = dao.getResources(ResourceQuery.create());

    assertThat(resources).hasSize(4);
  }

  @Test
  public void getResources_filter_by_qualifier() {
    setupData("fixture");

    List<ResourceDto> resources = dao.getResources(ResourceQuery.create().setQualifiers(new String[] {"TRK", "BRC"}));
    assertThat(resources).extracting("qualifier").containsOnly("TRK", "BRC");

    resources = dao.getResources(ResourceQuery.create().setQualifiers(new String[] {"XXX"}));
    assertThat(resources).isEmpty();

    resources = dao.getResources(ResourceQuery.create().setQualifiers(new String[] {}));
    assertThat(resources).hasSize(4);
  }

  @Test
  public void getResources_filter_by_key() {
    setupData("fixture");

    ResourceQuery query = ResourceQuery.create().setKey("org.struts:struts-core");
    List<ResourceDto> resources = dao.getResources(query);
    assertThat(resources).hasSize(1);
    assertThat(resources.get(0).getKey()).isEqualTo("org.struts:struts-core");

    assertThat(dao.getResource(query).getKey()).isEqualTo("org.struts:struts-core");
  }

  @Test
  public void getResourceIds_all() {
    setupData("fixture");

    List<Long> ids = dao.getResourceIds(ResourceQuery.create());

    assertThat(ids).hasSize(4);
  }

  @Test
  public void getResourceIds_filter_by_qualifier() {
    setupData("fixture");

    List<Long> ids = dao.getResourceIds(ResourceQuery.create().setQualifiers(new String[] {"TRK", "BRC"}));
    assertThat(ids).containsOnly(1L, 2L);

    ids = dao.getResourceIds(ResourceQuery.create().setQualifiers(new String[] {"XXX"}));
    assertThat(ids).isEmpty();

    ids = dao.getResourceIds(ResourceQuery.create().setQualifiers(new String[] {}));
    assertThat(ids).hasSize(4);
  }

  @Test
  public void getResources_exclude_disabled() {
    setupData("getResources_exclude_disabled");

    assertThat(dao.getResourceIds(ResourceQuery.create().setExcludeDisabled(false))).containsOnly(1L, 2L);
    assertThat(dao.getResourceIds(ResourceQuery.create().setExcludeDisabled(true))).containsOnly(2L);
  }

  @Test
  public void find_root_project_by_component_key() {
    setupData("fixture");

    assertThat(dao.getRootProjectByComponentKey("org.struts:struts-core:src/org/struts/RequestContext.java").getKey()).isEqualTo("org.struts:struts");
    assertThat(dao.getRootProjectByComponentKey("org.struts:struts-core:src/org/struts").getKey()).isEqualTo("org.struts:struts");
    assertThat(dao.getRootProjectByComponentKey("org.struts:struts-core").getKey()).isEqualTo("org.struts:struts");
    assertThat(dao.getRootProjectByComponentKey("org.struts:struts").getKey()).isEqualTo("org.struts:struts");

    assertThat(dao.getRootProjectByComponentKey("unknown")).isNull();
  }

  @Test
  public void find_root_project_by_component_Id() {
    setupData("fixture");

    assertThat(dao.getRootProjectByComponentId(4l).getKey()).isEqualTo("org.struts:struts");
    assertThat(dao.getRootProjectByComponentId(3l).getKey()).isEqualTo("org.struts:struts");
    assertThat(dao.getRootProjectByComponentId(2l).getKey()).isEqualTo("org.struts:struts");
    assertThat(dao.getRootProjectByComponentId(1l).getKey()).isEqualTo("org.struts:struts");
  }

  @Test
  public void find_parent_by_component_id() {
    setupData("fixture");

    assertThat(dao.getParentModuleByComponentId(4l, session).getKey()).isEqualTo("org.struts:struts");
    assertThat(dao.getParentModuleByComponentId(3l, session).getKey()).isEqualTo("org.struts:struts");
    assertThat(dao.getParentModuleByComponentId(2l, session).getKey()).isEqualTo("org.struts:struts");
    assertThat(dao.getParentModuleByComponentId(1l, session).getKey()).isEqualTo("org.struts:struts");
  }

  @Test
  public void should_update() {
    setupData("update");

    ResourceDto project = new ResourceDto()
      .setKey("org.struts:struts")
      .setDeprecatedKey("deprecated key").setScope(Scopes.PROJECT).setQualifier(Qualifiers.PROJECT)
      .setName("Struts").setLongName("Apache Struts").setLanguage("java").setDescription("MVC Framework")
      .setPath("/foo/bar")
      .setId(1L);

    dao.insertOrUpdate(project);

    assertThat(project.getId()).isNotNull();
    checkTables("update", "projects");
  }

  @Test
  public void should_insert() {
    setupData("insert");

    ResourceDto file1 = new ResourceDto()
      .setUuid("ABCD").setProjectUuid("EFGH").setModuleUuid("EFGH").setModuleUuidPath(".EFGH.")
      .setKey("org.struts:struts:/src/main/java/org/struts/Action.java")
      .setDeprecatedKey("org.struts:struts:org.struts.Action").setScope(Scopes.FILE).setQualifier(Qualifiers.FILE)
      .setLanguage("java").setName("Action").setLongName("org.struts.Action").setPath("/foo/bar");
    ResourceDto file2 = new ResourceDto()
      .setUuid("BCDE").setProjectUuid("FGHI").setModuleUuid("FGHI").setModuleUuidPath(".FGHI.")
      .setKey("org.struts:struts:/src/main/java/org/struts/Filter.java")
      .setDeprecatedKey("org.struts:struts:org.struts.Filter").setScope(Scopes.FILE).setQualifier(Qualifiers.FILE)
      .setLanguage("java").setName("Filter").setLongName("org.struts.Filter");

    dao.insertOrUpdate(file1, file2);

    assertThat(file1.getId()).isNotNull();
    assertThat(file2.getId()).isNotNull();
    checkTables("insert", new String[] {"authorization_updated_at", "created_at"}, "projects");

    // SONAR-3636 : created_at must be fed when inserting a new entry in the 'projects' table
    ResourceDto fileLoadedFromDB = dao.getResource(file1.getId());
    assertThat(fileLoadedFromDB.getCreatedAt()).isNotNull();
    assertThat(fileLoadedFromDB.getAuthorizationUpdatedAt()).isNotNull();
  }

  @Test
  public void should_insert_using_existing_session() throws Exception {
    setupData("insert");

    ResourceDto file1 = new ResourceDto().setUuid("ABCD")
      .setKey("org.struts:struts:/src/main/java/org/struts/Action.java")
      .setDeprecatedKey("org.struts:struts:org.struts.Action").setScope(Scopes.FILE).setQualifier(Qualifiers.FILE)
      .setLanguage("java").setName("Action").setLongName("org.struts.Action");
    ResourceDto file2 = new ResourceDto().setUuid("BCDE")
      .setKey("org.struts:struts:/src/main/java/org/struts/Filter.java")
      .setDeprecatedKey("org.struts:struts:org.struts.Filter").setScope(Scopes.FILE).setQualifier(Qualifiers.FILE)
      .setLanguage("java").setName("Filter").setLongName("org.struts.Filter");

    SqlSession session = getMyBatis().openSession();

    dao.insertUsingExistingSession(file1, session);
    dao.insertUsingExistingSession(file2, session);

    session.rollback();

    assertEmptyTables("projects");
  }

  @Test
  public void insert_add_uuids_on_project_if_missing() {
    setupData("insert");

    ResourceDto project = new ResourceDto()
      .setKey("org.struts:struts:struts")
      .setScope(Scopes.PROJECT)
      .setQualifier(Qualifiers.PROJECT);

    ResourceDto file = new ResourceDto()
      .setKey("org.struts:struts:/src/main/java/org/struts/Action.java")
      .setScope(Scopes.FILE)
      .setQualifier(Qualifiers.FILE);

    dao.insertOrUpdate(project, file);

    assertThat(project.getUuid()).isNotNull();
    assertThat(project.getProjectUuid()).isEqualTo(project.getUuid());
    assertThat(project.getModuleUuidPath()).isEmpty();

    assertThat(file.getUuid()).isNull();
    assertThat(file.getProjectUuid()).isNull();
    assertThat(file.getModuleUuidPath()).isNull();
  }

  @Test
  public void should_find_component_by_key() {
    setupData("fixture");

    assertThat(dao.findByKey("org.struts:struts")).isNotNull();
    Component component = dao.findByKey("org.struts:struts-core:src/org/struts/RequestContext.java");
    assertThat(component).isNotNull();
    assertThat(component.path()).isEqualTo("src/org/struts/RequestContext.java");
    assertThat(dao.findByKey("unknown")).isNull();
  }

  @Test
  public void should_find_component_by_id() {
    setupData("fixture");

    assertThat(dao.findById(1L, session)).isNotNull();
    assertThat(dao.findById(4L, session)).isNotNull();
    assertThat(dao.findById(555L, session)).isNull();
  }

  @Test
  public void should_select_projects_by_qualifiers() {
    setupData("fixture-including-ghost-projects-and-technical-project");

    List<Component> components = dao.selectProjectsByQualifiers(newArrayList("TRK"));
    assertThat(components).hasSize(1);
    assertThat(components.get(0).key()).isEqualTo("org.struts:struts");
    assertThat(((ComponentDto) components.get(0)).getId()).isEqualTo(1L);

    assertThat(dao.selectProjectsIncludingNotCompletedOnesByQualifiers(newArrayList("unknown"))).isEmpty();
    assertThat(dao.selectProjectsIncludingNotCompletedOnesByQualifiers(Collections.<String>emptyList())).isEmpty();
  }

  @Test
  public void should_select_projects_including_not_finished_by_qualifiers() {
    setupData("fixture-including-ghost-projects-and-technical-project");

    List<Component> components = dao.selectProjectsIncludingNotCompletedOnesByQualifiers(newArrayList("TRK"));
    assertThat(getKeys(components)).containsOnly("org.struts:struts", "org.apache.shindig", "org.sample:sample");

    assertThat(dao.selectProjectsIncludingNotCompletedOnesByQualifiers(newArrayList("unknown"))).isEmpty();
    assertThat(dao.selectProjectsIncludingNotCompletedOnesByQualifiers(Collections.<String>emptyList())).isEmpty();
  }

  @Test
  public void should_select_ghosts_projects_by_qualifiers() {
    setupData("fixture-including-ghost-projects-and-technical-project");

    List<Component> components = dao.selectGhostsProjects(newArrayList("TRK"));
    assertThat(components).hasSize(1);
    assertThat(getKeys(components)).containsOnly("org.apache.shindig");

    assertThat(dao.selectGhostsProjects(newArrayList("unknown"))).isEmpty();
    assertThat(dao.selectGhostsProjects(Collections.<String>emptyList())).isEmpty();
  }

  @Test
  public void should_select_provisioned_projects_by_qualifiers() {
    setupData("fixture-including-ghost-projects-and-technical-project");

    List<ResourceDto> components = dao.selectProvisionedProjects(newArrayList("TRK"));
    assertThat(components).hasSize(1);
    assertThat(components.get(0).getKey()).isEqualTo("org.sample:sample");

    assertThat(dao.selectProvisionedProjects(newArrayList("unknown"))).isEmpty();
    assertThat(dao.selectProvisionedProjects(Collections.<String>emptyList())).isEmpty();
  }

  @Test
  public void should_select_provisioned_project_by_key() {
    setupData("fixture-including-ghost-projects-and-technical-project");

    String key = "org.sample:sample";
    assertThat(dao.selectProvisionedProject(key).getKey()).isEqualTo(key);
    assertThat(dao.selectProvisionedProject("unknown")).isNull();
  }

  @Test
  public void get_last_snapshot_by_component_uuid() {
    setupData("get_last_snapshot_by_component_uuid");

    SnapshotDto snapshotDto = dao.getLastSnapshotByResourceUuid("ABCD", session);
    assertThat(snapshotDto.getId()).isEqualTo(1);

    assertThat(snapshotDto.getPeriodMode(1)).isEqualTo("previous_analysis");
    assertThat(snapshotDto.getPeriodModeParameter(1)).isNull();
    assertThat(snapshotDto.getPeriodDate(1)).isNull();

    assertThat(snapshotDto.getPeriodMode(2)).isEqualTo("days");
    assertThat(snapshotDto.getPeriodModeParameter(2)).isEqualTo("30");
    assertThat(snapshotDto.getPeriodDate(2)).isEqualTo(1_316_815_200_000L);

    assertThat(snapshotDto.getPeriodMode(3)).isEqualTo("days");
    assertThat(snapshotDto.getPeriodModeParameter(3)).isEqualTo("90");
    assertThat(snapshotDto.getPeriodDate(3)).isEqualTo(1_311_631_200_000L);

    assertThat(snapshotDto.getPeriodMode(4)).isEqualTo("previous_analysis");
    assertThat(snapshotDto.getPeriodModeParameter(4)).isNull();
    assertThat(snapshotDto.getPeriodDate(4)).isNull();

    assertThat(snapshotDto.getPeriodMode(5)).isEqualTo("previous_version");
    assertThat(snapshotDto.getPeriodModeParameter(5)).isNull();
    assertThat(snapshotDto.getPeriodDate(5)).isNull();

    snapshotDto = dao.getLastSnapshotByResourceUuid("EFGH", session);
    assertThat(snapshotDto.getId()).isEqualTo(2L);

    snapshotDto = dao.getLastSnapshotByResourceUuid("GHIJ", session);
    assertThat(snapshotDto.getId()).isEqualTo(3L);

    assertThat(dao.getLastSnapshotByResourceUuid("UNKNOWN", session)).isNull();
  }

  @Test
  public void update_authorization_date() {
    setupData("update_authorization_date");

    when(system2.now()).thenReturn(987654321L);
    dao.updateAuthorizationDate(1L, session);
    session.commit();

    checkTables("update_authorization_date", "projects");
  }

  private List<String> getKeys(final List<Component> components) {
    return newArrayList(Iterables.transform(components, new Function<Component, String>() {
      @Override
      public String apply(@Nullable Component input) {
        return input.key();
      }
    }));
  }
}
