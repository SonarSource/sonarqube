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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;

import static org.assertj.core.api.Assertions.assertThat;


public class ResourceIndexDaoTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  ResourceIndexDao dao = dbTester.getDbClient().componentIndexDao();

  @Before
  public void setUp() throws Exception {
    dbTester.truncateTables();
  }

  @Test
  public void shouldIndexResource() {
    dbTester.prepareDbUnit(getClass(), "shouldIndexResource.xml");

    dao.indexResource(10, "ZipUtils", "FIL", 8);

    dbTester.assertDbUnit(getClass(), "shouldIndexResource-result.xml", new String[]{"id"}, "resource_index");
  }

  @Test
  public void shouldIndexProjects() {
    dbTester.prepareDbUnit(getClass(), "shouldIndexProjects.xml");

    dao.indexProjects();

    dbTester.assertDbUnit(getClass(), "shouldIndexProjects-result.xml", new String[]{"id"}, "resource_index");
  }

  @Test
  public void shouldIndexMultiModulesProject() {
    dbTester.prepareDbUnit(getClass(), "shouldIndexMultiModulesProject.xml");

    dao.indexProject(1);

    dbTester.assertDbUnit(getClass(), "shouldIndexMultiModulesProject-result.xml", new String[]{"id"}, "resource_index");
  }

  @Test
  public void shouldReindexProjectAfterRenaming() {
    dbTester.prepareDbUnit(getClass(), "shouldReindexProjectAfterRenaming.xml");

    dao.indexProject(1);

    dbTester.assertDbUnit(getClass(), "shouldReindexProjectAfterRenaming-result.xml", new String[]{"id"}, "resource_index");
  }

  @Test
  public void shouldNotIndexPackages() {
    dbTester.prepareDbUnit(getClass(), "shouldNotIndexPackages.xml");

    dao.indexProject(1);
    // project
    assertThat(dbTester.countSql("select count(1) from resource_index where resource_id=1")).isGreaterThan(0);
    // directory
    assertThat(dbTester.countSql("select count(1) from resource_index where resource_id=2")).isEqualTo(0);
    // file
    assertThat(dbTester.countSql("select count(1) from resource_index where resource_id=3")).isGreaterThan(0);
  }

  @Test
  public void shouldIndexTwoLettersLongResources() {
    dbTester.prepareDbUnit(getClass(), "shouldIndexTwoLettersLongResource.xml");

    dao.indexResource(10, "AB", Qualifiers.PROJECT, 3);

    dbTester.assertDbUnit(getClass(), "shouldIndexTwoLettersLongResource-result.xml", new String[]{"id"}, "resource_index");
  }

  @Test
  public void shouldReIndexTwoLettersLongResources() {
    dbTester.prepareDbUnit(getClass(), "shouldReIndexTwoLettersLongResource.xml");

    dao.indexResource(1, "AS", Qualifiers.PROJECT, 1);

    dbTester.assertDbUnit(getClass(), "shouldReIndexTwoLettersLongResource-result.xml", new String[]{"id"}, "resource_index");
  }

  @Test
  public void shouldReIndexNewTwoLettersLongResource() {
    dbTester.prepareDbUnit(getClass(), "shouldReIndexNewTwoLettersLongResource.xml");

    dao.indexResource(1, "AS", Qualifiers.PROJECT, 1);

    dbTester.assertDbUnit(getClass(), "shouldReIndexNewTwoLettersLongResource-result.xml", new String[]{"id"}, "resource_index");
  }

  @Test
  public void shouldReindexResource() {
    dbTester.prepareDbUnit(getClass(), "shouldReindexResource.xml");

    dao.indexResource(1, "New Struts", Qualifiers.PROJECT, 1);

    dbTester.assertDbUnit(getClass(), "shouldReindexResource-result.xml", new String[]{"id"}, "resource_index");
  }

  @Test
  public void shouldNotReindexUnchangedResource() {
    dbTester.prepareDbUnit(getClass(), "shouldNotReindexUnchangedResource.xml");

    dao.indexResource(1, "Struts", Qualifiers.PROJECT, 1);

    dbTester.assertDbUnit(getClass(), "shouldNotReindexUnchangedResource-result.xml", new String[]{"id"}, "resource_index");
  }

  @Test
  public void select_project_ids_from_query_and_view_or_sub_view_uuid() {
    dbTester.prepareDbUnit(getClass(), "select_project_ids_from_query_and_view_or_sub_view_uuid.xml");

    String viewUuid = "EFGH";

    DbSession session = dbTester.getSession();
    assertThat(dao.selectProjectIdsFromQueryAndViewOrSubViewUuid(session, "project", viewUuid)).containsOnly(1L, 2L);
    assertThat(dao.selectProjectIdsFromQueryAndViewOrSubViewUuid(session, "one", viewUuid)).containsOnly(1L);
    assertThat(dao.selectProjectIdsFromQueryAndViewOrSubViewUuid(session, "two", viewUuid)).containsOnly(2L);
    assertThat(dao.selectProjectIdsFromQueryAndViewOrSubViewUuid(session, "unknown", viewUuid)).isEmpty();
  }
}
