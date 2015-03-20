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

import org.apache.ibatis.exceptions.PersistenceException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.component.FilePathWithHashDto;
import org.sonar.core.persistence.AbstractDaoTestCase;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.exceptions.NotFoundException;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ComponentDaoTest extends AbstractDaoTestCase {

  DbSession session;

  ComponentDao dao;

  System2 system2;

  @Before
  public void createDao() throws Exception {
    session = getMyBatis().openSession(false);
    system2 = mock(System2.class);
    dao = new ComponentDao(system2);
  }

  @After
  public void tearDown() throws Exception {
    session.close();
  }

  @Test
  public void get_by_uuid() {
    setupData("shared");

    ComponentDto result = dao.getNullableByUuid(session, "KLMN");
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

  @Test
  public void get_by_uuid_on_technical_project_copy() {
    setupData("shared");

    ComponentDto result = dao.getNullableByUuid(session, "STUV");
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
    setupData("shared");

    ComponentDto result = dao.getNullableByUuid(session, "DCBA");
    assertThat(result).isNotNull();
    assertThat(result.isEnabled()).isFalse();
  }

  @Test(expected = NotFoundException.class)
  public void fail_to_get_by_uuid_when_component_not_found() {
    setupData("shared");

    dao.getByUuid(session, "unknown");
  }

  @Test
  public void get_by_key() {
    setupData("shared");

    ComponentDto result = dao.getNullableByKey(session, "org.struts:struts-core:src/org/struts/RequestContext.java");
    assertThat(result).isNotNull();
    assertThat(result.key()).isEqualTo("org.struts:struts-core:src/org/struts/RequestContext.java");
    assertThat(result.path()).isEqualTo("src/org/struts/RequestContext.java");
    assertThat(result.name()).isEqualTo("RequestContext.java");
    assertThat(result.longName()).isEqualTo("org.struts.RequestContext");
    assertThat(result.qualifier()).isEqualTo("FIL");
    assertThat(result.scope()).isEqualTo("FIL");
    assertThat(result.language()).isEqualTo("java");
    assertThat(result.parentProjectId()).isEqualTo(2);

    assertThat(dao.getNullableByKey(session, "unknown")).isNull();
  }

  @Test
  public void get_by_key_on_disabled_component() {
    setupData("shared");

    ComponentDto result = dao.getNullableByKey(session, "org.disabled.project");
    assertThat(result).isNotNull();
    assertThat(result.isEnabled()).isFalse();
  }

  @Test
  public void get_by_key_on_a_root_project() {
    setupData("shared");

    ComponentDto result = dao.getNullableByKey(session, "org.struts:struts");
    assertThat(result).isNotNull();
    assertThat(result.key()).isEqualTo("org.struts:struts");
    assertThat(result.deprecatedKey()).isEqualTo("org.struts:struts");
    assertThat(result.path()).isNull();
    assertThat(result.name()).isEqualTo("Struts");
    assertThat(result.longName()).isEqualTo("Apache Struts");
    assertThat(result.qualifier()).isEqualTo("TRK");
    assertThat(result.scope()).isEqualTo("PRJ");
    assertThat(result.language()).isNull();
    assertThat(result.parentProjectId()).isNull();
    assertThat(result.getAuthorizationUpdatedAt()).isEqualTo(123456789L);
  }

  @Test
  public void get_by_keys() {
    setupData("shared");

    List<ComponentDto> results = dao.getByKeys(session, "org.struts:struts-core:src/org/struts/RequestContext.java");
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

    assertThat(dao.getByKeys(session, "unknown")).isEmpty();
  }

  @Test
  public void get_by_ids() {
    setupData("shared");

    List<ComponentDto> results = dao.getByIds(session, newArrayList(4L));
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

    assertThat(dao.getByIds(session, newArrayList(555L))).isEmpty();
  }

  @Test
  public void get_by_uuids() {
    setupData("shared");

    List<ComponentDto> results = dao.getByUuids(session, newArrayList("KLMN"));
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

    assertThat(dao.getByUuids(session, newArrayList("unknown"))).isEmpty();
  }

  @Test
  public void get_by_uuids_on_removed_components() {
    setupData("shared");

    List<ComponentDto> results = dao.getByUuids(session, newArrayList("DCBA"));
    assertThat(results).hasSize(1);

    ComponentDto result = results.get(0);
    assertThat(result).isNotNull();
    assertThat(result.isEnabled()).isFalse();
  }

  @Test
  public void select_existing_uuids() {
    setupData("shared");

    List<String> results = dao.selectExistingUuids(session, newArrayList("KLMN"));
    assertThat(results).containsOnly("KLMN");

    assertThat(dao.selectExistingUuids(session, newArrayList("KLMN", "unknown"))).hasSize(1);
    assertThat(dao.selectExistingUuids(session, newArrayList("unknown"))).isEmpty();
  }

  @Test
  public void get_by_id() {
    setupData("shared");

    assertThat(dao.getById(4L, session)).isNotNull();
  }

  @Test
  public void get_by_id_on_disabled_component() {
    setupData("shared");

    ComponentDto result = dao.getNullableById(10L, session);
    assertThat(result).isNotNull();
    assertThat(result.isEnabled()).isFalse();
  }

  @Test(expected = NotFoundException.class)
  public void fail_to_get_by_id_when_project_not_found() {
    setupData("shared");

    dao.getById(111L, session);
  }

  @Test
  public void get_nullable_by_id() {
    setupData("shared");

    assertThat(dao.getNullableById(4L, session)).isNotNull();
    assertThat(dao.getNullableById(111L, session)).isNull();
  }

  @Test
  public void count_by_id() {
    setupData("shared");

    assertThat(dao.existsById(4L, session)).isTrue();
    assertThat(dao.existsById(111L, session)).isFalse();
  }

  @Test
  public void find_modules_by_project() throws Exception {
    setupData("multi-modules");

    List<ComponentDto> results = dao.findModulesByProject("org.struts:struts", session);
    assertThat(results).hasSize(1);
    assertThat(results.get(0).getKey()).isEqualTo("org.struts:struts-core");

    results = dao.findModulesByProject("org.struts:struts-core", session);
    assertThat(results).hasSize(1);
    assertThat(results.get(0).getKey()).isEqualTo("org.struts:struts-data");

    assertThat(dao.findModulesByProject("org.struts:struts-data", session)).isEmpty();

    assertThat(dao.findModulesByProject("unknown", session)).isEmpty();
  }

  @Test
  public void find_sub_projects_by_component_keys() throws Exception {
    setupData("multi-modules");

    // Sub project of a file
    List<ComponentDto> results = dao.findSubProjectsByComponentUuids(session, newArrayList("HIJK"));
    assertThat(results).hasSize(1);
    assertThat(results.get(0).getKey()).isEqualTo("org.struts:struts-data");

    // Sub project of a directory
    results = dao.findSubProjectsByComponentUuids(session, newArrayList("GHIJ"));
    assertThat(results).hasSize(1);
    assertThat(results.get(0).getKey()).isEqualTo("org.struts:struts-data");

    // Sub project of a sub module
    results = dao.findSubProjectsByComponentUuids(session, newArrayList("FGHI"));
    assertThat(results).hasSize(1);
    assertThat(results.get(0).getKey()).isEqualTo("org.struts:struts");

    // Sub project of a module
    results = dao.findSubProjectsByComponentUuids(session, newArrayList("EFGH"));
    assertThat(results).hasSize(1);
    assertThat(results.get(0).getKey()).isEqualTo("org.struts:struts");

    // Sub project of a project
    assertThat(dao.findSubProjectsByComponentUuids(session, newArrayList("ABCD"))).isEmpty();

    // SUb projects of a component and a sub module
    assertThat(dao.findSubProjectsByComponentUuids(session, newArrayList("HIJK", "FGHI"))).hasSize(2);

    assertThat(dao.findSubProjectsByComponentUuids(session, newArrayList("unknown"))).isEmpty();

    assertThat(dao.findSubProjectsByComponentUuids(session, Collections.<String>emptyList())).isEmpty();
  }

  @Test
  public void select_enabled_modules_tree() throws Exception {
    setupData("multi-modules");

    // From root project
    List<ComponentDto> modules = dao.selectEnabledDescendantModules(session, "ABCD");
    assertThat(modules).extracting("uuid").containsOnly("ABCD", "EFGH", "FGHI");

    // From module
    modules = dao.selectEnabledDescendantModules(session, "EFGH");
    assertThat(modules).extracting("uuid").containsOnly("EFGH", "FGHI");

    // From sub module
    modules = dao.selectEnabledDescendantModules(session, "FGHI");
    assertThat(modules).extracting("uuid").containsOnly("FGHI");

    // Folder
    assertThat(dao.selectEnabledDescendantModules(session, "GHIJ")).isEmpty();
    assertThat(dao.selectEnabledDescendantModules(session, "unknown")).isEmpty();
  }

  @Test
  public void select_all_modules_tree() throws Exception {
    setupData("multi-modules");

    // From root project, disabled sub module is returned
    List<ComponentDto> modules = dao.selectDescendantModules(session, "ABCD");
    assertThat(modules).extracting("uuid").containsOnly("ABCD", "EFGH", "FGHI", "IHGF");

    // From module, disabled sub module is returned
    modules = dao.selectDescendantModules(session, "EFGH");
    assertThat(modules).extracting("uuid").containsOnly("EFGH", "FGHI", "IHGF");

    // From removed sub module -> should not be returned
    assertThat(dao.selectDescendantModules(session, "IHGF")).isEmpty();
  }

  @Test
  public void select_enabled_module_files_tree() throws Exception {
    setupData("select_module_files_tree");

    // From root project
    List<FilePathWithHashDto> files = dao.selectEnabledDescendantFiles(session, "ABCD");
    assertThat(files).extracting("uuid").containsOnly("EFGHI", "HIJK");
    assertThat(files).extracting("moduleUuid").containsOnly("EFGH", "FGHI");
    assertThat(files).extracting("srcHash").containsOnly("srcEFGHI", "srcHIJK");
    assertThat(files).extracting("path").containsOnly("src/org/struts/pom.xml", "src/org/struts/RequestContext.java");

    // From module
    files = dao.selectEnabledDescendantFiles(session, "EFGH");
    assertThat(files).extracting("uuid").containsOnly("EFGHI", "HIJK");
    assertThat(files).extracting("moduleUuid").containsOnly("EFGH", "FGHI");
    assertThat(files).extracting("srcHash").containsOnly("srcEFGHI", "srcHIJK");
    assertThat(files).extracting("path").containsOnly("src/org/struts/pom.xml", "src/org/struts/RequestContext.java");

    // From sub module
    files = dao.selectEnabledDescendantFiles(session, "FGHI");
    assertThat(files).extracting("uuid").containsOnly("HIJK");
    assertThat(files).extracting("moduleUuid").containsOnly("FGHI");
    assertThat(files).extracting("srcHash").containsOnly("srcHIJK");
    assertThat(files).extracting("path").containsOnly("src/org/struts/RequestContext.java");

    // From directory
    assertThat(dao.selectEnabledDescendantFiles(session, "GHIJ")).isEmpty();

    assertThat(dao.selectEnabledDescendantFiles(session, "unknown")).isEmpty();
  }

  @Test
  public void insert() {
    when(system2.now()).thenReturn(DateUtils.parseDate("2014-06-18").getTime());
    setupData("empty");

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
      .setPath("src/org/struts/RequestContext.java")
      .setParentProjectId(3L)
      .setCopyResourceId(5L)
      .setEnabled(true)
      .setAuthorizationUpdatedAt(123456789L);

    dao.insert(session, componentDto);
    session.commit();

    assertThat(componentDto.getId()).isNotNull();
    checkTables("insert", "projects");
  }

  @Test
  public void insert_disabled_component() {
    when(system2.now()).thenReturn(DateUtils.parseDate("2014-06-18").getTime());
    setupData("empty");

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
      .setAuthorizationUpdatedAt(123456789L);

    dao.insert(session, componentDto);
    session.commit();

    assertThat(componentDto.getId()).isNotNull();
    checkTables("insert_disabled_component", "projects");
  }

  @Test(expected = IllegalStateException.class)
  public void update() {
    dao.update(session, new ComponentDto()
      .setId(1L)
      .setKey("org.struts:struts-core:src/org/struts/RequestContext.java")
      );
  }

  @Test
  public void delete() {
    setupData("shared");

    dao.delete(session, new ComponentDto()
      .setId(1L)
      .setKey("org.struts:struts-core:src/org/struts/RequestContext.java")
      );
    session.commit();

    checkTable("delete", "projects");
  }

  @Test
  public void find_project_uuids() {
    setupData("find_project_uuids");

    assertThat(dao.findProjectUuids(session)).containsExactly("ABCD");
  }

  @Test(expected = PersistenceException.class)
  public void synchronize_after() {
    dao.synchronizeAfter(session, new Date(0L));
  }

  @Test
  public void select_views_and_sub_views() {
    setupData("shared_views");

    assertThat(dao.selectAllViewsAndSubViews(session)).extracting("uuid").containsOnly("ABCD", "EFGH", "FGHI", "IJKL");
    assertThat(dao.selectAllViewsAndSubViews(session)).extracting("projectUuid").containsOnly("ABCD", "EFGH", "IJKL");
  }

  @Test
  public void select_projects_from_view() {
    setupData("shared_views");

    assertThat(dao.selectProjectsFromView(session, "ABCD", "ABCD")).containsOnly("JKLM");
    assertThat(dao.selectProjectsFromView(session, "EFGH", "EFGH")).containsOnly("KLMN", "JKLM");
    assertThat(dao.selectProjectsFromView(session, "FGHI", "EFGH")).containsOnly("JKLM");
    assertThat(dao.selectProjectsFromView(session, "IJKL", "IJKL")).isEmpty();
    assertThat(dao.selectProjectsFromView(session, "Unknown", "Unknown")).isEmpty();
  }
}
