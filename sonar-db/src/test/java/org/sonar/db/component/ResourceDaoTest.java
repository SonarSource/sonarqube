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
package org.sonar.db.component;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.component.Component;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Scopes;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class ResourceDaoTest {

  static System2 system = mock(System2.class);

  @Rule
  public DbTester dbTester = DbTester.create(system);

  ResourceDao underTest = dbTester.getDbClient().resourceDao();

  @Test
  public void get_resource_by_uuid() {
    dbTester.prepareDbUnit(getClass(), "fixture.xml");

    ResourceDto resource = underTest.selectResource("ABCD");

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
  public void getResource_filter_by_key() {
    dbTester.prepareDbUnit(getClass(), "fixture.xml");

    ResourceQuery query = ResourceQuery.create().setKey("org.struts:struts-core");

    assertThat(underTest.selectResource(query).getKey()).isEqualTo("org.struts:struts-core");
  }

  @Test
  public void find_root_project_by_component_key() {
    dbTester.prepareDbUnit(getClass(), "fixture.xml");

    assertThat(underTest.getRootProjectByComponentKey("org.struts:struts-core:src/org/struts/RequestContext.java").getKey()).isEqualTo("org.struts:struts");
    assertThat(underTest.getRootProjectByComponentKey("org.struts:struts-core:src/org/struts").getKey()).isEqualTo("org.struts:struts");
    assertThat(underTest.getRootProjectByComponentKey("org.struts:struts-core").getKey()).isEqualTo("org.struts:struts");
    assertThat(underTest.getRootProjectByComponentKey("org.struts:struts").getKey()).isEqualTo("org.struts:struts");

    assertThat(underTest.getRootProjectByComponentKey("unknown")).isNull();
  }

  @Test
  public void should_insert_using_existing_session() {
    dbTester.prepareDbUnit(getClass(), "insert.xml");

    ResourceDto file1 = new ResourceDto().setUuid("ABCD")
      .setKey("org.struts:struts:/src/main/java/org/struts/Action.java")
      .setDeprecatedKey("org.struts:struts:org.struts.Action").setScope(Scopes.FILE).setQualifier(Qualifiers.FILE)
      .setLanguage("java").setName("Action").setLongName("org.struts.Action");
    ResourceDto file2 = new ResourceDto().setUuid("BCDE")
      .setKey("org.struts:struts:/src/main/java/org/struts/Filter.java")
      .setDeprecatedKey("org.struts:struts:org.struts.Filter").setScope(Scopes.FILE).setQualifier(Qualifiers.FILE)
      .setLanguage("java").setName("Filter").setLongName("org.struts.Filter");

    underTest.insertUsingExistingSession(file1, dbTester.getSession());
    underTest.insertUsingExistingSession(file2, dbTester.getSession());

    dbTester.getSession().rollback();

    assertThat(dbTester.countRowsOfTable("projects")).isZero();
  }

  @Test
  public void should_find_component_by_key() {
    dbTester.prepareDbUnit(getClass(), "fixture.xml");

    assertThat(underTest.selectByKey("org.struts:struts")).isNotNull();
    Component component = underTest.selectByKey("org.struts:struts-core:src/org/struts/RequestContext.java");
    assertThat(component).isNotNull();
    assertThat(component.path()).isEqualTo("src/org/struts/RequestContext.java");
    assertThat(underTest.selectByKey("unknown")).isNull();
  }

  @Test
  public void should_select_projects_by_qualifiers() {
    dbTester.prepareDbUnit(getClass(), "fixture-including-ghost-projects-and-technical-project.xml");

    List<Component> components = underTest.selectProjectsByQualifiers(newArrayList("TRK"));
    assertThat(components).hasSize(1);
    assertThat(components.get(0).key()).isEqualTo("org.struts:struts");
    assertThat(((ComponentDto) components.get(0)).getId()).isEqualTo(1L);

    assertThat(underTest.selectProjectsIncludingNotCompletedOnesByQualifiers(newArrayList("unknown"))).isEmpty();
    assertThat(underTest.selectProjectsIncludingNotCompletedOnesByQualifiers(Collections.<String>emptyList())).isEmpty();
  }

  @Test
  public void should_select_projects_including_not_finished_by_qualifiers() {
    dbTester.prepareDbUnit(getClass(), "fixture-including-ghost-projects-and-technical-project.xml");

    List<Component> components = underTest.selectProjectsIncludingNotCompletedOnesByQualifiers(newArrayList("TRK"));
    assertThat(getKeys(components)).containsOnly("org.struts:struts", "org.apache.shindig", "org.sample:sample");

    assertThat(underTest.selectProjectsIncludingNotCompletedOnesByQualifiers(newArrayList("unknown"))).isEmpty();
    assertThat(underTest.selectProjectsIncludingNotCompletedOnesByQualifiers(Collections.<String>emptyList())).isEmpty();
  }

  @Test
  public void should_select_ghosts_projects_by_qualifiers() {
    dbTester.prepareDbUnit(getClass(), "fixture-including-ghost-projects-and-technical-project.xml");

    List<Component> components = underTest.selectGhostsProjects(newArrayList("TRK"));
    assertThat(components).hasSize(1);
    assertThat(getKeys(components)).containsOnly("org.apache.shindig");

    assertThat(underTest.selectGhostsProjects(newArrayList("unknown"))).isEmpty();
    assertThat(underTest.selectGhostsProjects(Collections.<String>emptyList())).isEmpty();
  }

  @Test
  public void should_select_provisioned_projects_by_qualifiers() {
    dbTester.prepareDbUnit(getClass(), "fixture-including-ghost-projects-and-technical-project.xml");

    List<ResourceDto> components = underTest.selectProvisionedProjects(newArrayList("TRK"));
    assertThat(components).hasSize(1);
    assertThat(components.get(0).getKey()).isEqualTo("org.sample:sample");

    assertThat(underTest.selectProvisionedProjects(newArrayList("unknown"))).isEmpty();
    assertThat(underTest.selectProvisionedProjects(Collections.<String>emptyList())).isEmpty();
  }

  @Test
  public void update_authorization_date() {
    dbTester.prepareDbUnit(getClass(), "update_authorization_date.xml");

    when(system.now()).thenReturn(987654321L);
    underTest.updateAuthorizationDate(1L, dbTester.getSession());
    dbTester.getSession().commit();

    dbTester.assertDbUnit(getClass(), "update_authorization_date-result.xml", "projects");
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
