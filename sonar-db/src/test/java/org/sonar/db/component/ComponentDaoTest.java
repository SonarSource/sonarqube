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

import com.google.common.base.Optional;
import java.util.Collections;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.RowNotFoundException;
import org.sonar.test.DbTests;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.guava.api.Assertions.assertThat;
import static org.sonar.db.component.ComponentTesting.newDeveloper;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newModuleDto;
import static org.sonar.db.component.ComponentTesting.newProjectDto;
import static org.sonar.db.component.ComponentTesting.newView;

@Category(DbTests.class)
public class ComponentDaoTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  ComponentDbTester componentDb = new ComponentDbTester(db);

  DbSession dbSession = db.getSession();

  ComponentDao underTest = new ComponentDao();

  @Test
  public void get_by_uuid() {
    db.prepareDbUnit(getClass(), "shared.xml");

    ComponentDto result = underTest.selectByUuid(dbSession, "KLMN").get();
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

    assertThat(underTest.selectByUuid(dbSession, "UNKNOWN")).isAbsent();
  }

  @Test
  public void get_by_uuid_on_technical_project_copy() {
    db.prepareDbUnit(getClass(), "shared.xml");

    ComponentDto result = underTest.selectByUuid(dbSession, "STUV").get();
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
  public void get_by_uuid_on_developer_project_copy() {
    db.prepareDbUnit(getClass(), "shared.xml");

    ComponentDto result = underTest.selectByUuid(dbSession, "STUV").get();
    assertThat(result.getDeveloperId()).isEqualTo(11L);
  }

  @Test
  public void get_by_uuid_on_disabled_component() {
    db.prepareDbUnit(getClass(), "shared.xml");

    ComponentDto result = underTest.selectByUuid(dbSession, "DCBA").get();
    assertThat(result).isNotNull();
    assertThat(result.isEnabled()).isFalse();
  }

  @Test
  public void fail_to_get_by_uuid_when_component_not_found() {
    thrown.expect(RowNotFoundException.class);

    db.prepareDbUnit(getClass(), "shared.xml");

    underTest.selectOrFailByUuid(dbSession, "unknown");
  }

  @Test
  public void get_by_key() {
    db.prepareDbUnit(getClass(), "shared.xml");

    Optional<ComponentDto> optional = underTest.selectByKey(dbSession, "org.struts:struts-core:src/org/struts/RequestContext.java");
    assertThat(optional).isPresent();

    ComponentDto result = optional.get();
    assertThat(result.key()).isEqualTo("org.struts:struts-core:src/org/struts/RequestContext.java");
    assertThat(result.path()).isEqualTo("src/org/struts/RequestContext.java");
    assertThat(result.name()).isEqualTo("RequestContext.java");
    assertThat(result.longName()).isEqualTo("org.struts.RequestContext");
    assertThat(result.qualifier()).isEqualTo("FIL");
    assertThat(result.scope()).isEqualTo("FIL");
    assertThat(result.language()).isEqualTo("java");
    assertThat(result.parentProjectId()).isEqualTo(2);

    assertThat(underTest.selectByKey(dbSession, "unknown")).isAbsent();
  }

  @Test
  public void fail_to_get_by_key_when_component_not_found() {
    thrown.expect(RowNotFoundException.class);

    db.prepareDbUnit(getClass(), "shared.xml");

    underTest.selectOrFailByKey(dbSession, "unknown");
  }

  @Test
  public void get_by_key_on_disabled_component() {
    db.prepareDbUnit(getClass(), "shared.xml");

    ComponentDto result = underTest.selectOrFailByKey(dbSession, "org.disabled.project");
    assertThat(result.isEnabled()).isFalse();
  }

  @Test
  public void get_by_key_on_a_root_project() {
    db.prepareDbUnit(getClass(), "shared.xml");

    ComponentDto result = underTest.selectOrFailByKey(dbSession, "org.struts:struts");
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
    db.prepareDbUnit(getClass(), "shared.xml");

    List<ComponentDto> results = underTest.selectByKeys(dbSession, singletonList("org.struts:struts-core:src/org/struts/RequestContext.java"));
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

    assertThat(underTest.selectByKeys(dbSession, singletonList("unknown"))).isEmpty();
  }

  @Test
  public void get_by_ids() {
    db.prepareDbUnit(getClass(), "shared.xml");

    List<ComponentDto> results = underTest.selectByIds(dbSession, newArrayList(4L));
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

    assertThat(underTest.selectByIds(dbSession, newArrayList(555L))).isEmpty();
  }

  @Test
  public void get_by_uuids() {
    db.prepareDbUnit(getClass(), "shared.xml");

    List<ComponentDto> results = underTest.selectByUuids(dbSession, newArrayList("KLMN"));
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

    assertThat(underTest.selectByUuids(dbSession, newArrayList("unknown"))).isEmpty();
  }

  @Test
  public void get_by_uuids_on_removed_components() {
    db.prepareDbUnit(getClass(), "shared.xml");

    List<ComponentDto> results = underTest.selectByUuids(dbSession, newArrayList("DCBA"));
    assertThat(results).hasSize(1);

    ComponentDto result = results.get(0);
    assertThat(result).isNotNull();
    assertThat(result.isEnabled()).isFalse();
  }

  @Test
  public void select_existing_uuids() {
    db.prepareDbUnit(getClass(), "shared.xml");

    List<String> results = underTest.selectExistingUuids(dbSession, newArrayList("KLMN"));
    assertThat(results).containsOnly("KLMN");

    assertThat(underTest.selectExistingUuids(dbSession, newArrayList("KLMN", "unknown"))).hasSize(1);
    assertThat(underTest.selectExistingUuids(dbSession, newArrayList("unknown"))).isEmpty();
  }

  @Test
  public void get_by_id() {
    db.prepareDbUnit(getClass(), "shared.xml");

    assertThat(underTest.selectOrFailById(dbSession, 4L)).isNotNull();
  }

  @Test
  public void get_by_id_on_disabled_component() {
    db.prepareDbUnit(getClass(), "shared.xml");

    Optional<ComponentDto> result = underTest.selectById(dbSession, 10L);
    assertThat(result).isPresent();
    assertThat(result.get().isEnabled()).isFalse();
  }

  @Test(expected = RowNotFoundException.class)
  public void fail_to_get_by_id_when_project_not_found() {
    db.prepareDbUnit(getClass(), "shared.xml");

    underTest.selectOrFailById(dbSession, 111L);
  }

  @Test
  public void get_nullable_by_id() {
    db.prepareDbUnit(getClass(), "shared.xml");

    assertThat(underTest.selectById(dbSession, 4L)).isPresent();
    assertThat(underTest.selectById(dbSession, 111L)).isAbsent();
  }

  @Test
  public void count_by_id() {
    db.prepareDbUnit(getClass(), "shared.xml");

    assertThat(underTest.existsById(4L, dbSession)).isTrue();
    assertThat(underTest.existsById(111L, dbSession)).isFalse();
  }

  @Test
  public void select_component_keys_by_qualifiers() {
    db.prepareDbUnit(getClass(), "shared.xml");

    assertThat(underTest.selectComponentsByQualifiers(dbSession, newHashSet("TRK"))).extracting("kee").containsOnly("org.struts:struts", "org.disabled.project");
    assertThat(underTest.selectComponentsByQualifiers(dbSession, newHashSet("BRC"))).extracting("kee").containsOnly("org.struts:struts-core");
    assertThat(underTest.selectComponentsByQualifiers(dbSession, newHashSet("DIR"))).extracting("kee").containsOnly("org.struts:struts-core:src/org/struts");
    assertThat(underTest.selectComponentsByQualifiers(dbSession, newHashSet("FIL"))).extracting("kee").containsOnly("org.struts:struts-core:src/org/struts/RequestContext.java");
    assertThat(underTest.selectComponentsByQualifiers(dbSession, newHashSet("unknown"))).isEmpty();
  }

  @Test
  public void fail_with_IAE_select_component_keys_by_qualifiers_on_empty_qualifier() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Qualifiers cannot be empty");

    db.prepareDbUnit(getClass(), "shared.xml");
    underTest.selectComponentsByQualifiers(dbSession, Collections.<String>emptySet());
  }

  @Test
  public void find_sub_projects_by_component_keys() {
    db.prepareDbUnit(getClass(), "multi-modules.xml");

    // Sub project of a file
    List<ComponentDto> results = underTest.selectSubProjectsByComponentUuids(dbSession, newArrayList("HIJK"));
    assertThat(results).hasSize(1);
    assertThat(results.get(0).getKey()).isEqualTo("org.struts:struts-data");

    // Sub project of a directory
    results = underTest.selectSubProjectsByComponentUuids(dbSession, newArrayList("GHIJ"));
    assertThat(results).hasSize(1);
    assertThat(results.get(0).getKey()).isEqualTo("org.struts:struts-data");

    // Sub project of a sub module
    results = underTest.selectSubProjectsByComponentUuids(dbSession, newArrayList("FGHI"));
    assertThat(results).hasSize(1);
    assertThat(results.get(0).getKey()).isEqualTo("org.struts:struts");

    // Sub project of a module
    results = underTest.selectSubProjectsByComponentUuids(dbSession, newArrayList("EFGH"));
    assertThat(results).hasSize(1);
    assertThat(results.get(0).getKey()).isEqualTo("org.struts:struts");

    // Sub project of a project
    assertThat(underTest.selectSubProjectsByComponentUuids(dbSession, newArrayList("ABCD"))).isEmpty();

    // SUb projects of a component and a sub module
    assertThat(underTest.selectSubProjectsByComponentUuids(dbSession, newArrayList("HIJK", "FGHI"))).hasSize(2);

    assertThat(underTest.selectSubProjectsByComponentUuids(dbSession, newArrayList("unknown"))).isEmpty();

    assertThat(underTest.selectSubProjectsByComponentUuids(dbSession, Collections.<String>emptyList())).isEmpty();
  }

  @Test
  public void select_enabled_modules_tree() {
    db.prepareDbUnit(getClass(), "multi-modules.xml");

    // From root project
    List<ComponentDto> modules = underTest.selectEnabledDescendantModules(dbSession, "ABCD");
    assertThat(modules).extracting("uuid").containsOnly("ABCD", "EFGH", "FGHI");

    // From module
    modules = underTest.selectEnabledDescendantModules(dbSession, "EFGH");
    assertThat(modules).extracting("uuid").containsOnly("EFGH", "FGHI");

    // From sub module
    modules = underTest.selectEnabledDescendantModules(dbSession, "FGHI");
    assertThat(modules).extracting("uuid").containsOnly("FGHI");

    // Folder
    assertThat(underTest.selectEnabledDescendantModules(dbSession, "GHIJ")).isEmpty();
    assertThat(underTest.selectEnabledDescendantModules(dbSession, "unknown")).isEmpty();
  }

  @Test
  public void select_all_modules_tree() {
    db.prepareDbUnit(getClass(), "multi-modules.xml");

    // From root project, disabled sub module is returned
    List<ComponentDto> modules = underTest.selectDescendantModules(dbSession, "ABCD");
    assertThat(modules).extracting("uuid").containsOnly("ABCD", "EFGH", "FGHI", "IHGF");

    // From module, disabled sub module is returned
    modules = underTest.selectDescendantModules(dbSession, "EFGH");
    assertThat(modules).extracting("uuid").containsOnly("EFGH", "FGHI", "IHGF");

    // From removed sub module -> should not be returned
    assertThat(underTest.selectDescendantModules(dbSession, "IHGF")).isEmpty();
  }

  @Test
  public void select_enabled_module_files_tree_from_module() {
    db.prepareDbUnit(getClass(), "select_module_files_tree.xml");

    // From root project
    List<FilePathWithHashDto> files = underTest.selectEnabledDescendantFiles(dbSession, "ABCD");
    assertThat(files).extracting("uuid").containsOnly("EFGHI", "HIJK");
    assertThat(files).extracting("moduleUuid").containsOnly("EFGH", "FGHI");
    assertThat(files).extracting("srcHash").containsOnly("srcEFGHI", "srcHIJK");
    assertThat(files).extracting("path").containsOnly("src/org/struts/pom.xml", "src/org/struts/RequestContext.java");
    assertThat(files).extracting("revision").containsOnly("123456789");

    // From module
    files = underTest.selectEnabledDescendantFiles(dbSession, "EFGH");
    assertThat(files).extracting("uuid").containsOnly("EFGHI", "HIJK");
    assertThat(files).extracting("moduleUuid").containsOnly("EFGH", "FGHI");
    assertThat(files).extracting("srcHash").containsOnly("srcEFGHI", "srcHIJK");
    assertThat(files).extracting("path").containsOnly("src/org/struts/pom.xml", "src/org/struts/RequestContext.java");
    assertThat(files).extracting("revision").containsOnly("123456789");

    // From sub module
    files = underTest.selectEnabledDescendantFiles(dbSession, "FGHI");
    assertThat(files).extracting("uuid").containsOnly("HIJK");
    assertThat(files).extracting("moduleUuid").containsOnly("FGHI");
    assertThat(files).extracting("srcHash").containsOnly("srcHIJK");
    assertThat(files).extracting("path").containsOnly("src/org/struts/RequestContext.java");
    assertThat(files).extracting("revision").containsOnly("123456789");

    // From directory
    assertThat(underTest.selectEnabledDescendantFiles(dbSession, "GHIJ")).isEmpty();

    assertThat(underTest.selectEnabledDescendantFiles(dbSession, "unknown")).isEmpty();
  }

  @Test
  public void select_enabled_module_files_tree_from_project() {
    db.prepareDbUnit(getClass(), "select_module_files_tree.xml");

    // From root project
    List<FilePathWithHashDto> files = underTest.selectEnabledFilesFromProject(dbSession, "ABCD");
    assertThat(files).extracting("uuid").containsOnly("EFGHI", "HIJK");
    assertThat(files).extracting("moduleUuid").containsOnly("EFGH", "FGHI");
    assertThat(files).extracting("srcHash").containsOnly("srcEFGHI", "srcHIJK");
    assertThat(files).extracting("path").containsOnly("src/org/struts/pom.xml", "src/org/struts/RequestContext.java");

    // From module
    assertThat(underTest.selectEnabledFilesFromProject(dbSession, "EFGH")).isEmpty();

    // From sub module
    assertThat(underTest.selectEnabledFilesFromProject(dbSession, "FGHI")).isEmpty();

    // From directory
    assertThat(underTest.selectEnabledFilesFromProject(dbSession, "GHIJ")).isEmpty();

    assertThat(underTest.selectEnabledFilesFromProject(dbSession, "unknown")).isEmpty();
  }

  @Test
  public void select_all_components_from_project() {
    db.prepareDbUnit(getClass(), "multi-modules.xml");

    List<ComponentDto> components = underTest.selectAllComponentsFromProjectKey(dbSession, "org.struts:struts");
    // Removed components are included
    assertThat(components).hasSize(8);

    assertThat(underTest.selectAllComponentsFromProjectKey(dbSession, "UNKNOWN")).isEmpty();
  }

  @Test
  public void select_modules_from_project() {
    db.prepareDbUnit(getClass(), "multi-modules.xml");

    List<ComponentDto> components = underTest.selectEnabledModulesFromProjectKey(dbSession, "org.struts:struts");
    assertThat(components).hasSize(3);

    assertThat(underTest.selectEnabledModulesFromProjectKey(dbSession, "UNKNOWN")).isEmpty();
  }

  @Test
  public void select_views_and_sub_views() {
    db.prepareDbUnit(getClass(), "shared_views.xml");

    assertThat(underTest.selectAllViewsAndSubViews(dbSession)).extracting("uuid").containsOnly("ABCD", "EFGH", "FGHI", "IJKL");
    assertThat(underTest.selectAllViewsAndSubViews(dbSession)).extracting("projectUuid").containsOnly("ABCD", "EFGH", "IJKL");
  }

  @Test
  public void select_projects_from_view() {
    db.prepareDbUnit(getClass(), "shared_views.xml");

    assertThat(underTest.selectProjectsFromView(dbSession, "ABCD", "ABCD")).containsOnly("JKLM");
    assertThat(underTest.selectProjectsFromView(dbSession, "EFGH", "EFGH")).containsOnly("KLMN", "JKLM");
    assertThat(underTest.selectProjectsFromView(dbSession, "FGHI", "EFGH")).containsOnly("JKLM");
    assertThat(underTest.selectProjectsFromView(dbSession, "IJKL", "IJKL")).isEmpty();
    assertThat(underTest.selectProjectsFromView(dbSession, "Unknown", "Unknown")).isEmpty();
  }

  @Test
  public void select_projects() {
    db.prepareDbUnit(getClass(), "select_provisioned_projects.xml");

    List<ComponentDto> result = underTest.selectProjects(dbSession);

    assertThat(result).extracting("id").containsOnly(42L, 1L);
  }

  @Test
  public void select_provisioned_projects() {
    db.prepareDbUnit(getClass(), "select_provisioned_projects.xml");

    List<ComponentDto> result = underTest.selectProvisionedProjects(dbSession, 0, 10, null);
    ComponentDto project = result.get(0);

    assertThat(result).hasSize(1);
    assertThat(project.getKey()).isEqualTo("org.provisioned.project");
  }

  @Test
  public void count_provisioned_projects() {
    db.prepareDbUnit(getClass(), "select_provisioned_projects.xml");

    int numberOfProjects = underTest.countProvisionedProjects(dbSession, null);

    assertThat(numberOfProjects).isEqualTo(1);
  }

  @Test
  public void select_ghost_projects() {
    db.prepareDbUnit(getClass(), "select_ghost_projects.xml");

    List<ComponentDto> result = underTest.selectGhostProjects(dbSession, 0, 10, null);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).key()).isEqualTo("org.ghost.project");
    assertThat(underTest.countGhostProjects(dbSession, null)).isEqualTo(1);
  }

  @Test
  public void selectResourcesByRootId() {
    db.prepareDbUnit(getClass(), "shared.xml");

    List<ComponentDto> resources = underTest.selectByProjectUuid("ABCD", dbSession);

    assertThat(resources).extracting("id").containsOnly(1l, 2l, 3l, 4l);
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
      .setDeveloperId(7L)
      .setEnabled(true)
      .setCreatedAt(DateUtils.parseDate("2014-06-18"))
      .setAuthorizationUpdatedAt(123456789L);

    underTest.insert(dbSession, componentDto);
    dbSession.commit();

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

    underTest.insert(dbSession, componentDto);
    dbSession.commit();

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
      .setDeveloperId(9L)
      .setEnabled(false)
      .setAuthorizationUpdatedAt(12345678910L);

    underTest.update(dbSession, componentDto);
    dbSession.commit();

    db.assertDbUnit(getClass(), "update-result.xml", "projects");
  }

  @Test
  public void delete() throws Exception {
    ComponentDto project1 = componentDb.insertComponent(newProjectDto().setKey("PROJECT_1"));
    componentDb.insertComponent(newProjectDto().setKey("PROJECT_2"));

    underTest.delete(dbSession, project1.getId());
    dbSession.commit();

    assertThat(underTest.selectByKey(dbSession, "PROJECT_1")).isAbsent();
    assertThat(underTest.selectByKey(dbSession, "PROJECT_2")).isPresent();
  }

  @Test
  public void select_components_with_paging_query_and_qualifiers() {
    underTest.insert(dbSession, newProjectDto().setName("aaaa-name"));
    underTest.insert(dbSession, newView());
    underTest.insert(dbSession, newDeveloper("project-name"));
    for (int i = 9; i >= 1; i--) {
      underTest.insert(dbSession, newProjectDto().setName("project-" + i));
    }

    List<ComponentDto> result = underTest.selectComponents(dbSession, singleton(Qualifiers.PROJECT), 1, 3, "project");

    assertThat(result).hasSize(3);
    assertThat(result).extracting("name").containsExactly("project-2", "project-3", "project-4");
  }

  @Test
  public void select_by_query_with_paging_query_and_qualifiers() {
    componentDb.insertProjectAndSnapshot(newProjectDto().setName("aaaa-name"));
    componentDb.insertProjectAndSnapshot(newView());
    componentDb.insertProjectAndSnapshot(newDeveloper("project-name"));
    for (int i = 9; i >= 1; i--) {
      componentDb.insertProjectAndSnapshot(newProjectDto().setName("project-" + i));
    }
    db.commit();
    componentDb.indexProjects();

    ComponentQuery query = new ComponentQuery("oJect", null, Qualifiers.PROJECT);
    List<ComponentDto> result = underTest.selectByQuery(dbSession, query, 1, 3);

    assertThat(result).hasSize(3);
    assertThat(underTest.countByQuery(dbSession, query)).isEqualTo(9);
    assertThat(result).extracting("name").containsExactly("project-2", "project-3", "project-4");
  }

  @Test
  public void select_by_query_name_with_special_characters() {
    componentDb.insertProjectAndSnapshot(newProjectDto().setName("project-\\_%/-name"));
    db.commit();
    componentDb.indexProjects();

    ComponentQuery query = new ComponentQuery("-\\_%/-", null, Qualifiers.PROJECT);
    List<ComponentDto> result = underTest.selectByQuery(dbSession, query, 0, 10);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).name()).isEqualTo("project-\\_%/-name");
  }

  @Test
  public void select_by_query_key_with_special_characters() {
    componentDb.insertProjectAndSnapshot(newProjectDto()
      .setKey("project-_%-key"));
    db.commit();
    componentDb.indexProjects();

    ComponentQuery query = new ComponentQuery("project-_%-", null, Qualifiers.PROJECT);
    List<ComponentDto> result = underTest.selectByQuery(dbSession, query, 0, 10);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).key()).isEqualTo("project-_%-key");
  }

  @Test
  public void select_by_query_filter_on_language() {
    componentDb.insertComponent(newProjectDto().setKey("java-project-key").setLanguage("java"));
    componentDb.insertComponent(newProjectDto().setKey("cpp-project-key").setLanguage("cpp"));
    db.commit();

    ComponentQuery query = new ComponentQuery(null, "java", Qualifiers.PROJECT);
    List<ComponentDto> result = underTest.selectByQuery(dbSession, query, 0, 10);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).key()).isEqualTo("java-project-key");
  }

  @Test
  public void select_direct_children_of_a_project() {
    ComponentDto project = newProjectDto().setKey("project-key").setUuid("project-uuid");
    SnapshotDto projectSnapshot = componentDb.insertProjectAndSnapshot(project);
    SnapshotDto moduleSnapshot = componentDb.insertComponentAndSnapshot(newModuleDto("module-1-uuid", project), projectSnapshot);
    componentDb.insertComponentAndSnapshot(newFileDto(project, "file-1-uuid"), projectSnapshot);
    componentDb.insertComponentAndSnapshot(newFileDto(project, "file-2-uuid"), moduleSnapshot);
    db.commit();
    componentDb.indexProjects();

    ComponentTreeQuery query = newTreeQuery(projectSnapshot).build();

    List<ComponentDto> result = underTest.selectDirectChildren(dbSession, query);
    int count = underTest.countDirectChildren(dbSession, query);

    assertThat(count).isEqualTo(2);
    assertThat(result).extracting("uuid").containsExactly("file-1-uuid", "module-1-uuid");
  }

  @Test
  public void select_direct_children_with_name_query() {
    ComponentDto project = newProjectDto().setKey("project-key").setUuid("project-uuid");
    SnapshotDto projectSnapshot = componentDb.insertProjectAndSnapshot(project);
    SnapshotDto moduleSnapshot = componentDb.insertComponentAndSnapshot(newModuleDto("module-1-uuid", project), projectSnapshot);
    componentDb.insertComponentAndSnapshot(newFileDto(project, "file-1-uuid").setName("file-name-1"), projectSnapshot);
    componentDb.insertComponentAndSnapshot(newFileDto(project, "file-2-uuid").setName("file-name-2"), moduleSnapshot);
    db.commit();
    componentDb.indexProjects();

    ComponentTreeQuery query = newTreeQuery(projectSnapshot)
      .setNameOrKeyQuery("file-name").build();

    List<ComponentDto> result = underTest.selectDirectChildren(dbSession, query);
    int count = underTest.countDirectChildren(dbSession, query);

    assertThat(count).isEqualTo(1);
    assertThat(result).extracting("uuid").containsExactly("file-1-uuid");
  }

  @Test
  public void select_direct_children_with_key_query() {
    ComponentDto project = newProjectDto().setKey("project-key").setUuid("project-uuid");
    SnapshotDto projectSnapshot = componentDb.insertProjectAndSnapshot(project);
    SnapshotDto moduleSnapshot = componentDb.insertComponentAndSnapshot(newModuleDto("module-1-uuid", project), projectSnapshot);
    componentDb.insertComponentAndSnapshot(newFileDto(project, "file-1-uuid").setKey("file-key-1"), projectSnapshot);
    componentDb.insertComponentAndSnapshot(newFileDto(project, "file-2-uuid").setKey("file-key-2"), moduleSnapshot);
    db.commit();
    componentDb.indexProjects();

    ComponentTreeQuery query = newTreeQuery(projectSnapshot)
      .setNameOrKeyQuery("file-key").build();

    List<ComponentDto> result = underTest.selectDirectChildren(dbSession, query);
    int count = underTest.countDirectChildren(dbSession, query);

    assertThat(count).isEqualTo(1);
    assertThat(result).extracting("uuid").containsExactly("file-1-uuid");
  }

  @Test
  public void select_direct_children_with_pagination() {
    ComponentDto project = newProjectDto().setKey("project-key").setUuid("project-uuid");
    SnapshotDto projectSnapshot = componentDb.insertProjectAndSnapshot(project);
    for (int i = 1; i <= 9; i++) {
      componentDb.insertComponentAndSnapshot(newFileDto(project, "file-uuid-" + i), projectSnapshot);
    }
    db.commit();
    componentDb.indexProjects();

    ComponentTreeQuery query = newTreeQuery(projectSnapshot)
      .setPage(2)
      .setPageSize(3)
      .setAsc(false)
      .build();

    List<ComponentDto> result = underTest.selectDirectChildren(dbSession, query);
    int count = underTest.countDirectChildren(dbSession, query);

    assertThat(count).isEqualTo(9);
    assertThat(result).extracting("uuid").containsExactly("file-uuid-6", "file-uuid-5", "file-uuid-4");
  }

  @Test
  public void select_direct_children_with_order_by_path() {
    ComponentDto project = newProjectDto();
    SnapshotDto projectSnapshot = componentDb.insertProjectAndSnapshot(project);
    componentDb.insertComponentAndSnapshot(newFileDto(project, "file-uuid-1").setName("file-name-1").setPath("3"), projectSnapshot);
    componentDb.insertComponentAndSnapshot(newFileDto(project, "file-uuid-2").setName("file-name-2").setPath("2"), projectSnapshot);
    componentDb.insertComponentAndSnapshot(newFileDto(project, "file-uuid-3").setName("file-name-3").setPath("1"), projectSnapshot);
    db.commit();
    componentDb.indexProjects();

    ComponentTreeQuery query = newTreeQuery(projectSnapshot)
      .setSortFields(singletonList("path"))
      .setAsc(true)
      .build();

    List<ComponentDto> result = underTest.selectDirectChildren(dbSession, query);

    assertThat(result).extracting("uuid").containsExactly("file-uuid-3", "file-uuid-2", "file-uuid-1");
  }

  @Test
  public void select_direct_children_of_a_module() {
    ComponentDto project = newProjectDto();
    SnapshotDto projectSnapshot = componentDb.insertProjectAndSnapshot(project);
    SnapshotDto moduleSnapshot = componentDb.insertComponentAndSnapshot(newModuleDto("module-1-uuid", project), projectSnapshot);
    componentDb.insertComponentAndSnapshot(newFileDto(project, "file-1-uuid"), projectSnapshot);
    componentDb.insertComponentAndSnapshot(newFileDto(project, "file-2-uuid"), moduleSnapshot);
    db.commit();
    componentDb.indexProjects();

    ComponentTreeQuery query = newTreeQuery(moduleSnapshot).build();

    List<ComponentDto> result = underTest.selectDirectChildren(dbSession, query);

    assertThat(result).extracting("uuid").containsOnly("file-2-uuid");
  }

  @Test
  public void select_all_children_of_a_project() {
    ComponentDto project = newProjectDto().setKey("project-key").setUuid("project-uuid");
    SnapshotDto projectSnapshot = componentDb.insertProjectAndSnapshot(project);
    SnapshotDto moduleSnapshot = componentDb.insertComponentAndSnapshot(newModuleDto("module-1-uuid", project), projectSnapshot);
    componentDb.insertComponentAndSnapshot(newFileDto(project, "file-1-uuid"), projectSnapshot);
    componentDb.insertComponentAndSnapshot(newFileDto(project, "file-2-uuid"), moduleSnapshot);
    db.commit();
    componentDb.indexProjects();

    ComponentTreeQuery query = newTreeQuery(projectSnapshot).build();

    List<ComponentDto> result = underTest.selectAllChildren(dbSession, query);
    int count = underTest.countAllChildren(dbSession, query);

    assertThat(count).isEqualTo(3);
    assertThat(result).extracting("uuid").containsExactly("file-1-uuid", "file-2-uuid", "module-1-uuid");
  }

  @Test
  public void select_all_files_of_a_project_paginated_and_ordered() {
    ComponentDto project = newProjectDto().setKey("project-key").setUuid("project-uuid");
    SnapshotDto projectSnapshot = componentDb.insertProjectAndSnapshot(project);
    SnapshotDto moduleSnapshot = componentDb.insertComponentAndSnapshot(newModuleDto("module-1-uuid", project), projectSnapshot);
    componentDb.insertComponentAndSnapshot(newFileDto(project, "file-uuid-1").setName("file-name-1"), projectSnapshot);
    componentDb.insertComponentAndSnapshot(newFileDto(project, "another-uuid"), projectSnapshot);
    for (int i = 2; i <= 9; i++) {
      componentDb.insertComponentAndSnapshot(newFileDto(project, "file-uuid-" + i).setName("file-name-" + i), moduleSnapshot);
    }
    db.commit();
    componentDb.indexProjects();

    ComponentTreeQuery query = newTreeQuery(projectSnapshot)
      .setQualifiers(newArrayList(Qualifiers.FILE))
      .setPage(2)
      .setPageSize(3)
      .setNameOrKeyQuery("file-name")
      .setSortFields(singletonList("name"))
      .setAsc(false)
      .build();

    List<ComponentDto> result = underTest.selectAllChildren(dbSession, query);
    int count = underTest.countAllChildren(dbSession, query);

    assertThat(count).isEqualTo(9);
    assertThat(result).extracting("uuid").containsExactly("file-uuid-6", "file-uuid-5", "file-uuid-4");
  }

  private static ComponentTreeQuery.Builder newTreeQuery(SnapshotDto baseSnapshot) {
    return ComponentTreeQuery.builder()
      .setPage(1)
      .setPageSize(500)
      .setBaseSnapshot(baseSnapshot)
      .setSortFields(singletonList("name"))
      .setAsc(true)
      .setQualifiers(newArrayList(Qualifiers.FILE, Qualifiers.MODULE, Qualifiers.DIRECTORY, Qualifiers.PROJECT));
  }
}
