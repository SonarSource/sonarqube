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
package org.sonar.db.component;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import java.util.List;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.api.component.Component;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Scopes;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.test.DbTests;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Category(DbTests.class)
public class ResourceDaoTest {

  System2 system2 = mock(System2.class);

  @Rule
  public DbTester dbTester = DbTester.create(system2);

  ResourceDao dao = dbTester.getDbClient().resourceDao();

  @Before
  public void createDao() {
    when(system2.now()).thenReturn(1_500_000_000_000L);
  }

  @Test
  public void testDescendantProjects_do_not_include_self() {
    dbTester.prepareDbUnit(getClass(), "fixture.xml");

    List<ResourceDto> resources = dao.getDescendantProjects(1L);

    assertThat(resources).extracting("id").containsOnly(2L);
  }

  @Test
  public void testDescendantProjects_id_not_found() {
    dbTester.prepareDbUnit(getClass(), "fixture.xml");

    List<ResourceDto> resources = dao.getDescendantProjects(33333L);

    assertThat(resources).isEmpty();
  }

  @Test
  public void get_resource_by_id() {
    dbTester.prepareDbUnit(getClass(), "fixture.xml");

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
    dbTester.prepareDbUnit(getClass(), "fixture.xml");

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
    dbTester.prepareDbUnit(getClass(), "fixture.xml");

    ResourceDto dir = dao.getResource(3L);
    assertThat(dir.getPath()).isEqualTo("src/org/struts");

    ResourceDto file = dao.getResource(4L);
    assertThat(file.getPath()).isEqualTo("src/org/struts/RequestContext.java");
  }

  @Test
  public void get_uuid() {
    dbTester.prepareDbUnit(getClass(), "fixture.xml");

    ResourceDto file = dao.getResource(4L);
    assertThat(file.getUuid()).isEqualTo("DEFG");
    assertThat(file.getProjectUuid()).isEqualTo("ABCD");
    assertThat(file.getModuleUuid()).isEqualTo("BCDE");
    assertThat(file.getModuleUuidPath()).isEqualTo(".ABCD.BCDE.");
  }

  @Test
  public void getResource_not_found() {
    dbTester.prepareDbUnit(getClass(), "fixture.xml");

    assertThat(dao.getResource(987654321L)).isNull();
  }

  @Test
  public void getResources_all() {
    dbTester.prepareDbUnit(getClass(), "fixture.xml");

    List<ResourceDto> resources = dao.getResources(ResourceQuery.create());

    assertThat(resources).hasSize(4);
  }

  @Test
  public void getResources_filter_by_qualifier() {
    dbTester.prepareDbUnit(getClass(), "fixture.xml");

    List<ResourceDto> resources = dao.getResources(ResourceQuery.create().setQualifiers(new String[] {"TRK", "BRC"}));
    assertThat(resources).extracting("qualifier").containsOnly("TRK", "BRC");

    resources = dao.getResources(ResourceQuery.create().setQualifiers(new String[] {"XXX"}));
    assertThat(resources).isEmpty();

    resources = dao.getResources(ResourceQuery.create().setQualifiers(new String[] {}));
    assertThat(resources).hasSize(4);
  }

  @Test
  public void getResources_filter_by_key() {
    dbTester.prepareDbUnit(getClass(), "fixture.xml");

    ResourceQuery query = ResourceQuery.create().setKey("org.struts:struts-core");
    List<ResourceDto> resources = dao.getResources(query);
    assertThat(resources).hasSize(1);
    assertThat(resources.get(0).getKey()).isEqualTo("org.struts:struts-core");

    assertThat(dao.getResource(query).getKey()).isEqualTo("org.struts:struts-core");
  }

  @Test
  public void find_root_project_by_component_key() {
    dbTester.prepareDbUnit(getClass(), "fixture.xml");

    assertThat(dao.getRootProjectByComponentKey("org.struts:struts-core:src/org/struts/RequestContext.java").getKey()).isEqualTo("org.struts:struts");
    assertThat(dao.getRootProjectByComponentKey("org.struts:struts-core:src/org/struts").getKey()).isEqualTo("org.struts:struts");
    assertThat(dao.getRootProjectByComponentKey("org.struts:struts-core").getKey()).isEqualTo("org.struts:struts");
    assertThat(dao.getRootProjectByComponentKey("org.struts:struts").getKey()).isEqualTo("org.struts:struts");

    assertThat(dao.getRootProjectByComponentKey("unknown")).isNull();
  }

  @Test
  public void find_root_project_by_component_Id() {
    dbTester.prepareDbUnit(getClass(), "fixture.xml");

    assertThat(dao.getRootProjectByComponentId(4l).getKey()).isEqualTo("org.struts:struts");
    assertThat(dao.getRootProjectByComponentId(3l).getKey()).isEqualTo("org.struts:struts");
    assertThat(dao.getRootProjectByComponentId(2l).getKey()).isEqualTo("org.struts:struts");
    assertThat(dao.getRootProjectByComponentId(1l).getKey()).isEqualTo("org.struts:struts");
  }

  @Test
  public void find_parent_by_component_id() {
    dbTester.prepareDbUnit(getClass(), "fixture.xml");

    assertThat(dao.getParentModuleByComponentId(4l, dbTester.getSession()).getKey()).isEqualTo("org.struts:struts");
    assertThat(dao.getParentModuleByComponentId(3l, dbTester.getSession()).getKey()).isEqualTo("org.struts:struts");
    assertThat(dao.getParentModuleByComponentId(2l, dbTester.getSession()).getKey()).isEqualTo("org.struts:struts");
    assertThat(dao.getParentModuleByComponentId(1l, dbTester.getSession()).getKey()).isEqualTo("org.struts:struts");
  }

  @Test
  public void should_update() {
    dbTester.prepareDbUnit(getClass(), "update.xml");
    ResourceDto project = new ResourceDto()
      .setKey("org.struts:struts")
      .setDeprecatedKey("deprecated key").setScope(Scopes.PROJECT).setQualifier(Qualifiers.PROJECT)
      .setName("Struts").setLongName("Apache Struts").setLanguage("java").setDescription("MVC Framework")
      .setPath("/foo/bar")
      .setId(1L);

    dao.insertOrUpdate(project);

    assertThat(project.getId()).isNotNull();
    dbTester.assertDbUnit(getClass(), "update-result.xml", "projects");
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
