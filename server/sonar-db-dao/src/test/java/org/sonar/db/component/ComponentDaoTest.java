/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

import com.google.common.base.Optional;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.apache.ibatis.session.RowBounds;
import org.assertj.core.api.ListAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.RowNotFoundException;
import org.sonar.db.organization.OrganizationDto;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.guava.api.Assertions.assertThat;
import static org.sonar.db.component.ComponentTesting.newDirectory;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newModuleDto;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;
import static org.sonar.db.component.ComponentTesting.newProjectCopy;
import static org.sonar.db.component.ComponentTesting.newSubView;
import static org.sonar.db.component.ComponentTesting.newView;
import static org.sonar.db.component.ComponentTreeQuery.Strategy.CHILDREN;
import static org.sonar.db.component.ComponentTreeQuery.Strategy.LEAVES;

public class ComponentDaoTest {

  private static final String PROJECT_UUID = "project-uuid";
  private static final String MODULE_UUID = "module-uuid";
  private static final String FILE_1_UUID = "file-1-uuid";
  private static final String FILE_2_UUID = "file-2-uuid";
  private static final String FILE_3_UUID = "file-3-uuid";
  private static final String A_VIEW_UUID = "view-uuid";
  private static final ComponentQuery ALL_PROJECTS_COMPONENT_QUERY = ComponentQuery.builder().setQualifiers("TRK").build();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private DbSession dbSession = db.getSession();
  private ComponentDao underTest = new ComponentDao();

  private static ComponentTreeQuery.Builder newTreeQuery(String baseUuid) {
    return ComponentTreeQuery.builder()
      .setBaseUuid(baseUuid)
      .setStrategy(CHILDREN);
  }

  @Test
  public void get_by_uuid() {
    db.prepareDbUnit(getClass(), "shared.xml");

    ComponentDto result = underTest.selectByUuid(dbSession, "U1").get();
    assertThat(result).isNotNull();
    assertThat(result.getOrganizationUuid()).isEqualTo("org1");
    assertThat(result.uuid()).isEqualTo("U1");
    assertThat(result.getUuidPath()).isEqualTo("uuid_path_of_U1");
    assertThat(result.moduleUuid()).isEqualTo("module_uuid_of_U1");
    assertThat(result.moduleUuidPath()).isEqualTo("module_uuid_path_of_U1");
    assertThat(result.getRootUuid()).isEqualTo("U1");
    assertThat(result.projectUuid()).isEqualTo("U1");
    assertThat(result.key()).isEqualTo("org.struts:struts");
    assertThat(result.path()).isEqualTo("path_of_U1");
    assertThat(result.name()).isEqualTo("Struts");
    assertThat(result.longName()).isEqualTo("Apache Struts");
    assertThat(result.qualifier()).isEqualTo("TRK");
    assertThat(result.scope()).isEqualTo("PRJ");
    assertThat(result.language()).isEqualTo("java");
    assertThat(result.getCopyResourceUuid()).isNull();
    assertThat(result.getDeveloperUuid()).isNull();
    assertThat(result.isPrivate()).isTrue();

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
    assertThat(result.isPrivate()).isFalse();
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
    expectedException.expect(RowNotFoundException.class);

    db.prepareDbUnit(getClass(), "shared.xml");

    underTest.selectOrFailByUuid(dbSession, "unknown");
  }

  @Test
  public void selectByKey() {
    db.prepareDbUnit(getClass(), "shared.xml");

    Optional<ComponentDto> optional = underTest.selectByKey(dbSession, "org.struts:struts-core:src/org/struts/RequestContext.java");
    assertThat(optional).isPresent();

    ComponentDto result = optional.get();
    assertThat(result.getOrganizationUuid()).isEqualTo("org1");
    assertThat(result.uuid()).isEqualTo("U4");
    assertThat(result.key()).isEqualTo("org.struts:struts-core:src/org/struts/RequestContext.java");
    assertThat(result.path()).isEqualTo("path_of_U4");
    assertThat(result.name()).isEqualTo("RequestContext.java");
    assertThat(result.longName()).isEqualTo("org.struts.RequestContext");
    assertThat(result.qualifier()).isEqualTo("FIL");
    assertThat(result.scope()).isEqualTo("FIL");
    assertThat(result.language()).isEqualTo("java");
    assertThat(result.getRootUuid()).isEqualTo("U1");

    assertThat(underTest.selectByKey(dbSession, "unknown")).isAbsent();
  }

  @Test
  public void selectOrFailByKey_fails_when_component_not_found() {
    expectedException.expect(RowNotFoundException.class);

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
    assertThat(result.getRootUuid()).isEqualTo("U1");
    assertThat(result.projectUuid()).isEqualTo("U1");
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
    assertThat(result.getOrganizationUuid()).isEqualTo("org1");
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
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Qualifiers cannot be empty");

    db.prepareDbUnit(getClass(), "shared.xml");
    underTest.selectComponentsByQualifiers(dbSession, Collections.emptySet());
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

    assertThat(underTest.selectSubProjectsByComponentUuids(dbSession, Collections.emptyList())).isEmpty();
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
    OrganizationDto organization = db.organizations().insert();
    ComponentDto provisionedProject = db.components().insertPrivateProject();
    ComponentDto provisionedView = db.components().insertView(organization, (dto) -> {
    });
    String projectUuid = db.components().insertProjectAndSnapshot(ComponentTesting.newPrivateProjectDto(organization)).getComponentUuid();
    String disabledProjectUuid = db.components().insertProjectAndSnapshot(ComponentTesting.newPrivateProjectDto(organization).setEnabled(false)).getComponentUuid();
    String viewUuid = db.components().insertProjectAndSnapshot(ComponentTesting.newView(organization)).getComponentUuid();

    assertThat(underTest.selectProjects(dbSession))
      .extracting(ComponentDto::uuid)
      .containsOnly(provisionedProject.uuid(), projectUuid);
  }

  @Test
  public void select_provisioned() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto provisionedProject = db.components().insertComponent(ComponentTesting.newPrivateProjectDto(organization).setKey("provisioned.project").setName("Provisioned Project"));
    ComponentDto provisionedView = db.components().insertView(organization);
    String projectUuid = db.components().insertProjectAndSnapshot(ComponentTesting.newPrivateProjectDto(organization)).getComponentUuid();
    String disabledProjectUuid = db.components().insertProjectAndSnapshot(ComponentTesting.newPrivateProjectDto(organization).setEnabled(false)).getComponentUuid();
    String viewUuid = db.components().insertProjectAndSnapshot(ComponentTesting.newView(organization)).getComponentUuid();

    Set<String> projectQualifiers = newHashSet(Qualifiers.PROJECT);
    assertThat(underTest.selectProvisioned(dbSession, organization.getUuid(), null, projectQualifiers, new RowBounds(0, 10)))
      .extracting(ComponentDto::uuid)
      .containsOnly(provisionedProject.uuid());

    // pagination
    assertThat(underTest.selectProvisioned(dbSession, organization.getUuid(), null, projectQualifiers, new RowBounds(2, 10)))
      .isEmpty();

    // filter on qualifiers
    assertThat(underTest.selectProvisioned(dbSession, organization.getUuid(), null, newHashSet("XXX"), new RowBounds(0, 10)))
      .isEmpty();
    assertThat(underTest.selectProvisioned(dbSession, organization.getUuid(), null, newHashSet(Qualifiers.PROJECT, "XXX"), new RowBounds(0, 10)))
      .extracting(ComponentDto::uuid)
      .containsOnly(provisionedProject.uuid());
    assertThat(underTest.selectProvisioned(dbSession, organization.getUuid(), null, newHashSet(Qualifiers.PROJECT, Qualifiers.VIEW), new RowBounds(0, 10)))
      .extracting(ComponentDto::uuid)
      .containsOnly(provisionedProject.uuid(), provisionedView.uuid());

    // match key
    assertThat(underTest.selectProvisioned(dbSession, organization.getUuid(), provisionedProject.getKey(), projectQualifiers, new RowBounds(0, 10)))
      .extracting(ComponentDto::uuid)
      .containsExactly(provisionedProject.uuid());
    assertThat(underTest.selectProvisioned(dbSession, organization.getUuid(), "pROvisiONed.proJEcT", projectQualifiers, new RowBounds(0, 10)))
      .extracting(ComponentDto::uuid)
      .containsExactly(provisionedProject.uuid());
    assertThat(underTest.selectProvisioned(dbSession, organization.getUuid(), "missing", projectQualifiers, new RowBounds(0, 10)))
      .isEmpty();
    assertThat(underTest.selectProvisioned(dbSession, organization.getUuid(), "to be escaped '\"\\%", projectQualifiers, new RowBounds(0, 10)))
      .isEmpty();

    // match name
    assertThat(underTest.selectProvisioned(dbSession, organization.getUuid(), "ned proj", projectQualifiers, new RowBounds(0, 10)))
      .extracting(ComponentDto::uuid)
      .containsExactly(provisionedProject.uuid());
  }

  @Test
  public void count_provisioned() {
    OrganizationDto organization = db.organizations().insert();
    db.components().insertPrivateProject(organization);
    db.components().insertProjectAndSnapshot(ComponentTesting.newPrivateProjectDto(organization));
    db.components().insertProjectAndSnapshot(ComponentTesting.newView(organization));

    assertThat(underTest.countProvisioned(dbSession, organization.getUuid(), null, newHashSet(Qualifiers.PROJECT))).isEqualTo(1);
    assertThat(underTest.countProvisioned(dbSession, organization.getUuid(), null, newHashSet(Qualifiers.VIEW))).isEqualTo(0);
    assertThat(underTest.countProvisioned(dbSession, organization.getUuid(), null, newHashSet(Qualifiers.VIEW, Qualifiers.PROJECT))).isEqualTo(1);
  }

  @Test
  public void select_ghost_projects() {
    OrganizationDto organization = db.organizations().insert();

    // ghosts because has at least one snapshot with status U but none with status P
    ComponentDto ghostProject = db.components().insertPrivateProject(organization);
    db.components().insertSnapshot(ghostProject, dto -> dto.setStatus("U"));
    db.components().insertSnapshot(ghostProject, dto -> dto.setStatus("U"));
    ComponentDto ghostProject2 = db.components().insertPrivateProject(organization);
    db.components().insertSnapshot(ghostProject2, dto -> dto.setStatus("U"));
    ComponentDto disabledGhostProject = db.components().insertPrivateProject(dto -> dto.setEnabled(false));
    db.components().insertSnapshot(disabledGhostProject, dto -> dto.setStatus("U"));

    ComponentDto project1 = db.components().insertPrivateProject(organization);
    db.components().insertSnapshot(project1, dto -> dto.setStatus("P"));
    db.components().insertSnapshot(project1, dto -> dto.setStatus("U"));
    ComponentDto module = db.components().insertComponent(newModuleDto(project1));
    ComponentDto dir = db.components().insertComponent(newDirectory(module, "foo"));
    db.components().insertComponent(newFileDto(module, dir, "bar"));

    ComponentDto provisionedProject = db.components().insertPrivateProject(organization);

    // not a ghost because has at least one snapshot with status P
    ComponentDto project2 = db.components().insertPrivateProject(organization);
    db.components().insertSnapshot(project2, dto -> dto.setStatus("P"));

    // not a ghost because it's not a project
    ComponentDto view = db.components().insertView(organization);
    db.components().insertSnapshot(view, dto -> dto.setStatus("U"));
    db.components().insertComponent(newProjectCopy("do", project1, view));

    assertThat(underTest.selectGhostProjects(dbSession, organization.getUuid(), null, 0, 10))
      .extracting(ComponentDto::uuid)
      .containsOnly(ghostProject.uuid(), ghostProject2.uuid(), disabledGhostProject.uuid());
    assertThat(underTest.countGhostProjects(dbSession, organization.getUuid(), null)).isEqualTo(3);
  }

  @Test
  public void selectByProjectUuid() {
    db.prepareDbUnit(getClass(), "shared.xml");

    List<ComponentDto> components = underTest.selectByProjectUuid("U1", dbSession);

    assertThat(components).extracting("id").containsOnly(1L, 2L, 3L, 4L);
  }

  @Test
  public void selectForIndexing_all() {
    assertSelectForIndexing(null)
      .doesNotContain("DIS7")
      .doesNotContain("COPY8")
      .containsOnly("U1", "U2", "U3", "U4", "U5", "U6");
  }

  @Test
  public void selectForIndexing_project() {
    assertSelectForIndexing("U1")
      .doesNotContain("DIS7")
      .doesNotContain("COPY8")
      .containsOnly("U1", "U2", "U3", "U4");
  }

  private ListAssert<String> assertSelectForIndexing(@Nullable String projectUuid) {
    db.prepareDbUnit(getClass(), "selectForIndexing.xml");

    List<ComponentDto> components = new ArrayList<>();
    underTest.scrollForIndexing(dbSession, projectUuid, context -> components.add(context.getResultObject()));
    return assertThat(components).extracting(ComponentDto::uuid);
  }

  @Test
  public void insert() {
    db.prepareDbUnit(getClass(), "empty.xml");

    ComponentDto componentDto = new ComponentDto()
      .setOrganizationUuid("org1")
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
      .setAuthorizationUpdatedAt(123456789L)
      .setTags(newArrayList("platform", "analyzers"));

    underTest.insert(dbSession, componentDto);
    dbSession.commit();

    assertThat(componentDto.getId()).isNotNull();
    db.assertDbUnit(getClass(), "insert-result.xml", "projects");
  }

  @Test
  public void insert_disabled_component() {
    db.prepareDbUnit(getClass(), "empty.xml");

    ComponentDto componentDto = new ComponentDto()
      .setOrganizationUuid("org1")
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
    db.components().insertPrivateProject(db.getDefaultOrganization(), "U1");

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
      .setBQualifier("qualifier"));
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
    ComponentDto dto1 = newPrivateProjectDto(db.getDefaultOrganization(), "U1");
    ComponentDto dto2 = newPrivateProjectDto(db.getDefaultOrganization(), "U2");
    ComponentDto dto3 = newPrivateProjectDto(db.getDefaultOrganization(), "U3");
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
  public void update_tags() {
    ComponentDto project = db.components().insertPrivateProject(p -> p.setTags(emptyList()));

    underTest.updateTags(dbSession, project.setTags(newArrayList("finance", "toto", "tutu")));
    dbSession.commit();

    assertThat(underTest.selectOrFailByKey(dbSession, project.key()).getTags()).containsOnly("finance", "toto", "tutu");
  }

  @Test
  public void delete() throws Exception {
    ComponentDto project1 = db.components().insertPrivateProject(db.getDefaultOrganization(), (t) -> t.setKey("PROJECT_1"));
    db.components().insertPrivateProject(db.getDefaultOrganization(), (t) -> t.setKey("PROJECT_2"));

    underTest.delete(dbSession, project1.getId());
    dbSession.commit();

    assertThat(underTest.selectByKey(dbSession, "PROJECT_1")).isAbsent();
    assertThat(underTest.selectByKey(dbSession, "PROJECT_2")).isPresent();
  }

  @Test
  public void selectByQuery_with_organization_throws_NPE_of_organizationUuid_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("organizationUuid can't be null");

    underTest.selectByQuery(dbSession, null, ALL_PROJECTS_COMPONENT_QUERY, 1, 1);
  }

  @Test
  public void countByQuery_with_organization_throws_NPE_of_organizationUuid_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("organizationUuid can't be null");

    underTest.countByQuery(dbSession, null, ALL_PROJECTS_COMPONENT_QUERY);
  }

  @Test
  public void selectByQuery_with_paging_query_and_qualifiers() {
    OrganizationDto organizationDto = db.organizations().insert();
    db.components().insertProjectAndSnapshot(ComponentTesting.newPrivateProjectDto(organizationDto).setName("aaaa-name"));
    db.components().insertProjectAndSnapshot(newView(organizationDto));
    for (int i = 9; i >= 1; i--) {
      db.components().insertProjectAndSnapshot(ComponentTesting.newPrivateProjectDto(organizationDto).setName("project-" + i));
    }

    ComponentQuery query = ComponentQuery.builder().setNameOrKeyQuery("oJect").setQualifiers(Qualifiers.PROJECT).build();
    List<ComponentDto> result = underTest.selectByQuery(dbSession, query, 1, 3);
    int count = underTest.countByQuery(dbSession, query);

    assertThat(result).hasSize(3);
    assertThat(count).isEqualTo(9);
    assertThat(result).extracting(ComponentDto::name).containsExactly("project-2", "project-3", "project-4");
    assertThat(result).extracting(ComponentDto::getOrganizationUuid).containsOnly(organizationDto.getUuid());
  }

  @Test
  public void selectByQuery_with_organization_filters_on_specified_organization() {
    OrganizationDto organization1 = db.organizations().insert();
    OrganizationDto organization2 = db.organizations().insert();
    ComponentDto project1 = db.components().insertPrivateProject(organization1);
    ComponentDto project2 = db.components().insertPrivateProject(organization2);

    assertThat(underTest.selectByQuery(dbSession, ALL_PROJECTS_COMPONENT_QUERY, 0, 2))
      .extracting(ComponentDto::uuid)
      .containsOnly(project1.uuid(), project2.uuid());
    assertThat(underTest.selectByQuery(dbSession, organization1.getUuid(), ALL_PROJECTS_COMPONENT_QUERY, 0, 2))
      .extracting(ComponentDto::uuid)
      .containsOnly(project1.uuid());
    assertThat(underTest.selectByQuery(dbSession, organization2.getUuid(), ALL_PROJECTS_COMPONENT_QUERY, 0, 2))
      .extracting(ComponentDto::uuid)
      .containsOnly(project2.uuid());
    assertThat(underTest.selectByQuery(dbSession, "non existent organization uuid", ALL_PROJECTS_COMPONENT_QUERY, 0, 2))
      .isEmpty();
  }

  @Test
  public void countByQuery_with_organization_filters_on_specified_organization() {
    OrganizationDto organization1 = db.organizations().insert();
    OrganizationDto organization2 = db.organizations().insert();
    ComponentDto project1 = db.components().insertPrivateProject(organization1);
    ComponentDto project2 = db.components().insertPrivateProject(organization2);

    assertThat(underTest.countByQuery(dbSession, ALL_PROJECTS_COMPONENT_QUERY))
      .isEqualTo(2);
    assertThat(underTest.countByQuery(dbSession, organization1.getUuid(), ALL_PROJECTS_COMPONENT_QUERY))
      .isEqualTo(1);
    assertThat(underTest.countByQuery(dbSession, organization2.getUuid(), ALL_PROJECTS_COMPONENT_QUERY))
      .isEqualTo(1);
    assertThat(underTest.countByQuery(dbSession, "non existent organization uuid", ALL_PROJECTS_COMPONENT_QUERY))
      .isEqualTo(0);
  }

  @Test
  public void selectByQuery_name_with_special_characters() {
    db.components().insertProjectAndSnapshot(ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization()).setName("project-\\_%/-name"));

    ComponentQuery query = ComponentQuery.builder().setNameOrKeyQuery("-\\_%/-").setQualifiers(Qualifiers.PROJECT).build();
    List<ComponentDto> result = underTest.selectByQuery(dbSession, query, 0, 10);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).name()).isEqualTo("project-\\_%/-name");
  }

  @Test
  public void selectByQuery_key_with_special_characters() {
    db.components().insertProjectAndSnapshot(ComponentTesting.newPrivateProjectDto(db.organizations().insert()).setKey("project-_%-key"));

    ComponentQuery query = ComponentQuery.builder().setNameOrKeyQuery("project-_%-key").setQualifiers(Qualifiers.PROJECT).build();
    List<ComponentDto> result = underTest.selectByQuery(dbSession, query, 0, 10);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).key()).isEqualTo("project-_%-key");
  }

  @Test
  public void selectByQuery_filter_on_language() {
    db.components().insertComponent(ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization()).setKey("java-project-key").setLanguage("java"));
    db.components().insertComponent(ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization()).setKey("cpp-project-key").setLanguage("cpp"));

    ComponentQuery query = ComponentQuery.builder().setLanguage("java").setQualifiers(Qualifiers.PROJECT).build();
    List<ComponentDto> result = underTest.selectByQuery(dbSession, query, 0, 10);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).key()).isEqualTo("java-project-key");
  }

  @Test
  public void selectByQuery_filter_on_visibility() {
    db.components().insertComponent(ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization()).setKey("private-key"));
    db.components().insertComponent(ComponentTesting.newPublicProjectDto(db.getDefaultOrganization()).setKey("public-key"));

    ComponentQuery privateProjectsQuery = ComponentQuery.builder().setPrivate(true).setQualifiers(Qualifiers.PROJECT).build();
    ComponentQuery publicProjectsQuery = ComponentQuery.builder().setPrivate(false).setQualifiers(Qualifiers.PROJECT).build();
    ComponentQuery allProjectsQuery = ComponentQuery.builder().setPrivate(null).setQualifiers(Qualifiers.PROJECT).build();

    assertThat(underTest.selectByQuery(dbSession, privateProjectsQuery, 0, 10)).extracting(ComponentDto::getKey).containsExactly("private-key");
    assertThat(underTest.selectByQuery(dbSession, publicProjectsQuery, 0, 10)).extracting(ComponentDto::getKey).containsExactly("public-key");
    assertThat(underTest.selectByQuery(dbSession, allProjectsQuery, 0, 10)).extracting(ComponentDto::getKey).containsOnly("public-key", "private-key");
  }

  @Test
  public void selectByQuery_on_empty_list_of_component_id() {
    ComponentQuery dbQuery = ComponentQuery.builder().setQualifiers(Qualifiers.PROJECT).setComponentIds(emptySet()).build();
    List<ComponentDto> result = underTest.selectByQuery(dbSession, dbQuery, 0, 10);
    int count = underTest.countByQuery(dbSession, dbQuery);

    assertThat(result).isEmpty();
    assertThat(count).isEqualTo(0);
  }

  @Test
  public void selectByQuery_on_component_ids() {
    OrganizationDto organizationDto = db.organizations().insert();
    ComponentDto sonarqube = db.components().insertComponent(ComponentTesting.newPrivateProjectDto(organizationDto));
    ComponentDto jdk8 = db.components().insertComponent(ComponentTesting.newPrivateProjectDto(organizationDto));
    ComponentDto cLang = db.components().insertComponent(ComponentTesting.newPrivateProjectDto(organizationDto));

    ComponentQuery query = ComponentQuery.builder().setQualifiers(Qualifiers.PROJECT)
      .setComponentIds(newHashSet(sonarqube.getId(), jdk8.getId())).build();
    List<ComponentDto> result = underTest.selectByQuery(dbSession, query, 0, 10);

    assertThat(result).hasSize(2).extracting(ComponentDto::getId)
      .containsOnlyOnce(sonarqube.getId(), jdk8.getId())
      .doesNotContain(cLang.getId());
  }

  @Test
  public void selectAncestors() {
    // organization
    OrganizationDto organization = db.organizations().insert();
    // project -> module -> file
    ComponentDto project = newPrivateProjectDto(organization, PROJECT_UUID);
    db.components().insertProjectAndSnapshot(project);
    ComponentDto module = newModuleDto(MODULE_UUID, project);
    db.components().insertComponent(module);
    ComponentDto file = newFileDto(module, null, FILE_1_UUID);
    db.components().insertComponent(file);
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
  public void select_descendants_with_children_stragegy() {
    // project has 2 children: module and file 1. Other files are part of module.
    ComponentDto project = newPrivateProjectDto(db.organizations().insert(), PROJECT_UUID);
    db.components().insertProjectAndSnapshot(project);
    ComponentDto module = newModuleDto(MODULE_UUID, project);
    db.components().insertComponent(module);
    ComponentDto file1 = newFileDto(project, null, FILE_1_UUID).setKey("file-key-1").setName("File One");
    db.components().insertComponent(file1);
    ComponentDto file2 = newFileDto(module, null, FILE_2_UUID).setKey("file-key-2").setName("File Two");
    db.components().insertComponent(file2);
    ComponentDto file3 = newFileDto(module, null, FILE_3_UUID).setKey("file-key-3").setName("File Three");
    db.components().insertComponent(file3);
    db.commit();

    // test children of root
    ComponentTreeQuery query = newTreeQuery(PROJECT_UUID).build();
    List<ComponentDto> children = underTest.selectDescendants(dbSession, query);
    assertThat(children).extracting("uuid").containsOnly(FILE_1_UUID, MODULE_UUID);

    // test children of root, filtered by qualifier
    query = newTreeQuery(PROJECT_UUID).setQualifiers(asList(Qualifiers.MODULE)).build();
    children = underTest.selectDescendants(dbSession, query);
    assertThat(children).extracting("uuid").containsOnly(MODULE_UUID);

    // test children of intermediate component (module here), default ordering by
    query = newTreeQuery(MODULE_UUID).build();
    assertThat(underTest.selectDescendants(dbSession, query)).extracting("uuid").containsOnly(FILE_2_UUID, FILE_3_UUID);

    // test children of leaf component (file here)
    query = newTreeQuery(FILE_1_UUID).build();
    assertThat(underTest.selectDescendants(dbSession, query)).isEmpty();

    // test children of root, matching name
    query = newTreeQuery(PROJECT_UUID).setNameOrKeyQuery("One").build();
    assertThat(underTest.selectDescendants(dbSession, query)).extracting("uuid").containsOnly(FILE_1_UUID);

    // test children of root, matching case-insensitive name
    query = newTreeQuery(PROJECT_UUID).setNameOrKeyQuery("OnE").build();
    assertThat(underTest.selectDescendants(dbSession, query)).extracting("uuid").containsOnly(FILE_1_UUID);

    // test children of root, matching key
    query = newTreeQuery(PROJECT_UUID).setNameOrKeyQuery("file-key-1").build();
    assertThat(underTest.selectDescendants(dbSession, query)).extracting("uuid").containsOnly(FILE_1_UUID);

    // test children of root, without matching name nor key
    query = newTreeQuery(PROJECT_UUID).setNameOrKeyQuery("does-not-exist").build();
    assertThat(underTest.selectDescendants(dbSession, query)).isEmpty();

    // test children of intermediate component (module here), matching name
    query = newTreeQuery(MODULE_UUID).setNameOrKeyQuery("Two").build();
    assertThat(underTest.selectDescendants(dbSession, query)).extracting("uuid").containsOnly(FILE_2_UUID);

    // test children of intermediate component (module here), without matching name
    query = newTreeQuery(MODULE_UUID).setNameOrKeyQuery("does-not-exist").build();
    assertThat(underTest.selectDescendants(dbSession, query)).isEmpty();

    // test children of leaf component (file here)
    query = newTreeQuery(FILE_1_UUID).build();
    assertThat(underTest.selectDescendants(dbSession, query)).isEmpty();

    // test children of leaf component (file here), matching name
    query = newTreeQuery(FILE_1_UUID).setNameOrKeyQuery("Foo").build();
    assertThat(underTest.selectDescendants(dbSession, query)).isEmpty();
  }

  @Test
  public void select_descendants_with_leaves_strategy() {
    ComponentDto project = newPrivateProjectDto(db.getDefaultOrganization(), PROJECT_UUID);
    db.components().insertProjectAndSnapshot(project);
    db.components().insertComponent(newModuleDto("module-1-uuid", project));
    db.components().insertComponent(newFileDto(project, null, "file-1-uuid"));
    db.components().insertComponent(newFileDto(project, null, "file-2-uuid"));
    db.commit();

    ComponentTreeQuery query = newTreeQuery(PROJECT_UUID).setStrategy(LEAVES).build();

    List<ComponentDto> result = underTest.selectDescendants(dbSession, query);
    assertThat(result).extracting("uuid").containsOnly("file-1-uuid", "file-2-uuid", "module-1-uuid");
  }

  @Test
  public void select_descendants_returns_empty_list_if_base_component_does_not_exist() {
    ComponentTreeQuery query = newTreeQuery(PROJECT_UUID).setStrategy(CHILDREN).build();

    List<ComponentDto> result = underTest.selectDescendants(dbSession, query);
    assertThat(result).isEmpty();
  }

  @Test
  public void select_descendants_of_a_view_and_filter_by_name() {
    OrganizationDto organizationDto = db.organizations().insert();
    ComponentDto view = newView(organizationDto, A_VIEW_UUID);
    db.components().insertViewAndSnapshot(view);
    // one subview
    ComponentDto subView = newSubView(view, "subview-uuid", "subview-key").setName("subview name");
    db.components().insertComponent(subView);
    // one project and its copy linked to the view
    ComponentDto project = newPrivateProjectDto(organizationDto, PROJECT_UUID).setName("project name");
    db.components().insertProjectAndSnapshot(project);
    db.components().insertComponent(newProjectCopy("project-copy-uuid", project, view));
    ComponentTreeQuery dbQuery = newTreeQuery(A_VIEW_UUID).setNameOrKeyQuery("name").setStrategy(CHILDREN).build();

    List<ComponentDto> components = underTest.selectDescendants(dbSession, dbQuery);
    assertThat(components).extracting("uuid").containsOnly("project-copy-uuid", "subview-uuid");
    assertThat(components).extracting("organizationUuid").containsOnly(organizationDto.getUuid());
  }

  @Test
  public void select_projects_by_name_query() {
    OrganizationDto organizationDto = db.organizations().insert();
    ComponentDto project1 = db.components().insertComponent(ComponentTesting.newPrivateProjectDto(organizationDto).setName("project1"));
    ComponentDto module1 = db.components().insertComponent(newModuleDto(project1).setName("module1"));
    ComponentDto subModule1 = db.components().insertComponent(newModuleDto(module1).setName("subModule1"));
    db.components().insertComponent(newFileDto(subModule1).setName("file"));
    ComponentDto project2 = db.components().insertComponent(ComponentTesting.newPrivateProjectDto(organizationDto).setName("project2"));
    ComponentDto project3 = db.components().insertComponent(ComponentTesting.newPrivateProjectDto(organizationDto).setName("project3"));

    assertThat(underTest.selectProjectsByNameQuery(dbSession, null, false)).extracting(ComponentDto::uuid)
      .containsOnly(project1.uuid(), project2.uuid(), project3.uuid());
    assertThat(underTest.selectProjectsByNameQuery(dbSession, null, true)).extracting(ComponentDto::uuid)
      .containsOnly(project1.uuid(), project2.uuid(), project3.uuid(), module1.uuid(), subModule1.uuid());
    assertThat(underTest.selectProjectsByNameQuery(dbSession, "project1", false)).extracting(ComponentDto::uuid).containsOnly(project1.uuid());
    assertThat(underTest.selectProjectsByNameQuery(dbSession, "ct1", false)).extracting(ComponentDto::uuid).containsOnly(project1.uuid());
    assertThat(underTest.selectProjectsByNameQuery(dbSession, "pro", false)).extracting(ComponentDto::uuid).containsOnly(project1.uuid(), project2.uuid(), project3.uuid());
    assertThat(underTest.selectProjectsByNameQuery(dbSession, "jec", false)).extracting(ComponentDto::uuid).containsOnly(project1.uuid(), project2.uuid(), project3.uuid());
    assertThat(underTest.selectProjectsByNameQuery(dbSession, "1", true)).extracting(ComponentDto::uuid).containsOnly(project1.uuid(), module1.uuid(), subModule1.uuid());
    assertThat(underTest.selectProjectsByNameQuery(dbSession, "unknown", true)).extracting(ComponentDto::uuid).isEmpty();
  }

  @Test
  public void setPrivateForRootComponentUuid_updates_private_column_to_specified_value_for_all_rows_with_specified_projectUuid() {
    String uuid1 = "uuid1";
    String uuid2 = "uuid2";

    OrganizationDto organizationDto = db.organizations().insert();
    String[] uuids = {
      db.components().insertComponent(ComponentTesting.newPrivateProjectDto(organizationDto).setProjectUuid(uuid1).setPrivate(true)).uuid(),
      db.components().insertComponent(ComponentTesting.newPrivateProjectDto(organizationDto).setProjectUuid(uuid1).setPrivate(false)).uuid(),
      db.components().insertComponent(ComponentTesting.newPrivateProjectDto(organizationDto).setProjectUuid(uuid2).setPrivate(true)).uuid(),
      db.components().insertComponent(ComponentTesting.newPrivateProjectDto(organizationDto).setProjectUuid(uuid2).setPrivate(false)).uuid(),
      db.components().insertComponent(ComponentTesting.newPrivateProjectDto(organizationDto).setRootUuid(uuid1).setProjectUuid("foo").setPrivate(false)).uuid(),
    };

    underTest.setPrivateForRootComponentUuid(db.getSession(), uuid1, true);

    assertThat(privateFlagOfUuid(uuids[0])).isTrue();
    assertThat(privateFlagOfUuid(uuids[1])).isTrue();
    assertThat(privateFlagOfUuid(uuids[2])).isTrue();
    assertThat(privateFlagOfUuid(uuids[3])).isFalse();
    assertThat(privateFlagOfUuid(uuids[4])).isFalse();

    underTest.setPrivateForRootComponentUuid(db.getSession(), uuid1, false);

    assertThat(privateFlagOfUuid(uuids[0])).isFalse();
    assertThat(privateFlagOfUuid(uuids[1])).isFalse();
    assertThat(privateFlagOfUuid(uuids[2])).isTrue();
    assertThat(privateFlagOfUuid(uuids[3])).isFalse();
    assertThat(privateFlagOfUuid(uuids[4])).isFalse();

    underTest.setPrivateForRootComponentUuid(db.getSession(), uuid2, false);

    assertThat(privateFlagOfUuid(uuids[0])).isFalse();
    assertThat(privateFlagOfUuid(uuids[1])).isFalse();
    assertThat(privateFlagOfUuid(uuids[2])).isFalse();
    assertThat(privateFlagOfUuid(uuids[3])).isFalse();
    assertThat(privateFlagOfUuid(uuids[4])).isFalse();

    underTest.setPrivateForRootComponentUuid(db.getSession(), uuid2, true);

    assertThat(privateFlagOfUuid(uuids[0])).isFalse();
    assertThat(privateFlagOfUuid(uuids[1])).isFalse();
    assertThat(privateFlagOfUuid(uuids[2])).isTrue();
    assertThat(privateFlagOfUuid(uuids[3])).isTrue();
    assertThat(privateFlagOfUuid(uuids[4])).isFalse();
  }

  private boolean privateFlagOfUuid(String uuid) {
    return underTest.selectByUuid(db.getSession(), uuid).get().isPrivate();
  }
}
