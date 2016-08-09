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

import com.google.common.base.Optional;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.RowNotFoundException;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.guava.api.Assertions.assertThat;
import static org.sonar.db.component.ComponentTesting.newDeveloper;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newModuleDto;
import static org.sonar.db.component.ComponentTesting.newProjectCopy;
import static org.sonar.db.component.ComponentTesting.newProjectDto;
import static org.sonar.db.component.ComponentTesting.newSubView;
import static org.sonar.db.component.ComponentTesting.newView;

public class ComponentDaoTest {

  private static final String PROJECT_UUID = "project-uuid";
  private static final String MODULE_UUID = "module-uuid";
  private static final String FILE_1_UUID = "file-1-uuid";
  private static final String FILE_2_UUID = "file-2-uuid";
  private static final String FILE_3_UUID = "file-3-uuid";
  private static final String A_VIEW_UUID = "view-uuid";

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  ComponentDbTester componentDb = new ComponentDbTester(db);

  final DbSession dbSession = db.getSession();

  ComponentDao underTest = new ComponentDao();

  @Test
  public void get_by_uuid() {
    db.prepareDbUnit(getClass(), "shared.xml");

    ComponentDto result = underTest.selectByUuid(dbSession, "U1").get();
    assertThat(result).isNotNull();
    assertThat(result.uuid()).isEqualTo("U1");
    assertThat(result.getUuidPath()).isEqualTo("uuid_path_of_U1");
    assertThat(result.moduleUuid()).isEqualTo("module_uuid_of_U1");
    assertThat(result.moduleUuidPath()).isEqualTo("module_uuid_path_of_U1");
    assertThat(result.getRootUuid()).isEqualTo("root_uuid_of_U1");
    assertThat(result.projectUuid()).isEqualTo("project_uuid_of_U1");
    assertThat(result.key()).isEqualTo("org.struts:struts");
    assertThat(result.path()).isEqualTo("path_of_U1");
    assertThat(result.name()).isEqualTo("Struts");
    assertThat(result.longName()).isEqualTo("Apache Struts");
    assertThat(result.qualifier()).isEqualTo("TRK");
    assertThat(result.scope()).isEqualTo("PRJ");
    assertThat(result.language()).isEqualTo("java");
    assertThat(result.getCopyResourceUuid()).isNull();
    assertThat(result.getDeveloperUuid()).isNull();

    assertThat(underTest.selectByUuid(dbSession, "UNKNOWN")).isAbsent();
  }

  @Test
  public void get_by_uuid_on_technical_project_copy() {
    db.prepareDbUnit(getClass(), "shared.xml");

    ComponentDto result = underTest.selectByUuid(dbSession, "U7").get();
    assertThat(result).isNotNull();
    assertThat(result.uuid()).isEqualTo("U7");
    assertThat(result.moduleUuid()).isEqualTo("module_uuid_of_U7");
    assertThat(result.moduleUuidPath()).isEqualTo("module_uuid_path_of_U7");
    assertThat(result.getRootUuid()).isEqualTo("root_uuid_of_U7");
    assertThat(result.projectUuid()).isEqualTo("project_uuid_of_U7");
    assertThat(result.key()).isEqualTo("DEV:anakin@skywalker.name:org.struts:struts");
    assertThat(result.path()).isNull();
    assertThat(result.name()).isEqualTo("Apache Struts");
    assertThat(result.longName()).isEqualTo("Apache Struts");
    assertThat(result.qualifier()).isEqualTo("DEV_PRJ");
    assertThat(result.scope()).isEqualTo("PRJ");
    assertThat(result.language()).isNull();
    assertThat(result.getCopyResourceUuid()).isEqualTo("U1");
    assertThat(result.getDeveloperUuid()).isEqualTo("developer_uuid_of_U7");
  }

  @Test
  public void selectByUuidon_developer_project_copy() {
    db.prepareDbUnit(getClass(), "shared.xml");

    ComponentDto result = underTest.selectByUuid(dbSession, "U7").get();
    assertThat(result.getDeveloperUuid()).isEqualTo("developer_uuid_of_U7");
  }

  @Test
  public void selectByUuid_on_disabled_component() {
    db.prepareDbUnit(getClass(), "shared.xml");

    ComponentDto result = underTest.selectByUuid(dbSession, "U5").get();
    assertThat(result).isNotNull();
    assertThat(result.uuid()).isEqualTo("U5");
    assertThat(result.isEnabled()).isFalse();
  }

  @Test
  public void selectOrFailByUuid_fails_when_component_not_found() {
    thrown.expect(RowNotFoundException.class);

    db.prepareDbUnit(getClass(), "shared.xml");

    underTest.selectOrFailByUuid(dbSession, "unknown");
  }

  @Test
  public void selectByKey() {
    db.prepareDbUnit(getClass(), "shared.xml");

    Optional<ComponentDto> optional = underTest.selectByKey(dbSession, "org.struts:struts-core:src/org/struts/RequestContext.java");
    assertThat(optional).isPresent();

    ComponentDto result = optional.get();
    assertThat(result.key()).isEqualTo("org.struts:struts-core:src/org/struts/RequestContext.java");
    assertThat(result.path()).isEqualTo("path_of_U4");
    assertThat(result.name()).isEqualTo("RequestContext.java");
    assertThat(result.longName()).isEqualTo("org.struts.RequestContext");
    assertThat(result.qualifier()).isEqualTo("FIL");
    assertThat(result.scope()).isEqualTo("FIL");
    assertThat(result.language()).isEqualTo("java");
    assertThat(result.uuid()).isEqualTo("U4");
    assertThat(result.getRootUuid()).isEqualTo("U1");

    assertThat(underTest.selectByKey(dbSession, "unknown")).isAbsent();
  }

  @Test
  public void selectOrFailByKey_fails_when_component_not_found() {
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
    assertThat(result.uuid()).isEqualTo("U1");
    assertThat(result.getUuidPath()).isEqualTo("uuid_path_of_U1");
    assertThat(result.deprecatedKey()).isEqualTo("org.struts:struts");
    assertThat(result.path()).isEqualToIgnoringCase("path_of_U1");
    assertThat(result.name()).isEqualTo("Struts");
    assertThat(result.longName()).isEqualTo("Apache Struts");
    assertThat(result.description()).isEqualTo("the description");
    assertThat(result.qualifier()).isEqualTo("TRK");
    assertThat(result.scope()).isEqualTo("PRJ");
    assertThat(result.getRootUuid()).isEqualTo("root_uuid_of_U1");
    assertThat(result.getAuthorizationUpdatedAt()).isEqualTo(123_456_789L);
  }

  @Test
  public void get_by_keys() {
    db.prepareDbUnit(getClass(), "shared.xml");

    List<ComponentDto> results = underTest.selectByKeys(dbSession, singletonList("org.struts:struts-core:src/org/struts/RequestContext.java"));
    assertThat(results).hasSize(1);

    ComponentDto result = results.get(0);
    assertThat(result).isNotNull();
    assertThat(result.key()).isEqualTo("org.struts:struts-core:src/org/struts/RequestContext.java");
    assertThat(result.path()).isEqualTo("path_of_U4");
    assertThat(result.name()).isEqualTo("RequestContext.java");
    assertThat(result.longName()).isEqualTo("org.struts.RequestContext");
    assertThat(result.qualifier()).isEqualTo("FIL");
    assertThat(result.scope()).isEqualTo("FIL");
    assertThat(result.language()).isEqualTo("java");
    assertThat(result.getRootUuid()).isEqualTo("U1");

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
    assertThat(result.path()).isEqualTo("path_of_U4");
    assertThat(result.name()).isEqualTo("RequestContext.java");
    assertThat(result.longName()).isEqualTo("org.struts.RequestContext");
    assertThat(result.qualifier()).isEqualTo("FIL");
    assertThat(result.scope()).isEqualTo("FIL");
    assertThat(result.language()).isEqualTo("java");
    assertThat(result.getRootUuid()).isEqualTo("U1");

    assertThat(underTest.selectByIds(dbSession, newArrayList(555L))).isEmpty();
  }

  @Test
  public void get_by_uuids() {
    db.prepareDbUnit(getClass(), "shared.xml");

    List<ComponentDto> results = underTest.selectByUuids(dbSession, newArrayList("U4"));
    assertThat(results).hasSize(1);

    ComponentDto result = results.get(0);
    assertThat(result).isNotNull();
    assertThat(result.uuid()).isEqualTo("U4");
    assertThat(result.moduleUuid()).isEqualTo("module_uuid_of_U4");
    assertThat(result.moduleUuidPath()).isEqualTo("module_uuid_path_of_U4");
    assertThat(result.getRootUuid()).isEqualTo("U1");
    assertThat(result.projectUuid()).isEqualTo("U1");
    assertThat(result.key()).isEqualTo("org.struts:struts-core:src/org/struts/RequestContext.java");
    assertThat(result.path()).isEqualTo("path_of_U4");
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

    List<ComponentDto> results = underTest.selectByUuids(dbSession, newArrayList("U5"));
    assertThat(results).hasSize(1);

    ComponentDto result = results.get(0);
    assertThat(result).isNotNull();
    assertThat(result.uuid()).isEqualTo("U5");
    assertThat(result.isEnabled()).isFalse();
  }

  @Test
  public void select_existing_uuids() {
    db.prepareDbUnit(getClass(), "shared.xml");

    List<String> results = underTest.selectExistingUuids(dbSession, newArrayList("U4"));
    assertThat(results).containsOnly("U4");

    assertThat(underTest.selectExistingUuids(dbSession, newArrayList("U4", "unknown"))).hasSize(1);
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
    assertThat(underTest.selectSubProjectsByComponentUuids(dbSession, newArrayList("ABCD"))).extracting("uuid").containsOnly("ABCD");

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
  public void selectByProjectUuid() {
    db.prepareDbUnit(getClass(), "shared.xml");

    List<ComponentDto> components = underTest.selectByProjectUuid("U1", dbSession);

    assertThat(components).extracting("id").containsOnly(2l, 3l, 4l);
  }

  @Test
  public void insert() {
    db.prepareDbUnit(getClass(), "empty.xml");

    ComponentDto componentDto = new ComponentDto()
      .setUuid("GHIJ")
      .setUuidPath("ABCD.EFGH.GHIJ.")
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
      .setRootUuid("uuid_3")
      .setCopyComponentUuid("uuid_5")
      .setDeveloperUuid("uuid_7")
      .setEnabled(true)
      .setCreatedAt(DateUtils.parseDate("2014-06-18"))
      .setAuthorizationUpdatedAt(123456789L);

    underTest.insert(dbSession, componentDto);
    dbSession.commit();

    assertThat(componentDto.getId()).isNotNull();
    db.assertDbUnit(getClass(), "insert-result.xml", "projects");
  }

  @Test
  public void insertBatch() {
    try (DbSession batchSession = db.myBatis().openSession(true)) {
      db.prepareDbUnit(getClass(), "empty.xml");

      ComponentDto componentDto = new ComponentDto()
        .setUuid("GHIJ")
        .setUuidPath("ABCD.EFGH.GHIJ.")
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
        .setRootUuid("uuid_3")
        .setCopyComponentUuid("uuid_5")
        .setDeveloperUuid("uuid_7")
        .setEnabled(true)
        .setCreatedAt(DateUtils.parseDate("2014-06-18"))
        .setAuthorizationUpdatedAt(123456789L);

      underTest.insertBatch(batchSession, componentDto);
      batchSession.commit();

      assertThat(componentDto.getId()).isNull();
      db.assertDbUnit(getClass(), "insert-result.xml", "projects");
    }
  }

  @Test
  public void insert_disabled_component() {
    db.prepareDbUnit(getClass(), "empty.xml");

    ComponentDto componentDto = new ComponentDto()
      .setId(1L)
      .setUuid("GHIJ")
      .setUuidPath("ABCD.EFGH.GHIJ.")
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
      .setRootUuid("uuid_3")
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
    ComponentDto dto = ComponentTesting.newProjectDto("U1");
    underTest.insert(dbSession, dto);
    dbSession.commit();

    underTest.update(dbSession, new ComponentUpdateDto()
      .setUuid("U1")
      .setBCopyComponentUuid("copy")
      .setBChanged(true)
      .setBDescription("desc")
      .setBEnabled(true)
      .setBUuidPath("uuid_path")
      .setBLanguage("lang")
      .setBLongName("longName")
      .setBModuleUuid("moduleUuid")
      .setBModuleUuidPath("moduleUuidPath")
      .setBName("name")
      .setBPath("path")
      .setBQualifier("qualifier")
      );
    dbSession.commit();

    Map<String, Object> row = selectBColumnsForUuid("U1");
    assertThat(row.get("bChanged")).isIn(true, /* for Oracle */1L, 1);
    assertThat(row.get("bCopyComponentUuid")).isEqualTo("copy");
    assertThat(row.get("bDescription")).isEqualTo("desc");
    assertThat(row.get("bEnabled")).isIn(true, /* for Oracle */1L, 1);
    assertThat(row.get("bUuidPath")).isEqualTo("uuid_path");
    assertThat(row.get("bLanguage")).isEqualTo("lang");
    assertThat(row.get("bLongName")).isEqualTo("longName");
    assertThat(row.get("bModuleUuid")).isEqualTo("moduleUuid");
    assertThat(row.get("bModuleUuidPath")).isEqualTo("moduleUuidPath");
    assertThat(row.get("bName")).isEqualTo("name");
    assertThat(row.get("bPath")).isEqualTo("path");
    assertThat(row.get("bQualifier")).isEqualTo("qualifier");
  }

  @Test
  public void updateBEnabledToFalse() {
    ComponentDto dto1 = ComponentTesting.newProjectDto("U1");
    ComponentDto dto2 = ComponentTesting.newProjectDto("U2");
    ComponentDto dto3 = ComponentTesting.newProjectDto("U3");
    underTest.insert(dbSession, dto1, dto2, dto3);

    underTest.updateBEnabledToFalse(dbSession, asList("U1", "U2"));
    dbSession.commit();

    Map<String, Object> row1 = selectBColumnsForUuid("U1");
    assertThat(row1.get("bChanged")).isIn(true, /* for Oracle */1L, 1);
    assertThat(row1.get("bCopyComponentUuid")).isEqualTo(dto1.getCopyResourceUuid());
    assertThat(row1.get("bDescription")).isEqualTo(dto1.description());
    assertThat(row1.get("bEnabled")).isIn(false, /* for Oracle */0L, 0);
    assertThat(row1.get("bUuidPath")).isEqualTo(dto1.getUuidPath());
    assertThat(row1.get("bLanguage")).isEqualTo(dto1.language());
    assertThat(row1.get("bLongName")).isEqualTo(dto1.longName());
    assertThat(row1.get("bModuleUuid")).isEqualTo(dto1.moduleUuid());
    assertThat(row1.get("bModuleUuidPath")).isEqualTo(dto1.moduleUuidPath());
    assertThat(row1.get("bName")).isEqualTo(dto1.name());
    assertThat(row1.get("bPath")).isEqualTo(dto1.path());
    assertThat(row1.get("bQualifier")).isEqualTo(dto1.qualifier());

    Map<String, Object> row2 = selectBColumnsForUuid("U2");
    assertThat(row2.get("bChanged")).isIn(true, /* for Oracle */1L, 1);
    assertThat(row2.get("bCopyComponentUuid")).isEqualTo(dto2.getCopyResourceUuid());
    assertThat(row2.get("bDescription")).isEqualTo(dto2.description());
    assertThat(row2.get("bEnabled")).isIn(false, /* for Oracle */0L, 0);
    assertThat(row2.get("bUuidPath")).isEqualTo(dto2.getUuidPath());
    assertThat(row2.get("bLanguage")).isEqualTo(dto2.language());
    assertThat(row2.get("bLongName")).isEqualTo(dto2.longName());
    assertThat(row2.get("bModuleUuid")).isEqualTo(dto2.moduleUuid());
    assertThat(row2.get("bModuleUuidPath")).isEqualTo(dto2.moduleUuidPath());
    assertThat(row2.get("bName")).isEqualTo(dto2.name());
    assertThat(row2.get("bPath")).isEqualTo(dto2.path());
    assertThat(row2.get("bQualifier")).isEqualTo(dto2.qualifier());

    Map<String, Object> row3 = selectBColumnsForUuid("U3");
    assertThat(row3.get("bChanged")).isIn(false, /* for Oracle */0L, 0);
  }

  private Map<String, Object> selectBColumnsForUuid(String uuid) {
    return db.selectFirst(
      "select b_changed as \"bChanged\", b_copy_component_uuid as \"bCopyComponentUuid\", b_description as \"bDescription\", " +
        "b_enabled as \"bEnabled\", b_uuid_path as \"bUuidPath\", b_language as \"bLanguage\", b_long_name as \"bLongName\"," +
        "b_module_uuid as \"bModuleUuid\", b_module_uuid_path as \"bModuleUuidPath\", b_name as \"bName\", " +
        "b_path as \"bPath\", b_qualifier as \"bQualifier\" " +
        "from projects where uuid='" + uuid + "'");
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
  public void select_by_query_with_paging_query_and_qualifiers() {
    componentDb.insertProjectAndSnapshot(newProjectDto().setName("aaaa-name"));
    componentDb.insertProjectAndSnapshot(newView());
    componentDb.insertProjectAndSnapshot(newDeveloper("project-name"));
    for (int i = 9; i >= 1; i--) {
      componentDb.insertProjectAndSnapshot(newProjectDto().setName("project-" + i));
    }
    componentDb.indexAllComponents();

    ComponentQuery query = ComponentQuery.builder().setNameOrKeyQuery("oJect").setQualifiers(Qualifiers.PROJECT).build();
    List<ComponentDto> result = underTest.selectByQuery(dbSession, query, 1, 3);
    int count = underTest.countByQuery(dbSession, query);

    assertThat(result).hasSize(3);
    assertThat(count).isEqualTo(9);
    assertThat(result).extracting("name").containsExactly("project-2", "project-3", "project-4");
  }

  @Test
  public void select_by_query_name_with_special_characters() {
    componentDb.insertProjectAndSnapshot(newProjectDto().setName("project-\\_%/-name"));
    componentDb.indexAllComponents();

    ComponentQuery query = ComponentQuery.builder().setNameOrKeyQuery("-\\_%/-").setQualifiers(Qualifiers.PROJECT).build();
    List<ComponentDto> result = underTest.selectByQuery(dbSession, query, 0, 10);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).name()).isEqualTo("project-\\_%/-name");
  }

  @Test
  public void select_by_query_key_with_special_characters() {
    componentDb.insertProjectAndSnapshot(newProjectDto().setKey("project-_%-key"));
    componentDb.indexAllComponents();

    ComponentQuery query = ComponentQuery.builder().setNameOrKeyQuery("project-_%-key").setQualifiers(Qualifiers.PROJECT).build();
    List<ComponentDto> result = underTest.selectByQuery(dbSession, query, 0, 10);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).key()).isEqualTo("project-_%-key");
  }

  @Test
  public void select_by_query_filter_on_language() {
    componentDb.insertComponent(newProjectDto().setKey("java-project-key").setLanguage("java"));
    componentDb.insertComponent(newProjectDto().setKey("cpp-project-key").setLanguage("cpp"));

    ComponentQuery query = ComponentQuery.builder().setLanguage("java").setQualifiers(Qualifiers.PROJECT).build();
    List<ComponentDto> result = underTest.selectByQuery(dbSession, query, 0, 10);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).key()).isEqualTo("java-project-key");
  }

  @Test
  public void select_by_query_on_empty_list_of_component_id() {
    ComponentQuery dbQuery = ComponentQuery.builder().setQualifiers(Qualifiers.PROJECT).setComponentIds(emptySet()).build();
    List<ComponentDto> result = underTest.selectByQuery(dbSession, dbQuery, 0, 10);
    int count = underTest.countByQuery(dbSession, dbQuery);

    assertThat(result).isEmpty();
    assertThat(count).isEqualTo(0);
  }

  @Test
  public void select_by_query_on_component_ids() {
    ComponentDto sonarqube = componentDb.insertComponent(newProjectDto());
    ComponentDto jdk8 = componentDb.insertComponent(newProjectDto());
    ComponentDto cLang = componentDb.insertComponent(newProjectDto());

    ComponentQuery query = ComponentQuery.builder().setQualifiers(Qualifiers.PROJECT)
      .setComponentIds(newHashSet(sonarqube.getId(), jdk8.getId())).build();
    List<ComponentDto> result = underTest.selectByQuery(dbSession, query, 0, 10);

    assertThat(result).hasSize(2).extracting(ComponentDto::getId)
      .containsOnlyOnce(sonarqube.getId(), jdk8.getId())
      .doesNotContain(cLang.getId());
  }

  @Test
  public void selectChildren() {
    // project has 2 children: module and file 1. Other files are part of module.
    ComponentDto project = newProjectDto(PROJECT_UUID);
    componentDb.insertProjectAndSnapshot(project);
    ComponentDto module = newModuleDto(MODULE_UUID, project);
    componentDb.insertComponent(module);
    ComponentDto file1 = newFileDto(project, null, FILE_1_UUID).setKey("file-key-1").setName("File One");
    componentDb.insertComponent(file1);
    ComponentDto file2 = newFileDto(module, null, FILE_2_UUID).setKey("file-key-2").setName("File Two");
    componentDb.insertComponent(file2);
    ComponentDto file3 = newFileDto(module, null, FILE_3_UUID).setKey("file-key-3").setName("File Three");
    componentDb.insertComponent(file3);
    db.commit();
    componentDb.indexAllComponents();

    // test children of root
    ComponentTreeQuery query = newTreeQuery(PROJECT_UUID).build();
    List<ComponentDto> children = underTest.selectChildren(dbSession, query);
    assertThat(children).extracting("uuid").containsExactly(FILE_1_UUID, MODULE_UUID);
    assertThat(underTest.countChildren(dbSession, query)).isEqualTo(2);

    // test children of root, filtered by qualifier
    query = newTreeQuery(PROJECT_UUID).setQualifiers(asList(Qualifiers.MODULE)).build();
    children = underTest.selectChildren(dbSession, query);
    assertThat(children).extracting("uuid").containsExactly(MODULE_UUID);
    assertThat(underTest.countChildren(dbSession, query)).isEqualTo(1);

    // test children of intermediate component (module here), default ordering by
    query = newTreeQuery(MODULE_UUID).build();
    assertThat(underTest.selectChildren(dbSession, query)).extracting("uuid").containsOnly(FILE_2_UUID, FILE_3_UUID);
    assertThat(underTest.countChildren(dbSession, query)).isEqualTo(2);

    // test children of leaf component (file here)
    query = newTreeQuery(FILE_1_UUID).build();
    assertThat(underTest.selectChildren(dbSession, query)).isEmpty();
    assertThat(underTest.countChildren(dbSession, query)).isEqualTo(0);

    // test children of root, matching name
    query = newTreeQuery(PROJECT_UUID).setNameOrKeyQuery("One").build();
    assertThat(underTest.selectChildren(dbSession, query)).extracting("uuid").containsOnly(FILE_1_UUID);
    assertThat(underTest.countChildren(dbSession, query)).isEqualTo(1);

    // test children of root, matching case-insensitive name
    query = newTreeQuery(PROJECT_UUID).setNameOrKeyQuery("OnE").build();
    assertThat(underTest.selectChildren(dbSession, query)).extracting("uuid").containsOnly(FILE_1_UUID);
    assertThat(underTest.countChildren(dbSession, query)).isEqualTo(1);

    // test children of root, matching key
    query = newTreeQuery(PROJECT_UUID).setNameOrKeyQuery("file-key-1").build();
    assertThat(underTest.selectChildren(dbSession, query)).extracting("uuid").containsOnly(FILE_1_UUID);
    assertThat(underTest.countChildren(dbSession, query)).isEqualTo(1);

    // test children of root, without matching name nor key
    query = newTreeQuery(PROJECT_UUID).setNameOrKeyQuery("does-not-exist").build();
    assertThat(underTest.selectChildren(dbSession, query)).isEmpty();
    assertThat(underTest.countChildren(dbSession, query)).isEqualTo(0);

    // test children of intermediate component (module here), matching name
    query = newTreeQuery(MODULE_UUID).setNameOrKeyQuery("Two").build();
    assertThat(underTest.selectChildren(dbSession, query)).extracting("uuid").containsOnly(FILE_2_UUID);
    assertThat(underTest.countChildren(dbSession, query)).isEqualTo(1);

    // test children of intermediate component (module here), without matching name
    query = newTreeQuery(MODULE_UUID).setNameOrKeyQuery("does-not-exist").build();
    assertThat(underTest.selectChildren(dbSession, query)).isEmpty();
    assertThat(underTest.countChildren(dbSession, query)).isEqualTo(0);

    // test children of leaf component (file here)
    query = newTreeQuery(FILE_1_UUID).build();
    assertThat(underTest.selectChildren(dbSession, query)).isEmpty();
    assertThat(underTest.countChildren(dbSession, query)).isEqualTo(0);

    // test children of leaf component (file here), matching name
    query = newTreeQuery(FILE_1_UUID).setNameOrKeyQuery("Foo").build();
    assertThat(underTest.selectChildren(dbSession, query)).isEmpty();
    assertThat(underTest.countChildren(dbSession, query)).isEqualTo(0);
  }

  @Test
  public void selectChildren_with_pagination() {
    ComponentDto project = newProjectDto(PROJECT_UUID);
    componentDb.insertProjectAndSnapshot(project);
    for (int i = 1; i <= 9; i++) {
      componentDb.insertComponent(newFileDto(project, null, "file-uuid-" + i));
    }
    db.commit();

    ComponentTreeQuery query = newTreeQuery(PROJECT_UUID)
      .setPage(2)
      .setPageSize(3)
      .setAsc(false)
      .build();

    assertThat(underTest.selectChildren(dbSession, query)).extracting("uuid").containsExactly("file-uuid-6", "file-uuid-5", "file-uuid-4");
    assertThat(underTest.countChildren(dbSession, query)).isEqualTo(9);
  }

  @Test
  public void selectChildren_ordered_by_file_path() {
    ComponentDto project = newProjectDto(PROJECT_UUID);
    componentDb.insertProjectAndSnapshot(project);
    componentDb.insertComponent(newFileDto(project, null, "file-uuid-1").setName("file-name-1").setPath("3"));
    componentDb.insertComponent(newFileDto(project, null, "file-uuid-2").setName("file-name-2").setPath("2"));
    componentDb.insertComponent(newFileDto(project, null, "file-uuid-3").setName("file-name-3").setPath("1"));
    db.commit();
    componentDb.indexAllComponents();

    ComponentTreeQuery query = newTreeQuery(PROJECT_UUID)
      .setSortFields(singletonList("path"))
      .setAsc(true)
      .build();

    List<ComponentDto> result = underTest.selectChildren(dbSession, query);
    assertThat(result).extracting("uuid").containsExactly("file-uuid-3", "file-uuid-2", "file-uuid-1");
  }

  @Test
  public void selectChildren_returns_empty_list_if_base_component_does_not_exist() {
    ComponentTreeQuery query = newTreeQuery(PROJECT_UUID).build();

    List<ComponentDto> result = underTest.selectChildren(dbSession, query);
    assertThat(result).isEmpty();
  }

  @Test
  public void selectChildren_of_a_view() {
    ComponentDto view = newView(A_VIEW_UUID);
    componentDb.insertViewAndSnapshot(view);
    // one subview
    ComponentDto subView = newSubView(view, "subview-uuid", "subview-key").setName("subview-name");
    componentDb.insertComponent(subView);
    // one project and its copy linked to the view
    ComponentDto project = newProjectDto(PROJECT_UUID).setName("project-name");
    componentDb.insertProjectAndSnapshot(project);
    componentDb.insertComponent(newProjectCopy("project-copy-uuid", project, view));
    componentDb.indexAllComponents();
    ComponentTreeQuery query = newTreeQuery(A_VIEW_UUID).build();

    List<ComponentDto> components = underTest.selectChildren(dbSession, query);
    assertThat(components).extracting("uuid").containsOnly("project-copy-uuid", "subview-uuid");
  }

  @Test
  public void selectChildren_of_a_view_and_filter_by_name() {
    ComponentDto view = newView(A_VIEW_UUID);
    componentDb.insertViewAndSnapshot(view);
    // one subview
    ComponentDto subView = newSubView(view, "subview-uuid", "subview-key").setName("subview name");
    componentDb.insertComponent(subView);
    // one project and its copy linked to the view
    ComponentDto project = newProjectDto(PROJECT_UUID).setName("project name");
    componentDb.insertProjectAndSnapshot(project);
    componentDb.insertComponent(newProjectCopy("project-copy-uuid", project, view));
    componentDb.indexAllComponents();
    ComponentTreeQuery dbQuery = newTreeQuery(A_VIEW_UUID).setNameOrKeyQuery("name").build();

    List<ComponentDto> components = underTest.selectChildren(dbSession, dbQuery);
    assertThat(components).extracting("uuid").containsOnly("project-copy-uuid", "subview-uuid");
  }

  @Test
  public void selectParent() {
    // project -> module -> file
    ComponentDto project = newProjectDto(PROJECT_UUID);
    componentDb.insertProjectAndSnapshot(project);
    ComponentDto module = newModuleDto(MODULE_UUID, project);
    componentDb.insertComponent(module);
    ComponentDto file = newFileDto(module, null, FILE_1_UUID);
    componentDb.insertComponent(file);
    db.commit();

    assertThat(underTest.selectParent(dbSession, project)).isAbsent();
    assertThat(underTest.selectParent(dbSession, module).get().uuid()).isEqualTo(PROJECT_UUID);
    assertThat(underTest.selectParent(dbSession, file).get().uuid()).isEqualTo(MODULE_UUID);
  }

  @Test
  public void selectAncestors() {
    // project -> module -> file
    ComponentDto project = newProjectDto(PROJECT_UUID);
    componentDb.insertProjectAndSnapshot(project);
    ComponentDto module = newModuleDto(MODULE_UUID, project);
    componentDb.insertComponent(module);
    ComponentDto file = newFileDto(module, null, FILE_1_UUID);
    componentDb.insertComponent(file);
    db.commit();

    // ancestors of root
    List<ComponentDto> ancestors = underTest.selectAncestors(dbSession, project);
    assertThat(ancestors).isEmpty();

    // ancestors of module
    ancestors = underTest.selectAncestors(dbSession, module);
    assertThat(ancestors).extracting("uuid").containsExactly(PROJECT_UUID);

    // ancestors of file
    ancestors = underTest.selectAncestors(dbSession, file);
    assertThat(ancestors).extracting("uuid").containsExactly(PROJECT_UUID, MODULE_UUID);
  }

  @Test
  public void selectDescendants() {
    ComponentDto project = newProjectDto(PROJECT_UUID);
    componentDb.insertProjectAndSnapshot(project);
    componentDb.insertComponent(newModuleDto("module-1-uuid", project));
    componentDb.insertComponent(newFileDto(project, null, "file-1-uuid"));
    componentDb.insertComponent(newFileDto(project, null, "file-2-uuid"));
    db.commit();
    componentDb.indexAllComponents();

    ComponentTreeQuery query = newTreeQuery(PROJECT_UUID).build();

    List<ComponentDto> result = underTest.selectDescendants(dbSession, query);
    assertThat(result).extracting("uuid").containsExactly("file-1-uuid", "file-2-uuid", "module-1-uuid");
    int count = underTest.countDescendants(dbSession, query);
    assertThat(count).isEqualTo(3);
  }

  @Test
  public void selectDescendants_returns_empty_list_if_base_component_does_not_exist() {
    ComponentTreeQuery query = newTreeQuery(PROJECT_UUID).build();

    List<ComponentDto> result = underTest.selectDescendants(dbSession, query);
    assertThat(result).isEmpty();
  }

  @Test
  public void selectDescendants_of_a_project_paginated_and_ordered() {
    ComponentDto project = newProjectDto(PROJECT_UUID).setKey("project-key");
    componentDb.insertProjectAndSnapshot(project);
    componentDb.insertComponent(newModuleDto("module-1-uuid", project));
    componentDb.insertComponent(newFileDto(project, null, "file-uuid-1").setName("file-name-1"));
    componentDb.insertComponent(newFileDto(project, null, "another-uuid"));
    for (int i = 2; i <= 9; i++) {
      componentDb.insertComponent(newFileDto(project, null, "file-uuid-" + i).setName("file-name-" + i));
    }
    db.commit();
    componentDb.indexAllComponents();

    ComponentTreeQuery query = newTreeQuery(PROJECT_UUID)
      .setQualifiers(newArrayList(Qualifiers.FILE))
      .setPage(2)
      .setPageSize(3)
      .setNameOrKeyQuery("file-name")
      .setSortFields(singletonList("name"))
      .setAsc(false)
      .build();

    List<ComponentDto> result = underTest.selectDescendants(dbSession, query);
    int count = underTest.countDescendants(dbSession, query);

    assertThat(count).isEqualTo(9);
    assertThat(result).extracting("uuid").containsExactly("file-uuid-6", "file-uuid-5", "file-uuid-4");
  }

  private static ComponentTreeQuery.Builder newTreeQuery(String baseUuid) {
    return ComponentTreeQuery.builder()
      .setPage(1)
      .setPageSize(500)
      .setBaseUuid(baseUuid)
      .setSortFields(singletonList("name"))
      .setAsc(true);
  }
}
