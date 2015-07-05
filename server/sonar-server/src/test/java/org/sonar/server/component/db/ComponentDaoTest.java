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

package org.sonar.server.component.db;

import java.util.Collections;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.DateUtils;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.FilePathWithHashDto;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.test.DbTests;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;

@Category(DbTests.class)
public class ComponentDaoTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  ComponentDao sut;

  @ClassRule
  public static DbTester db = new DbTester();

  DbSession session;

  @Before
  public void createDao() {
    session = db.myBatis().openSession(false);
    sut = new ComponentDao();
  }

  @After
  public void tearDown() {
    session.close();
  }

  @Test
  public void get_by_uuid() {
    loadBasicDataInDatabase();

    ComponentDto result = sut.selectNullableByUuid(session, "KLMN");
    assertThat(result).isNotNull();
    assertThat(result.uuid()).isEqualTo("KLMN");
    assertThat(result.moduleUuid()).isEqualTo("EFGH");
    assertThat(result.moduleUuidPath()).isEqualTo(".ABCD.EFGH.");
    assertThat(result.parentProjectId()).isEqualTo(2);
    assertThat(result.projectUuid()).isEqualTo("ABCD");
    assertThat(result.key()).isEqualTo("org.struts:struts-core:src/org/struts/RequestContext.java");
    assertThat(result.path()).isEqualTo("src/org/struts/RequestContext.java");
    assertThat(result.name()).isEqualTo("RequestContext.java");
    assertThat(result.longName()).isEqualTo("org.struts.RequestContext");
    assertThat(result.qualifier()).isEqualTo("FIL");
    assertThat(result.scope()).isEqualTo("FIL");
    assertThat(result.language()).isEqualTo("java");
    assertThat(result.getCopyResourceId()).isNull();
  }

  private void loadBasicDataInDatabase() {
    db.prepareDbUnit(getClass(), "shared.xml");
  }

  @Test
  public void get_by_uuid_on_technical_project_copy() {
    loadBasicDataInDatabase();

    ComponentDto result = sut.selectNullableByUuid(session, "STUV");
    assertThat(result).isNotNull();
    assertThat(result.uuid()).isEqualTo("STUV");
    assertThat(result.moduleUuid()).isEqualTo("OPQR");
    assertThat(result.moduleUuidPath()).isEqualTo(".OPQR.");
    assertThat(result.parentProjectId()).isEqualTo(11);
    assertThat(result.projectUuid()).isEqualTo("OPQR");
    assertThat(result.key()).isEqualTo("DEV:anakin@skywalker.name:org.struts:struts");
    assertThat(result.path()).isNull();
    assertThat(result.name()).isEqualTo("Apache Struts");
    assertThat(result.longName()).isEqualTo("Apache Struts");
    assertThat(result.qualifier()).isEqualTo("DEV_PRJ");
    assertThat(result.scope()).isEqualTo("PRJ");
    assertThat(result.language()).isNull();
    assertThat(result.getCopyResourceId()).isEqualTo(1L);
  }

  @Test
  public void get_by_uuid_on_disabled_component() {
    loadBasicDataInDatabase();

    ComponentDto result = sut.selectNullableByUuid(session, "DCBA");
    assertThat(result).isNotNull();
    assertThat(result.isEnabled()).isFalse();
  }

  @Test
  public void fail_to_get_by_uuid_when_component_not_found() {
    thrown.expect(NotFoundException.class);

    loadBasicDataInDatabase();

    sut.selectByUuid(session, "unknown");
  }

  @Test
  public void get_by_key() {
    loadBasicDataInDatabase();

    ComponentDto result = sut.selectNullableByKey(session, "org.struts:struts-core:src/org/struts/RequestContext.java");
    assertThat(result).isNotNull();
    assertThat(result.key()).isEqualTo("org.struts:struts-core:src/org/struts/RequestContext.java");
    assertThat(result.path()).isEqualTo("src/org/struts/RequestContext.java");
    assertThat(result.name()).isEqualTo("RequestContext.java");
    assertThat(result.longName()).isEqualTo("org.struts.RequestContext");
    assertThat(result.qualifier()).isEqualTo("FIL");
    assertThat(result.scope()).isEqualTo("FIL");
    assertThat(result.language()).isEqualTo("java");
    assertThat(result.parentProjectId()).isEqualTo(2);

    assertThat(sut.selectNullableByKey(session, "unknown")).isNull();
  }

  @Test
  public void fail_to_get_by_key_when_component_not_found() {
    thrown.expect(NotFoundException.class);

    loadBasicDataInDatabase();

    sut.selectByUuid(session, "unknown");
  }

  @Test
  public void get_by_key_on_disabled_component() {
    loadBasicDataInDatabase();

    ComponentDto result = sut.selectByKey(session, "org.disabled.project");
    assertThat(result).isNotNull();
    assertThat(result.isEnabled()).isFalse();
  }

  @Test
  public void get_by_key_on_a_root_project() {
    loadBasicDataInDatabase();

    ComponentDto result = sut.selectByKey(session, "org.struts:struts");
    assertThat(result).isNotNull();
    assertThat(result.key()).isEqualTo("org.struts:struts");
    assertThat(result.deprecatedKey()).isEqualTo("org.struts:struts");
    assertThat(result.path()).isNull();
    assertThat(result.name()).isEqualTo("Struts");
    assertThat(result.longName()).isEqualTo("Apache Struts");
    assertThat(result.description()).isEqualTo("the description");
    assertThat(result.qualifier()).isEqualTo("TRK");
    assertThat(result.scope()).isEqualTo("PRJ");
    assertThat(result.language()).isNull();
    assertThat(result.parentProjectId()).isNull();
    assertThat(result.getAuthorizationUpdatedAt()).isEqualTo(123456789L);
  }

  @Test
  public void get_by_keys() {
    loadBasicDataInDatabase();

    List<ComponentDto> results = sut.selectByKeys(session, Collections.singletonList("org.struts:struts-core:src/org/struts/RequestContext.java"));
    assertThat(results).hasSize(1);

    ComponentDto result = results.get(0);
    assertThat(result).isNotNull();
    assertThat(result.key()).isEqualTo("org.struts:struts-core:src/org/struts/RequestContext.java");
    assertThat(result.path()).isEqualTo("src/org/struts/RequestContext.java");
    assertThat(result.name()).isEqualTo("RequestContext.java");
    assertThat(result.longName()).isEqualTo("org.struts.RequestContext");
    assertThat(result.qualifier()).isEqualTo("FIL");
    assertThat(result.scope()).isEqualTo("FIL");
    assertThat(result.language()).isEqualTo("java");
    assertThat(result.parentProjectId()).isEqualTo(2);

    assertThat(sut.selectByKeys(session, Collections.singletonList("unknown"))).isEmpty();
  }

  @Test
  public void get_by_ids() {
    loadBasicDataInDatabase();

    List<ComponentDto> results = sut.selectByIds(session, newArrayList(4L));
    assertThat(results).hasSize(1);

    ComponentDto result = results.get(0);
    assertThat(result).isNotNull();
    assertThat(result.key()).isEqualTo("org.struts:struts-core:src/org/struts/RequestContext.java");
    assertThat(result.path()).isEqualTo("src/org/struts/RequestContext.java");
    assertThat(result.name()).isEqualTo("RequestContext.java");
    assertThat(result.longName()).isEqualTo("org.struts.RequestContext");
    assertThat(result.qualifier()).isEqualTo("FIL");
    assertThat(result.scope()).isEqualTo("FIL");
    assertThat(result.language()).isEqualTo("java");
    assertThat(result.parentProjectId()).isEqualTo(2);

    assertThat(sut.selectByIds(session, newArrayList(555L))).isEmpty();
  }

  @Test
  public void get_by_uuids() {
    loadBasicDataInDatabase();

    List<ComponentDto> results = sut.selectByUuids(session, newArrayList("KLMN"));
    assertThat(results).hasSize(1);

    ComponentDto result = results.get(0);
    assertThat(result).isNotNull();
    assertThat(result.uuid()).isEqualTo("KLMN");
    assertThat(result.moduleUuid()).isEqualTo("EFGH");
    assertThat(result.moduleUuidPath()).isEqualTo(".ABCD.EFGH.");
    assertThat(result.parentProjectId()).isEqualTo(2);
    assertThat(result.projectUuid()).isEqualTo("ABCD");
    assertThat(result.key()).isEqualTo("org.struts:struts-core:src/org/struts/RequestContext.java");
    assertThat(result.path()).isEqualTo("src/org/struts/RequestContext.java");
    assertThat(result.name()).isEqualTo("RequestContext.java");
    assertThat(result.longName()).isEqualTo("org.struts.RequestContext");
    assertThat(result.qualifier()).isEqualTo("FIL");
    assertThat(result.scope()).isEqualTo("FIL");
    assertThat(result.language()).isEqualTo("java");

    assertThat(sut.selectByUuids(session, newArrayList("unknown"))).isEmpty();
  }

  @Test
  public void get_by_uuids_on_removed_components() {
    loadBasicDataInDatabase();

    List<ComponentDto> results = sut.selectByUuids(session, newArrayList("DCBA"));
    assertThat(results).hasSize(1);

    ComponentDto result = results.get(0);
    assertThat(result).isNotNull();
    assertThat(result.isEnabled()).isFalse();
  }

  @Test
  public void select_existing_uuids() {
    loadBasicDataInDatabase();

    List<String> results = sut.selectExistingUuids(session, newArrayList("KLMN"));
    assertThat(results).containsOnly("KLMN");

    assertThat(sut.selectExistingUuids(session, newArrayList("KLMN", "unknown"))).hasSize(1);
    assertThat(sut.selectExistingUuids(session, newArrayList("unknown"))).isEmpty();
  }

  @Test
  public void get_by_id() {
    loadBasicDataInDatabase();

    assertThat(sut.selectById(4L, session)).isNotNull();
  }

  @Test
  public void get_by_id_on_disabled_component() {
    loadBasicDataInDatabase();

    ComponentDto result = sut.selectNullableById(10L, session);
    assertThat(result).isNotNull();
    assertThat(result.isEnabled()).isFalse();
  }

  @Test(expected = IllegalArgumentException.class)
  public void fail_to_get_by_id_when_project_not_found() {
    loadBasicDataInDatabase();

    sut.selectById(111L, session);
  }

  @Test
  public void get_nullable_by_id() {
    loadBasicDataInDatabase();

    assertThat(sut.selectNullableById(4L, session)).isNotNull();
    assertThat(sut.selectNullableById(111L, session)).isNull();
  }

  @Test
  public void count_by_id() {
    loadBasicDataInDatabase();

    assertThat(sut.existsById(4L, session)).isTrue();
    assertThat(sut.existsById(111L, session)).isFalse();
  }

  @Test
  public void find_sub_projects_by_component_keys() {
    db.prepareDbUnit(getClass(), "multi-modules.xml");

    // Sub project of a file
    List<ComponentDto> results = sut.selectSubProjectsByComponentUuids(session, newArrayList("HIJK"));
    assertThat(results).hasSize(1);
    assertThat(results.get(0).getKey()).isEqualTo("org.struts:struts-data");

    // Sub project of a directory
    results = sut.selectSubProjectsByComponentUuids(session, newArrayList("GHIJ"));
    assertThat(results).hasSize(1);
    assertThat(results.get(0).getKey()).isEqualTo("org.struts:struts-data");

    // Sub project of a sub module
    results = sut.selectSubProjectsByComponentUuids(session, newArrayList("FGHI"));
    assertThat(results).hasSize(1);
    assertThat(results.get(0).getKey()).isEqualTo("org.struts:struts");

    // Sub project of a module
    results = sut.selectSubProjectsByComponentUuids(session, newArrayList("EFGH"));
    assertThat(results).hasSize(1);
    assertThat(results.get(0).getKey()).isEqualTo("org.struts:struts");

    // Sub project of a project
    assertThat(sut.selectSubProjectsByComponentUuids(session, newArrayList("ABCD"))).isEmpty();

    // SUb projects of a component and a sub module
    assertThat(sut.selectSubProjectsByComponentUuids(session, newArrayList("HIJK", "FGHI"))).hasSize(2);

    assertThat(sut.selectSubProjectsByComponentUuids(session, newArrayList("unknown"))).isEmpty();

    assertThat(sut.selectSubProjectsByComponentUuids(session, Collections.<String>emptyList())).isEmpty();
  }

  @Test
  public void select_enabled_modules_tree() {
    db.prepareDbUnit(getClass(), "multi-modules.xml");

    // From root project
    List<ComponentDto> modules = sut.selectEnabledDescendantModules(session, "ABCD");
    assertThat(modules).extracting("uuid").containsOnly("ABCD", "EFGH", "FGHI");

    // From module
    modules = sut.selectEnabledDescendantModules(session, "EFGH");
    assertThat(modules).extracting("uuid").containsOnly("EFGH", "FGHI");

    // From sub module
    modules = sut.selectEnabledDescendantModules(session, "FGHI");
    assertThat(modules).extracting("uuid").containsOnly("FGHI");

    // Folder
    assertThat(sut.selectEnabledDescendantModules(session, "GHIJ")).isEmpty();
    assertThat(sut.selectEnabledDescendantModules(session, "unknown")).isEmpty();
  }

  @Test
  public void select_all_modules_tree() {
    db.prepareDbUnit(getClass(), "multi-modules.xml");

    // From root project, disabled sub module is returned
    List<ComponentDto> modules = sut.selectDescendantModules(session, "ABCD");
    assertThat(modules).extracting("uuid").containsOnly("ABCD", "EFGH", "FGHI", "IHGF");

    // From module, disabled sub module is returned
    modules = sut.selectDescendantModules(session, "EFGH");
    assertThat(modules).extracting("uuid").containsOnly("EFGH", "FGHI", "IHGF");

    // From removed sub module -> should not be returned
    assertThat(sut.selectDescendantModules(session, "IHGF")).isEmpty();
  }

  @Test
  public void select_enabled_module_files_tree_from_module() {
    db.prepareDbUnit(getClass(), "select_module_files_tree.xml");

    // From root project
    List<FilePathWithHashDto> files = sut.selectEnabledDescendantFiles(session, "ABCD");
    assertThat(files).extracting("uuid").containsOnly("EFGHI", "HIJK");
    assertThat(files).extracting("moduleUuid").containsOnly("EFGH", "FGHI");
    assertThat(files).extracting("srcHash").containsOnly("srcEFGHI", "srcHIJK");
    assertThat(files).extracting("path").containsOnly("src/org/struts/pom.xml", "src/org/struts/RequestContext.java");

    // From module
    files = sut.selectEnabledDescendantFiles(session, "EFGH");
    assertThat(files).extracting("uuid").containsOnly("EFGHI", "HIJK");
    assertThat(files).extracting("moduleUuid").containsOnly("EFGH", "FGHI");
    assertThat(files).extracting("srcHash").containsOnly("srcEFGHI", "srcHIJK");
    assertThat(files).extracting("path").containsOnly("src/org/struts/pom.xml", "src/org/struts/RequestContext.java");

    // From sub module
    files = sut.selectEnabledDescendantFiles(session, "FGHI");
    assertThat(files).extracting("uuid").containsOnly("HIJK");
    assertThat(files).extracting("moduleUuid").containsOnly("FGHI");
    assertThat(files).extracting("srcHash").containsOnly("srcHIJK");
    assertThat(files).extracting("path").containsOnly("src/org/struts/RequestContext.java");

    // From directory
    assertThat(sut.selectEnabledDescendantFiles(session, "GHIJ")).isEmpty();

    assertThat(sut.selectEnabledDescendantFiles(session, "unknown")).isEmpty();
  }

  @Test
  public void select_enabled_module_files_tree_from_project() {
    db.prepareDbUnit(getClass(), "select_module_files_tree.xml");

    // From root project
    List<FilePathWithHashDto> files = sut.selectEnabledFilesFromProject(session, "ABCD");
    assertThat(files).extracting("uuid").containsOnly("EFGHI", "HIJK");
    assertThat(files).extracting("moduleUuid").containsOnly("EFGH", "FGHI");
    assertThat(files).extracting("srcHash").containsOnly("srcEFGHI", "srcHIJK");
    assertThat(files).extracting("path").containsOnly("src/org/struts/pom.xml", "src/org/struts/RequestContext.java");

    // From module
    assertThat(sut.selectEnabledFilesFromProject(session, "EFGH")).isEmpty();

    // From sub module
    assertThat(sut.selectEnabledFilesFromProject(session, "FGHI")).isEmpty();

    // From directory
    assertThat(sut.selectEnabledFilesFromProject(session, "GHIJ")).isEmpty();

    assertThat(sut.selectEnabledFilesFromProject(session, "unknown")).isEmpty();
  }

  @Test
  public void select_components_from_project() {
    db.prepareDbUnit(getClass(), "multi-modules.xml");

    List<ComponentDto> components = sut.selectComponentsFromProjectKey(session, "org.struts:struts");
    assertThat(components).hasSize(5);

    assertThat(sut.selectComponentsFromProjectKey(session, "UNKNOWN")).isEmpty();
  }

  @Test
  public void select_modules_from_project() {
    db.prepareDbUnit(getClass(), "multi-modules.xml");

    List<ComponentDto> components = sut.selectModulesFromProjectKey(session, "org.struts:struts");
    assertThat(components).hasSize(3);

    assertThat(sut.selectModulesFromProjectKey(session, "UNKNOWN")).isEmpty();
  }

  @Test
  public void select_views_and_sub_views() {
    db.prepareDbUnit(getClass(), "shared_views.xml");

    assertThat(sut.selectAllViewsAndSubViews(session)).extracting("uuid").containsOnly("ABCD", "EFGH", "FGHI", "IJKL");
    assertThat(sut.selectAllViewsAndSubViews(session)).extracting("projectUuid").containsOnly("ABCD", "EFGH", "IJKL");
  }

  @Test
  public void select_projects_from_view() {
    db.prepareDbUnit(getClass(), "shared_views.xml");

    assertThat(sut.selectProjectsFromView(session, "ABCD", "ABCD")).containsOnly("JKLM");
    assertThat(sut.selectProjectsFromView(session, "EFGH", "EFGH")).containsOnly("KLMN", "JKLM");
    assertThat(sut.selectProjectsFromView(session, "FGHI", "EFGH")).containsOnly("JKLM");
    assertThat(sut.selectProjectsFromView(session, "IJKL", "IJKL")).isEmpty();
    assertThat(sut.selectProjectsFromView(session, "Unknown", "Unknown")).isEmpty();
  }

  @Test
  public void select_provisioned_projects() {
    db.prepareDbUnit(getClass(), "select_provisioned_projects.xml");

    List<ComponentDto> result = sut.selectProvisionedProjects(session, new SearchOptions(), null);
    ComponentDto project = result.get(0);

    assertThat(result).hasSize(1);
    assertThat(project.getKey()).isEqualTo("org.provisioned.project");
  }

  @Test
  public void count_provisioned_projects() {
    db.prepareDbUnit(getClass(), "select_provisioned_projects.xml");

    int numberOfProjects = sut.countProvisionedProjects(session, null);

    assertThat(numberOfProjects).isEqualTo(1);
  }

  @Test
  public void select_ghost_projects() throws Exception {
    db.prepareDbUnit(getClass(), "select_ghost_projects.xml");

    List<ComponentDto> result = sut.selectGhostProjects(session, null, new SearchOptions());

    assertThat(result).hasSize(1);
    assertThat(result.get(0).key()).isEqualTo("org.ghost.project");
    assertThat(sut.countGhostProjects(session, null)).isEqualTo(1);
  }

  @Test
  public void insert() {
    db.prepareDbUnit(getClass(), "empty.xml");

    ComponentDto componentDto = new ComponentDto()
      .setId(1L)
      .setUuid("GHIJ")
      .setProjectUuid("ABCD")
      .setModuleUuid("EFGH")
      .setModuleUuidPath(".ABCD.EFGH.")
      .setKey("org.struts:struts-core:src/org/struts/RequestContext.java")
      .setDeprecatedKey("org.struts:struts-core:src/org/struts/RequestContext.java")
      .setName("RequestContext.java")
      .setLongName("org.struts.RequestContext")
      .setQualifier("FIL")
      .setScope("FIL")
      .setLanguage("java")
      .setDescription("description")
      .setPath("src/org/struts/RequestContext.java")
      .setParentProjectId(3L)
      .setCopyResourceId(5L)
      .setEnabled(true)
      .setCreatedAt(DateUtils.parseDate("2014-06-18"))
      .setAuthorizationUpdatedAt(123456789L);

    sut.insert(session, componentDto);
    session.commit();

    assertThat(componentDto.getId()).isNotNull();
    db.assertDbUnit(getClass(), "insert-result.xml", "projects");
  }

  @Test
  public void insert_disabled_component() {
    db.prepareDbUnit(getClass(), "empty.xml");

    ComponentDto componentDto = new ComponentDto()
      .setId(1L)
      .setUuid("GHIJ")
      .setProjectUuid("ABCD")
      .setModuleUuid("EFGH")
      .setModuleUuidPath(".ABCD.EFGH.")
      .setKey("org.struts:struts-core:src/org/struts/RequestContext.java")
      .setName("RequestContext.java")
      .setLongName("org.struts.RequestContext")
      .setQualifier("FIL")
      .setScope("FIL")
      .setLanguage("java")
      .setPath("src/org/struts/RequestContext.java")
      .setParentProjectId(3L)
      .setEnabled(false)
      .setCreatedAt(DateUtils.parseDate("2014-06-18"))
      .setAuthorizationUpdatedAt(123456789L);

    sut.insert(session, componentDto);
    session.commit();

    assertThat(componentDto.getId()).isNotNull();
    db.assertDbUnit(getClass(), "insert_disabled_component-result.xml", "projects");
  }

  @Test
  public void update() {
    db.prepareDbUnit(getClass(), "update.xml");

    ComponentDto componentDto = new ComponentDto()
      .setUuid("GHIJ")
      .setProjectUuid("DCBA")
      .setModuleUuid("HGFE")
      .setModuleUuidPath(".DCBA.HGFE.")
      .setKey("org.struts:struts-core:src/org/struts/RequestContext2.java")
      .setDeprecatedKey("org.struts:struts-core:src/org/struts/RequestContext2.java")
      .setName("RequestContext2.java")
      .setLongName("org.struts.RequestContext2")
      .setQualifier("LIF")
      .setScope("LIF")
      .setLanguage("java2")
      .setDescription("description2")
      .setPath("src/org/struts/RequestContext2.java")
      .setParentProjectId(4L)
      .setCopyResourceId(6L)
      .setEnabled(false)
      .setAuthorizationUpdatedAt(12345678910L);

    sut.update(session, componentDto);
    session.commit();

    db.assertDbUnit(getClass(), "update-result.xml", "projects");
  }
}
