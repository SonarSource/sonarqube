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
import org.sonar.core.component.AuthorizedComponentDto;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.persistence.AbstractDaoTestCase;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.exceptions.NotFoundException;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;
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
    assertThat(result.moduleUuidPath()).isEqualTo("ABCD.EFGH.");
    assertThat(result.subProjectId()).isEqualTo(2);
    assertThat(result.projectUuid()).isEqualTo("ABCD");
    assertThat(result.key()).isEqualTo("org.struts:struts-core:src/org/struts/RequestContext.java");
    assertThat(result.path()).isEqualTo("src/org/struts/RequestContext.java");
    assertThat(result.name()).isEqualTo("RequestContext.java");
    assertThat(result.longName()).isEqualTo("org.struts.RequestContext");
    assertThat(result.qualifier()).isEqualTo("FIL");
    assertThat(result.scope()).isEqualTo("FIL");
    assertThat(result.language()).isEqualTo("java");
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
    assertThat(result.subProjectId()).isEqualTo(2);

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
    assertThat(result.path()).isNull();
    assertThat(result.name()).isEqualTo("Struts");
    assertThat(result.longName()).isEqualTo("Apache Struts");
    assertThat(result.qualifier()).isEqualTo("TRK");
    assertThat(result.scope()).isEqualTo("PRJ");
    assertThat(result.language()).isNull();
    assertThat(result.subProjectId()).isNull();
    assertThat(result.getAuthorizationUpdatedAt()).isEqualTo(DateUtils.parseDate("2014-06-18"));
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
    assertThat(result.subProjectId()).isEqualTo(2);
    assertThat(result.projectId()).isEqualTo(1);

    assertThat(dao.getByKeys(session, "unknown")).isEmpty();
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
    assertThat(result.moduleUuidPath()).isEqualTo("ABCD.EFGH.");
    assertThat(result.subProjectId()).isEqualTo(2);
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
    assertThat(result.subProjectId()).isEqualTo(2);
    assertThat(result.projectId()).isEqualTo(1);

    assertThat(dao.getByIds(session, newArrayList(123L))).isEmpty();
    assertThat(dao.getByIds(session, Collections.<Long>emptyList())).isEmpty();
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
  public void get_nullable_root_project_by_key() throws Exception {
    setupData("multi-modules");

    assertThat(dao.getNullableRootProjectByKey("org.struts:struts-data", session).getKey()).isEqualTo("org.struts:struts");
    assertThat(dao.getNullableRootProjectByKey("org.struts:struts-core", session).getKey()).isEqualTo("org.struts:struts");

    // Root project of a project is itself
    assertThat(dao.getNullableRootProjectByKey("org.struts:struts", session).getKey()).isEqualTo("org.struts:struts");

    assertThat(dao.getNullableRootProjectByKey("unknown", session)).isNull();
  }

  @Test
  public void get_root_project_by_key() throws Exception {
    setupData("multi-modules");

    assertThat(dao.getRootProjectByKey("org.struts:struts-data", session).getKey()).isEqualTo("org.struts:struts");
    assertThat(dao.getRootProjectByKey("org.struts:struts-core", session).getKey()).isEqualTo("org.struts:struts");

    // Root project of a project is itself
    assertThat(dao.getRootProjectByKey("org.struts:struts", session).getKey()).isEqualTo("org.struts:struts");
  }

  @Test(expected = NotFoundException.class)
  public void get_root_project_by_key_on_unknown_project() throws Exception {
    dao.getRootProjectByKey("unknown", session);
  }

  @Test
  public void get_parent_module_by_key() throws Exception {
    setupData("multi-modules");

    assertThat(dao.getParentModuleByKey("org.struts:struts-data", session).getKey()).isEqualTo("org.struts:struts-core");
    assertThat(dao.getParentModuleByKey("org.struts:struts-core", session).getKey()).isEqualTo("org.struts:struts");
    assertThat(dao.getParentModuleByKey("org.struts:struts", session)).isNull();

    assertThat(dao.getParentModuleByKey("unknown", session)).isNull();
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
  public void get_nullable_authorized_component_by_id() {
    setupData("shared");

    AuthorizedComponentDto result = dao.getNullableAuthorizedComponentById(4L, session);
    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(4);
    assertThat(result.getKey()).isEqualTo("org.struts:struts-core:src/org/struts/RequestContext.java");
    assertThat(result.qualifier()).isEqualTo("FIL");
    assertThat(result.scope()).isEqualTo("FIL");

    assertThat(dao.getNullableAuthorizedComponentById(111L, session)).isNull();
  }

  @Test
  public void get_authorized_component_by_id() {
    setupData("shared");

    assertThat(dao.getAuthorizedComponentById(4L, session)).isNotNull();
  }

  @Test(expected = NotFoundException.class)
  public void fail_to_get_authorized_component_by_id_when_project_not_found() {
    setupData("shared");

    dao.getAuthorizedComponentById(111L, session);
  }

  @Test
  public void get_nullable_authorized_component_by_key() {
    setupData("shared");

    AuthorizedComponentDto result = dao.getNullableAuthorizedComponentByKey("org.struts:struts-core:src/org/struts/RequestContext.java", session);
    assertThat(result).isNotNull();
    assertThat(result.key()).isEqualTo("org.struts:struts-core:src/org/struts/RequestContext.java");

    assertThat(dao.getNullableAuthorizedComponentByKey("unknown", session)).isNull();
  }

  @Test
  public void get_authorized_component_by_key() {
    setupData("shared");

    assertThat(dao.getAuthorizedComponentByKey("org.struts:struts-core:src/org/struts/RequestContext.java", session)).isNotNull();
  }

  @Test(expected = NotFoundException.class)
  public void fail_to_get_authorized_component_by_key_when_project_not_found() {
    setupData("shared");

    dao.getAuthorizedComponentByKey("unknown", session);
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
      .setModuleUuidPath("ABCD.EFGH")
      .setKey("org.struts:struts-core:src/org/struts/RequestContext.java")
      .setName("RequestContext.java")
      .setLongName("org.struts.RequestContext")
      .setQualifier("FIL")
      .setScope("FIL")
      .setLanguage("java")
      .setPath("src/org/struts/RequestContext.java")
      .setSubProjectId(3L)
      .setEnabled(true)
      .setAuthorizationUpdatedAt(DateUtils.parseDate("2014-06-18"));

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
      .setModuleUuidPath("ABCD.EFGH")
      .setKey("org.struts:struts-core:src/org/struts/RequestContext.java")
      .setName("RequestContext.java")
      .setLongName("org.struts.RequestContext")
      .setQualifier("FIL")
      .setScope("FIL")
      .setLanguage("java")
      .setPath("src/org/struts/RequestContext.java")
      .setSubProjectId(3L)
      .setEnabled(false)
      .setAuthorizationUpdatedAt(DateUtils.parseDate("2014-06-18"));

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

  @Test(expected = PersistenceException.class)
  public void synchronize_after() {
    dao.synchronizeAfter(session, new Date(0L));
  }
}
