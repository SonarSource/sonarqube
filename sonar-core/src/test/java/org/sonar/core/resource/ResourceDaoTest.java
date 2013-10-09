/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.component.Component;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Scopes;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.persistence.AbstractDaoTestCase;

import javax.annotation.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;

public class ResourceDaoTest extends AbstractDaoTestCase {

  private ResourceDao dao;

  @Before
  public void createDao() {
    dao = new ResourceDao(getMyBatis());
  }

  @Test
  public void testDescendantProjects_do_not_include_self() {
    setupData("fixture");

    List<ResourceDto> resources = dao.getDescendantProjects(1L);

    assertThat(resources).onProperty("id").containsOnly(2L);
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

    assertThat(resource.getName()).isEqualTo("Struts");
    assertThat(resource.getLongName()).isEqualTo("Apache Struts");
    assertThat(resource.getScope()).isEqualTo("PRJ");
    assertThat(resource.getDescription()).isEqualTo("the description");
    assertThat(resource.getLanguage()).isEqualTo("java");
    assertThat(resource.isEnabled()).isTrue();
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

    List<ResourceDto> resources = dao.getResources(ResourceQuery.create().setQualifiers(new String[]{"TRK", "BRC"}));
    assertThat(resources).onProperty("qualifier").containsOnly("TRK", "BRC");

    resources = dao.getResources(ResourceQuery.create().setQualifiers(new String[]{"XXX"}));
    assertThat(resources).isEmpty();

    resources = dao.getResources(ResourceQuery.create().setQualifiers(new String[]{}));
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

    List<Long> ids = dao.getResourceIds(ResourceQuery.create().setQualifiers(new String[]{"TRK", "BRC"}));
    assertThat(ids).containsOnly(1L, 2L);

    ids = dao.getResourceIds(ResourceQuery.create().setQualifiers(new String[]{"XXX"}));
    assertThat(ids).isEmpty();

    ids = dao.getResourceIds(ResourceQuery.create().setQualifiers(new String[]{}));
    assertThat(ids).hasSize(4);
  }

  @Test
  public void getResources_exclude_disabled() {
    setupData("getResources_exclude_disabled");

    assertThat(dao.getResourceIds(ResourceQuery.create().setExcludeDisabled(false))).containsOnly(1L, 2L);
    assertThat(dao.getResourceIds(ResourceQuery.create().setExcludeDisabled(true))).containsOnly(2L);
  }

  @Test
  public void should_find_components_by_resource_ids() {
    setupData("fixture");

    Collection<Component> results = dao.findByIds(newArrayList(1l));
    assertThat(results).hasSize(1);
    Component component = results.iterator().next();
    assertThat(component.key()).isNotNull();
    assertThat(component.name()).isNotNull();
    assertThat(component.longName()).isNotNull();
    assertThat(component.qualifier()).isNotNull();
  }

  @Test
  public void should_find_components_by_resource_ids_on_huge_number_of_ids() {
    setupData("fixture");

    List<Long> hugeNbOfIds = newArrayList();
    for (long i=0; i<4500; i++) {
      hugeNbOfIds.add(i);
    }
    Collection<Component> results = dao.findByIds(hugeNbOfIds);

    // The goal of this test is only to check that the query do no fail, not to check the number of results
    assertThat(results).isNotNull();
  }

  @Test
  public void should_find_root_project_by_component_key() {
    setupData("fixture");

    ResourceDto resource = dao.getRootProjectByComponentKey("org.struts:struts:org.struts.RequestContext");
    assertThat(resource.getName()).isEqualTo("Struts");

    resource = dao.getRootProjectByComponentKey("org.struts:struts:org.struts");
    assertThat(resource.getName()).isEqualTo("Struts");

    resource = dao.getRootProjectByComponentKey("org.struts:struts-core");
    assertThat(resource.getName()).isEqualTo("Struts");
  }

  @Test
  public void should_find_root_project_by_component_Id() {
    setupData("fixture");

    ResourceDto resource = dao.getRootProjectByComponentId(4l);
    assertThat(resource.getName()).isEqualTo("Struts");

    resource = dao.getRootProjectByComponentId(3l);
    assertThat(resource.getName()).isEqualTo("Struts");

    resource = dao.getRootProjectByComponentId(2l);
    assertThat(resource.getName()).isEqualTo("Struts");
  }

  @Test
  public void should_update() {
    setupData("update");

    ResourceDto project = new ResourceDto()
      .setKey("org.struts:struts").setScope(Scopes.PROJECT).setQualifier(Qualifiers.PROJECT)
      .setName("Struts").setLongName("Apache Struts").setLanguage("java").setDescription("MVC Framework")
      .setId(1L);

    dao.insertOrUpdate(project);

    assertThat(project.getId()).isNotNull();
    checkTables("update", "projects");
  }

  @Test
  public void should_insert() {
    setupData("insert");

    ResourceDto file1 = new ResourceDto()
      .setKey("org.struts:struts:org.struts.Action").setScope(Scopes.FILE).setQualifier(Qualifiers.FILE)
      .setLanguage("java").setName("Action").setLongName("org.struts.Action");
    ResourceDto file2 = new ResourceDto()
      .setKey("org.struts:struts:org.struts.Filter").setScope(Scopes.FILE).setQualifier(Qualifiers.FILE)
      .setLanguage("java").setName("Filter").setLongName("org.struts.Filter");

    dao.insertOrUpdate(file1, file2);

    assertThat(file1.getId()).isNotNull();
    assertThat(file2.getId()).isNotNull();
    checkTables("insert", new String[]{"created_at"}, "projects");

    // SONAR-3636 : created_at must be fed when inserting a new entry in the 'projects' table
    ResourceDto fileLoadedFromDB = dao.getResource(file1.getId());
    assertThat(fileLoadedFromDB.getCreatedAt()).isNotNull();
  }

  @Test
  public void should_insert_using_existing_session() throws Exception {
    setupData("insert");

    ResourceDto file1 = new ResourceDto()
      .setKey("org.struts:struts:org.struts.Action").setScope(Scopes.FILE).setQualifier(Qualifiers.FILE)
      .setLanguage("java").setName("Action").setLongName("org.struts.Action");
    ResourceDto file2 = new ResourceDto()
      .setKey("org.struts:struts:org.struts.Filter").setScope(Scopes.FILE).setQualifier(Qualifiers.FILE)
      .setLanguage("java").setName("Filter").setLongName("org.struts.Filter");

    SqlSession session = getMyBatis().openSession();

    dao.insertUsingExistingSession(file1, session);
    dao.insertUsingExistingSession(file2, session);

    session.rollback();

    assertEmptyTables("projects");
  }

  @Test
  public void should_find_children_component_ids_for_unsecured_project(){
    setupData("fixture");

    assertThat(dao.findAuthorizedChildrenComponentIds(newArrayList("org.struts:struts"), null, "user")).hasSize(4);
    assertThat(dao.findAuthorizedChildrenComponentIds(newArrayList("org.struts:struts-core"), null, "user")).hasSize(3);
    assertThat(dao.findAuthorizedChildrenComponentIds(newArrayList("org.struts:struts:org.struts"), null, "user")).hasSize(2);
    assertThat(dao.findAuthorizedChildrenComponentIds(newArrayList("org.struts:struts:org.struts.RequestContext"), null, "user")).hasSize(1);

    assertThat(dao.findAuthorizedChildrenComponentIds(newArrayList("unknown"), null, "user")).isEmpty();
    assertThat(dao.findAuthorizedChildrenComponentIds(Collections.<String>emptyList(), null, "user")).isEmpty();
  }

  @Test
  public void should_find_children_component_ids_for_secured_project_for_user(){
    setupData("should_find_children_component_ids_for_secured_project_for_user");

    assertThat(dao.findAuthorizedChildrenComponentIds(newArrayList("org.struts:struts"), 100, "user")).hasSize(4);
    assertThat(dao.findAuthorizedChildrenComponentIds(newArrayList("org.struts:struts-core"), 100, "user")).hasSize(3);
    assertThat(dao.findAuthorizedChildrenComponentIds(newArrayList("org.struts:struts:org.struts"), 100, "user")).hasSize(2);
    assertThat(dao.findAuthorizedChildrenComponentIds(newArrayList("org.struts:struts:org.struts.RequestContext"), 100, "user")).hasSize(1);

    assertThat(dao.findAuthorizedChildrenComponentIds(newArrayList("unknown"), 100, "user")).isEmpty();
    assertThat(dao.findAuthorizedChildrenComponentIds(Collections.<String>emptyList(), 100, "user")).isEmpty();
  }

  @Test
  public void should_find_children_component_ids_for_secured_project_for_group(){
    setupData("should_find_children_component_ids_for_secured_project_for_group");

    assertThat(dao.findAuthorizedChildrenComponentIds(newArrayList("org.struts:struts"), 100, "user")).hasSize(4);
    assertThat(dao.findAuthorizedChildrenComponentIds(newArrayList("org.struts:struts-core"), 100, "user")).hasSize(3);
    assertThat(dao.findAuthorizedChildrenComponentIds(newArrayList("org.struts:struts:org.struts"), 100, "user")).hasSize(2);
    assertThat(dao.findAuthorizedChildrenComponentIds(newArrayList("org.struts:struts:org.struts.RequestContext"), 100, "user")).hasSize(1);

    assertThat(dao.findAuthorizedChildrenComponentIds(newArrayList("unknown"), 100, "user")).isEmpty();
    assertThat(dao.findAuthorizedChildrenComponentIds(Collections.<String>emptyList(), 100, "user")).isEmpty();
  }

  @Test
  public void should_find_component_by_key(){
    setupData("fixture");

    assertThat(dao.findByKey("org.struts:struts")).isNotNull();
    assertThat(dao.findByKey("org.struts:struts:org.struts.RequestContext")).isNotNull();
    assertThat(dao.findByKey("unknown")).isNull();
  }

  @Test
  public void should_select_projects_by_qualifiers(){
    setupData("fixture-including-ghost-projects-and-technical-project");

    List<Component> components = dao.selectProjectsByQualifiers(newArrayList("TRK"));
    assertThat(components).hasSize(1);
    assertThat(components.get(0).key()).isEqualTo("org.struts:struts");
    assertThat(((ComponentDto)components.get(0)).getId()).isEqualTo(1L);

    assertThat(dao.selectProjectsIncludingNotCompletedOnesByQualifiers(newArrayList("unknown"))).isEmpty();
    assertThat(dao.selectProjectsIncludingNotCompletedOnesByQualifiers(Collections.<String>emptyList())).isEmpty();
  }

  @Test
  public void should_select_projects_including_not_finished_by_qualifiers(){
    setupData("fixture-including-ghost-projects-and-technical-project");

    List<Component> components = dao.selectProjectsIncludingNotCompletedOnesByQualifiers(newArrayList("TRK"));
    assertThat(getKeys(components)).containsOnly("org.struts:struts", "org.apache.shindig", "org.sample:sample");

    assertThat(dao.selectProjectsIncludingNotCompletedOnesByQualifiers(newArrayList("unknown"))).isEmpty();
    assertThat(dao.selectProjectsIncludingNotCompletedOnesByQualifiers(Collections.<String>emptyList())).isEmpty();
  }

  @Test
  public void should_select_ghosts_projects_by_qualifiers(){
    setupData("fixture-including-ghost-projects-and-technical-project");

    List<Component> components = dao.selectGhostsProjects(newArrayList("TRK"));
    assertThat(components).hasSize(1);
    assertThat(getKeys(components)).containsOnly("org.apache.shindig");

    assertThat(dao.selectGhostsProjects(newArrayList("unknown"))).isEmpty();
    assertThat(dao.selectGhostsProjects(Collections.<String>emptyList())).isEmpty();
  }

  @Test
  public void should_select_provisioned_projects_by_qualifiers(){
    setupData("fixture-including-ghost-projects-and-technical-project");

    List<ResourceDto> components = dao.selectProvisionedProjects(newArrayList("TRK"));
    assertThat(components).hasSize(1);
    assertThat(components.get(0).getKey()).isEqualTo("org.sample:sample");

    assertThat(dao.selectProvisionedProjects(newArrayList("unknown"))).isEmpty();
    assertThat(dao.selectProvisionedProjects(Collections.<String>emptyList())).isEmpty();
  }

  @Test
  public void should_select_provisioned_project_by_key(){
    setupData("fixture-including-ghost-projects-and-technical-project");

    String key = "org.sample:sample";
    assertThat(dao.selectProvisionedProject(key).getKey()).isEqualTo(key);
    assertThat(dao.selectProvisionedProject("unknown")).isNull();
  }

  private List<String> getKeys(final List<Component> components){
    return newArrayList(Iterables.transform(components, new Function<Component, String>() {
      @Override
      public String apply(@Nullable Component input) {
        return input.key();
      }
    }));
  }
}
